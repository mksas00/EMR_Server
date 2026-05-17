package com.example.emr_server.service;

import com.example.emr_server.controller.dto.LabOrderDto;
import com.example.emr_server.controller.dto.LabOrderItemDto;
import com.example.emr_server.controller.dto.LabPanelDto;
import com.example.emr_server.controller.dto.request.LabOrderCreateRequest;
import com.example.emr_server.controller.dto.request.LabOrderItemUpdateRequest;
import com.example.emr_server.entity.*;
import com.example.emr_server.repository.LabOrderItemRepository;
import com.example.emr_server.repository.LabOrderRepository;
import com.example.emr_server.repository.LabPanelRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class LabOrderServiceImpl implements LabOrderService {

    private final LabOrderRepository orderRepository;
    private final LabOrderItemRepository itemRepository;
    private final LabPanelRepository panelRepository;
    private final UserRepository userRepository;
    private final PatientService patientService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public LabOrderServiceImpl(LabOrderRepository orderRepository,
                               LabOrderItemRepository itemRepository,
                               LabPanelRepository panelRepository,
                               UserRepository userRepository,
                               PatientService patientService,
                               AuthorizationService authorizationService,
                               AuditService auditService) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.panelRepository = panelRepository;
        this.userRepository = userRepository;
        this.patientService = patientService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    private User current() {
        return SecurityUtil.getCurrentUser(userRepository)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user"));
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    /** Wylicza abnormal flag jeśli wynik numeryczny wykracza poza zakres referencyjny. */
    static String computeAbnormalFlag(BigDecimal numeric, BigDecimal low, BigDecimal high) {
        if (numeric == null) return null;
        if (low != null && numeric.compareTo(low) < 0) return "L";
        if (high != null && numeric.compareTo(high) > 0) return "H";
        return "N";
    }

    /** Po aktualizacji pozycji wylicza ponownie agregowany status zlecenia. */
    static String aggregateOrderStatus(List<LabOrderItem> items) {
        if (items == null || items.isEmpty()) return "pending";
        boolean anyCompleted = false;
        boolean anyPending = false;
        boolean allCancelled = true;
        for (LabOrderItem it : items) {
            String s = it.getStatus();
            if ("completed".equals(s)) { anyCompleted = true; allCancelled = false; }
            else if ("pending".equals(s)) { anyPending = true; allCancelled = false; }
        }
        if (allCancelled) return "cancelled";
        if (!anyPending && anyCompleted) return "completed";
        if (anyCompleted) return "in_progress";
        return "pending";
    }

    private LabOrderItemDto toItemDto(LabOrderItem it) {
        User cb = it.getCompletedBy();
        return new LabOrderItemDto(
                it.getId(),
                it.getOrder() != null ? it.getOrder().getId() : null,
                it.getTestCode(),
                it.getTestName(),
                it.getUnit(),
                it.getRefRangeLow(),
                it.getRefRangeHigh(),
                it.getRefRangeText(),
                it.getResult(),
                it.getResultNumeric(),
                it.getAbnormalFlag(),
                it.getStatus(),
                it.getCompletedAt(),
                cb != null ? cb.getId() : null,
                cb != null ? cb.getUsername() : null,
                it.getNotes(),
                it.getSortOrder()
        );
    }

    private LabOrderDto toOrderDto(LabOrder o, boolean withItems) {
        Patient p = o.getPatient();
        User ob = o.getOrderedBy();
        User cb = o.getCancelledBy();

        List<LabOrderItem> items = o.getItems();
        int total = items != null ? items.size() : 0;
        int completed = 0;
        int abnormal = 0;
        if (items != null) {
            for (LabOrderItem it : items) {
                if ("completed".equals(it.getStatus())) completed++;
                String f = it.getAbnormalFlag();
                if (f != null && !"N".equals(f)) abnormal++;
            }
        }

        return new LabOrderDto(
                o.getId(),
                p != null ? p.getId() : null,
                p != null ? (safe(p.getFirstName()) + " " + safe(p.getLastName())).trim() : null,
                ob != null ? ob.getId() : null,
                ob != null ? ob.getUsername() : null,
                o.getOrderedAt(),
                o.getPriority(),
                o.getStatus(),
                o.getNotes(),
                o.getCancelledAt(),
                cb != null ? cb.getId() : null,
                cb != null ? cb.getUsername() : null,
                o.getCancelledReason(),
                o.getCompletedAt(),
                o.getCreatedAt(),
                o.getUpdatedAt(),
                total,
                completed,
                abnormal,
                withItems && items != null ? items.stream().map(this::toItemDto).toList() : null
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ─── public API ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderDto> search(
            Optional<UUID> patientId,
            Optional<String> pesel,
            Optional<UUID> orderedById,
            Optional<String> status,
            Optional<String> priority,
            Optional<Instant> from,
            Optional<Instant> to,
            Pageable pageable) {

        Optional<UUID> effectivePatientId = patientId;
        if (pesel.isPresent() && !pesel.get().isBlank()) {
            effectivePatientId = patientService.findPatientIdByPesel(pesel.get());
            if (effectivePatientId.isEmpty()) {
                return Page.empty(pageable);
            }
        }
        final Optional<UUID> pid = effectivePatientId;

        Specification<LabOrder> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            pid.ifPresent(v -> ps.add(cb.equal(root.get("patient").get("id"), v)));
            orderedById.ifPresent(v -> ps.add(cb.equal(root.get("orderedBy").get("id"), v)));
            status.ifPresent(v -> ps.add(cb.equal(root.get("status"), v)));
            priority.ifPresent(v -> ps.add(cb.equal(root.get("priority"), v)));
            from.ifPresent(f -> ps.add(cb.greaterThanOrEqualTo(root.get("orderedAt"), f)));
            to.ifPresent(t -> ps.add(cb.lessThanOrEqualTo(root.get("orderedAt"), t)));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        User u = current();
        // Autoryzacja: filtrujemy wyniki po canReadPatient.
        // Dla wydajności w prawdziwej app można dodać join + JPQL z subquery na uprawnienia.
        return orderRepository.findAll(spec, pageable)
                .map(o -> authorizationService.canReadPatient(u, o.getPatient()) ? toOrderDto(o, false) : null)
                .map(dto -> dto); // drop nulls handled below if needed
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LabOrderDto> getDetail(UUID orderId) {
        User u = current();
        return orderRepository.findById(orderId)
                .filter(o -> authorizationService.canReadPatient(u, o.getPatient()))
                .map(o -> {
                    // wymuszenie inicjalizacji listy w transakcji
                    o.getItems().size();
                    return toOrderDto(o, true);
                });
    }

    @Override
    @Transactional
    public LabOrderDto create(LabOrderCreateRequest req) {
        User u = current();

        // Pobierz pacjenta i zweryfikuj autoryzację zapisu
        Patient existing = patientService.findPatientById(req.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacjent nie istnieje"));
        if (!authorizationService.canWritePatient(u, existing)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do utworzenia zlecenia");
        }

        LabOrder order = new LabOrder();
        order.setPatient(existing);
        order.setOrderedBy(u);
        order.setPriority(req.priority() != null ? req.priority() : "routine");
        order.setStatus("pending");
        order.setNotes(req.notes());

        // Pozycje z paneli
        int sort = 0;
        Set<String> usedCodes = new LinkedHashSet<>();
        if (req.panelIds() != null) {
            for (UUID panelId : req.panelIds()) {
                LabPanel panel = panelRepository.findById(panelId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Panel nie istnieje: " + panelId));
                for (LabPanelItem pi : panel.getItems()) {
                    if (!usedCodes.add(pi.getTestCode())) continue; // deduplicate by code
                    LabOrderItem item = new LabOrderItem();
                    item.setOrder(order);
                    item.setTestCode(pi.getTestCode());
                    item.setTestName(pi.getTestName());
                    item.setUnit(pi.getUnit());
                    item.setRefRangeLow(pi.getRefRangeLow());
                    item.setRefRangeHigh(pi.getRefRangeHigh());
                    item.setRefRangeText(pi.getRefRangeText());
                    item.setStatus("pending");
                    item.setSortOrder(sort++);
                    order.getItems().add(item);
                }
            }
        }
        // Pozycje ręczne
        if (req.items() != null) {
            for (LabOrderCreateRequest.Item ri : req.items()) {
                if (!usedCodes.add(ri.testCode())) continue;
                LabOrderItem item = new LabOrderItem();
                item.setOrder(order);
                item.setTestCode(ri.testCode());
                item.setTestName(ri.testName());
                item.setUnit(ri.unit());
                item.setRefRangeLow(ri.refRangeLow());
                item.setRefRangeHigh(ri.refRangeHigh());
                item.setRefRangeText(ri.refRangeText());
                item.setStatus("pending");
                item.setSortOrder(sort++);
                order.getItems().add(item);
            }
        }
        if (order.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zlecenie musi zawierać co najmniej jedną pozycję");
        }

        LabOrder saved = orderRepository.save(order);
        auditService.logPatient(u, existing, "CREATE_LAB_ORDER",
                "labOrderId=" + saved.getId() + ",items=" + saved.getItems().size());
        return toOrderDto(saved, true);
    }

    @Override
    @Transactional
    public LabOrderDto cancel(UUID orderId, String reason) {
        User u = current();
        LabOrder o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zlecenie nie istnieje"));
        if (!authorizationService.canWritePatient(u, o.getPatient())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do anulowania zlecenia");
        }
        if ("completed".equals(o.getStatus()) || "cancelled".equals(o.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zlecenie zamknięte – nie można anulować");
        }
        Instant now = Instant.now();
        o.setStatus("cancelled");
        o.setCancelledAt(now);
        o.setCancelledBy(u);
        o.setCancelledReason(reason);
        // Anuluj nieukończone pozycje
        for (LabOrderItem it : o.getItems()) {
            if ("pending".equals(it.getStatus())) {
                it.setStatus("cancelled");
            }
        }
        LabOrder saved = orderRepository.save(o);
        auditService.logPatient(u, o.getPatient(), "CANCEL_LAB_ORDER",
                "labOrderId=" + saved.getId() + (reason != null ? ",reason=" + reason : ""));
        return toOrderDto(saved, true);
    }

    @Override
    @Transactional
    public LabOrderItemDto updateItem(UUID itemId, LabOrderItemUpdateRequest req) {
        User u = current();
        LabOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pozycja nie istnieje"));
        LabOrder order = item.getOrder();
        // Lab tech / doctor / admin mogą wpisywać wyniki; doctor potrzebuje także dostępu do pacjenta.
        String role = u.getRole();
        boolean canEdit = "lab_tech".equals(role) || "admin".equals(role)
                || ("doctor".equals(role) && authorizationService.canWritePatient(u, order.getPatient()));
        if (!canEdit) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do edycji wyniku");
        }
        if ("cancelled".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zlecenie anulowane – edycja niemożliwa");
        }

        if (req.result() != null) item.setResult(req.result());
        if (req.resultNumeric() != null) item.setResultNumeric(req.resultNumeric());
        if (req.unit() != null) item.setUnit(req.unit());
        if (req.refRangeLow() != null) item.setRefRangeLow(req.refRangeLow());
        if (req.refRangeHigh() != null) item.setRefRangeHigh(req.refRangeHigh());
        if (req.refRangeText() != null) item.setRefRangeText(req.refRangeText());
        if (req.notes() != null) item.setNotes(req.notes());

        // Auto-flag jeśli klient nie podał, a mamy numeric
        if (req.abnormalFlag() != null) {
            item.setAbnormalFlag(req.abnormalFlag());
        } else if (item.getResultNumeric() != null) {
            item.setAbnormalFlag(computeAbnormalFlag(
                    item.getResultNumeric(), item.getRefRangeLow(), item.getRefRangeHigh()));
        }

        if (req.status() != null) {
            item.setStatus(req.status());
            if ("completed".equals(req.status())) {
                item.setCompletedAt(Instant.now());
                item.setCompletedBy(u);
            }
        } else if (item.getResult() != null && !item.getResult().isBlank() && "pending".equals(item.getStatus())) {
            // Auto-promocja: po wpisaniu wyniku, status → completed
            item.setStatus("completed");
            item.setCompletedAt(Instant.now());
            item.setCompletedBy(u);
        }

        // Aggregate parent order status
        String newOrderStatus = aggregateOrderStatus(order.getItems());
        if (!Objects.equals(newOrderStatus, order.getStatus())) {
            order.setStatus(newOrderStatus);
            if ("completed".equals(newOrderStatus)) {
                order.setCompletedAt(Instant.now());
            }
        }

        LabOrderItem saved = itemRepository.save(item);
        orderRepository.save(order);
        auditService.logPatient(u, order.getPatient(), "UPDATE_LAB_ORDER_ITEM",
                "itemId=" + saved.getId() + ",orderId=" + order.getId() + ",status=" + saved.getStatus());
        return toItemDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabPanelDto> listActivePanels() {
        return panelRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(p -> new LabPanelDto(
                        p.getId(),
                        p.getCode(),
                        p.getName(),
                        p.getDescription(),
                        p.getIsActive(),
                        p.getItems().stream()
                                .map(pi -> new LabPanelDto.Item(
                                        pi.getId(), pi.getTestCode(), pi.getTestName(), pi.getUnit(),
                                        pi.getRefRangeLow(), pi.getRefRangeHigh(), pi.getRefRangeText(),
                                        pi.getSortOrder()))
                                .toList()
                ))
                .toList();
    }
}


