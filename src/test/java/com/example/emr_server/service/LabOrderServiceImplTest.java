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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LabOrderServiceImpl}.
 *
 * Covered scenarios:
 *  - create: happy path with panel + manual item, deduplication, no items error,
 *            unauthorized patient, missing patient, missing panel
 *  - cancel: happy path, completed/cancelled (conflict), forbidden
 *  - updateItem: lab_tech happy path with auto-flag and order aggregation,
 *                forbidden role, cancelled order conflict
 *  - listActivePanels: mapping of panel + items
 *  - aggregateOrderStatus / computeAbnormalFlag (pure helpers)
 */
@ExtendWith(MockitoExtension.class)
class LabOrderServiceImplTest {

    @Mock private LabOrderRepository orderRepository;
    @Mock private LabOrderItemRepository itemRepository;
    @Mock private LabPanelRepository panelRepository;
    @Mock private UserRepository userRepository;
    @Mock private PatientService patientService;
    @Mock private AuthorizationService authorizationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private LabOrderServiceImpl service;

    private User doctor;
    private User labTech;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setUsername("doc");
        doctor.setRole("doctor");

        labTech = new User();
        labTech.setId(UUID.randomUUID());
        labTech.setUsername("lab1");
        labTech.setRole("lab_tech");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Jan");
        patient.setLastName("Kowalski");
    }

    // ── computeAbnormalFlag (pure helper) ──────────────────────────────

    @Test
    void computeAbnormalFlag_belowLow_returnsL() {
        // Given / When
        String flag = LabOrderServiceImpl.computeAbnormalFlag(
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("20"));

        // Then
        assertThat(flag).isEqualTo("L");
    }

    @Test
    void computeAbnormalFlag_aboveHigh_returnsH() {
        // Given / When
        String flag = LabOrderServiceImpl.computeAbnormalFlag(
                new BigDecimal("25"), new BigDecimal("10"), new BigDecimal("20"));

        // Then
        assertThat(flag).isEqualTo("H");
    }

    @Test
    void computeAbnormalFlag_inRange_returnsN() {
        // Given / When
        String flag = LabOrderServiceImpl.computeAbnormalFlag(
                new BigDecimal("15"), new BigDecimal("10"), new BigDecimal("20"));

        // Then
        assertThat(flag).isEqualTo("N");
    }

    @Test
    void computeAbnormalFlag_nullNumeric_returnsNull() {
        // Given / When
        String flag = LabOrderServiceImpl.computeAbnormalFlag(null, BigDecimal.ZERO, BigDecimal.TEN);

        // Then
        assertThat(flag).isNull();
    }

    // ── aggregateOrderStatus (pure helper) ─────────────────────────────

    @Test
    void aggregateOrderStatus_emptyList_returnsPending() {
        // Given / When
        String status = LabOrderServiceImpl.aggregateOrderStatus(List.of());

        // Then
        assertThat(status).isEqualTo("pending");
    }

    @Test
    void aggregateOrderStatus_allCompleted_returnsCompleted() {
        // Given
        LabOrderItem a = itemWithStatus("completed");
        LabOrderItem b = itemWithStatus("completed");

        // When
        String status = LabOrderServiceImpl.aggregateOrderStatus(List.of(a, b));

        // Then
        assertThat(status).isEqualTo("completed");
    }

    @Test
    void aggregateOrderStatus_someCompleted_someExpressedPending_returnsInProgress() {
        // Given
        LabOrderItem a = itemWithStatus("completed");
        LabOrderItem b = itemWithStatus("pending");

        // When
        String status = LabOrderServiceImpl.aggregateOrderStatus(List.of(a, b));

        // Then
        assertThat(status).isEqualTo("in_progress");
    }

    @Test
    void aggregateOrderStatus_allCancelled_returnsCancelled() {
        // Given
        LabOrderItem a = itemWithStatus("cancelled");
        LabOrderItem b = itemWithStatus("cancelled");

        // When
        String status = LabOrderServiceImpl.aggregateOrderStatus(List.of(a, b));

        // Then
        assertThat(status).isEqualTo("cancelled");
    }

    private LabOrderItem itemWithStatus(String s) {
        LabOrderItem it = new LabOrderItem();
        it.setStatus(s);
        return it;
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_doctorWithPanelAndManualItem_persistsOrderWithDeduplicatedItems() {
        // Given: panel z 2 testami i ręczna pozycja z duplikującym się kodem CRP (powinna być pominięta)
        UUID panelId = UUID.randomUUID();
        LabPanel panel = new LabPanel();
        panel.setId(panelId);
        LabPanelItem pi1 = panelItem("CRP", "Białko C-reaktywne", "mg/L");
        LabPanelItem pi2 = panelItem("WBC", "Leukocyty", "10^3/uL");
        panel.getItems().add(pi1);
        panel.getItems().add(pi2);

        LabOrderCreateRequest req = new LabOrderCreateRequest(
                patient.getId(),
                "urgent",
                "Pilne kontrolne",
                List.of(panelId),
                List.of(new LabOrderCreateRequest.Item("CRP", "Duplikat – pominięty", null, null, null, null),
                        new LabOrderCreateRequest.Item("ESR", "OB", "mm/h", null, null, null))
        );

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(panelRepository.findById(panelId)).thenReturn(Optional.of(panel));
        when(orderRepository.save(any(LabOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        LabOrderDto dto;
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            dto = service.create(req);
        }

        // Then: 3 unikalne pozycje (CRP z panelu, WBC z panelu, ESR ręczne)
        assertThat(dto).isNotNull();
        assertThat(dto.priority()).isEqualTo("urgent");
        assertThat(dto.status()).isEqualTo("pending");
        assertThat(dto.itemCount()).isEqualTo(3);
        assertThat(dto.items()).hasSize(3)
                .extracting("testCode")
                .containsExactly("CRP", "WBC", "ESR");

        ArgumentCaptor<LabOrder> orderCaptor = ArgumentCaptor.forClass(LabOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        LabOrder saved = orderCaptor.getValue();
        assertThat(saved.getOrderedBy()).isEqualTo(doctor);
        assertThat(saved.getPatient()).isEqualTo(patient);
        assertThat(saved.getItems()).hasSize(3);

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_LAB_ORDER"), anyString());
    }

    @Test
    void create_noItemsAfterDedup_throwsBadRequest() {
        // Given: panel pusty, brak ręcznych pozycji
        UUID panelId = UUID.randomUUID();
        LabPanel panel = new LabPanel();
        panel.setId(panelId);
        LabOrderCreateRequest req = new LabOrderCreateRequest(
                patient.getId(), "routine", null, List.of(panelId), List.of());

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(panelRepository.findById(panelId)).thenReturn(Optional.of(panel));

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("co najmniej jedną pozycję");
        }

        verify(orderRepository, never()).save(any());
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    @Test
    void create_unauthorizedDoctor_throwsForbidden() {
        // Given
        LabOrderCreateRequest req = new LabOrderCreateRequest(
                patient.getId(), "routine", null, List.of(), List.of(
                        new LabOrderCreateRequest.Item("X", "Test X", null, null, null, null)));

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Brak uprawnień");
        }

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_missingPatient_throwsNotFound() {
        // Given
        LabOrderCreateRequest req = new LabOrderCreateRequest(
                patient.getId(), null, null, List.of(), List.of());
        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.empty());

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Pacjent nie istnieje");
        }
    }

    @Test
    void create_missingPanel_throwsBadRequest() {
        // Given
        UUID panelId = UUID.randomUUID();
        LabOrderCreateRequest req = new LabOrderCreateRequest(
                patient.getId(), "routine", null, List.of(panelId), List.of());

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(panelRepository.findById(panelId)).thenReturn(Optional.empty());

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Panel nie istnieje");
        }
    }

    // ── cancel ────────────────────────────────────────────────────────

    @Test
    void cancel_pendingOrder_marksOrderAndPendingItems() {
        // Given
        LabOrder order = new LabOrder();
        order.setId(UUID.randomUUID());
        order.setPatient(patient);
        order.setOrderedBy(doctor);
        order.setStatus("in_progress");
        LabOrderItem completed = itemWithStatus("completed");
        LabOrderItem pending = itemWithStatus("pending");
        order.getItems().add(completed);
        order.getItems().add(pending);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(orderRepository.save(any(LabOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        LabOrderDto dto;
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            dto = service.cancel(order.getId(), "Pomyłka");
        }

        // Then
        assertThat(dto.status()).isEqualTo("cancelled");
        assertThat(order.getCancelledBy()).isEqualTo(doctor);
        assertThat(order.getCancelledReason()).isEqualTo("Pomyłka");
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(completed.getStatus()).isEqualTo("completed"); // nie zmienione
        assertThat(pending.getStatus()).isEqualTo("cancelled");
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CANCEL_LAB_ORDER"), anyString());
    }

    @Test
    void cancel_alreadyCompletedOrder_throwsConflict() {
        // Given
        LabOrder order = new LabOrder();
        order.setId(UUID.randomUUID());
        order.setPatient(patient);
        order.setStatus("completed");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.cancel(order.getId(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Zlecenie zamknięte");
        }
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_unauthorizedDoctor_throwsForbidden() {
        // Given
        LabOrder order = new LabOrder();
        order.setId(UUID.randomUUID());
        order.setPatient(patient);
        order.setStatus("pending");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.cancel(order.getId(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Brak uprawnień");
        }
    }

    // ── updateItem ────────────────────────────────────────────────────

    @Test
    void updateItem_labTechProvidesNumericResult_autoFlagsAndPromotesStatuses() {
        // Given: pojedyncza pozycja w zleceniu, lab_tech wpisuje wynik 8.0 przy normie 0–5 → flag H,
        // status pozycji → completed, status zlecenia → completed.
        LabOrder order = new LabOrder();
        order.setId(UUID.randomUUID());
        order.setPatient(patient);
        order.setStatus("pending");

        LabOrderItem item = new LabOrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setTestCode("CRP");
        item.setTestName("CRP");
        item.setStatus("pending");
        item.setRefRangeLow(BigDecimal.ZERO);
        item.setRefRangeHigh(new BigDecimal("5"));
        order.getItems().add(item);

        LabOrderItemUpdateRequest req = new LabOrderItemUpdateRequest(
                "8.0", new BigDecimal("8.0"), null, null, null, null, null, null, null);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(LabOrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(LabOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        LabOrderItemDto dto;
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(labTech));
            dto = service.updateItem(item.getId(), req);
        }

        // Then
        assertThat(dto.abnormalFlag()).isEqualTo("H");
        assertThat(dto.status()).isEqualTo("completed");
        assertThat(item.getCompletedBy()).isEqualTo(labTech);
        assertThat(item.getCompletedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("completed");
        assertThat(order.getCompletedAt()).isNotNull();
        verify(itemRepository).save(item);
        verify(orderRepository).save(order);
        verify(auditService).logPatient(eq(labTech), eq(patient), eq("UPDATE_LAB_ORDER_ITEM"), anyString());
    }

    @Test
    void updateItem_nurseRole_throwsForbidden() {
        // Given
        User nurse = new User();
        nurse.setId(UUID.randomUUID());
        nurse.setRole("nurse");

        LabOrder order = new LabOrder();
        order.setPatient(patient);
        order.setStatus("pending");
        LabOrderItem item = new LabOrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setStatus("pending");

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(nurse));
            assertThatThrownBy(() -> service.updateItem(item.getId(),
                    new LabOrderItemUpdateRequest("x", null, null, null, null, null, null, null, null)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Brak uprawnień");
        }
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_cancelledOrder_throwsConflict() {
        // Given
        LabOrder order = new LabOrder();
        order.setPatient(patient);
        order.setStatus("cancelled");
        LabOrderItem item = new LabOrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(labTech));
            assertThatThrownBy(() -> service.updateItem(item.getId(),
                    new LabOrderItemUpdateRequest("x", null, null, null, null, null, null, null, null)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("anulowane");
        }
    }

    @Test
    void updateItem_missingItem_throwsNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(labTech));
            assertThatThrownBy(() -> service.updateItem(id,
                    new LabOrderItemUpdateRequest(null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Pozycja nie istnieje");
        }
    }

    // ── listActivePanels ──────────────────────────────────────────────

    @Test
    void listActivePanels_mapsPanelsAndItems() {
        // Given
        LabPanel panel = new LabPanel();
        panel.setId(UUID.randomUUID());
        panel.setCode("LIPIDS");
        panel.setName("Lipidogram");
        panel.setIsActive(true);
        panel.getItems().add(panelItem("LDL", "LDL", "mg/dL"));
        when(panelRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(panel));

        // When
        List<LabPanelDto> dtos = service.listActivePanels();

        // Then
        assertThat(dtos).hasSize(1);
        LabPanelDto p = dtos.get(0);
        assertThat(p.code()).isEqualTo("LIPIDS");
        assertThat(p.name()).isEqualTo("Lipidogram");
        assertThat(p.items()).hasSize(1);
        assertThat(p.items().get(0).testCode()).isEqualTo("LDL");
        verify(panelRepository, times(1)).findAllByIsActiveTrueOrderByNameAsc();
    }

    private LabPanelItem panelItem(String code, String name, String unit) {
        LabPanelItem pi = new LabPanelItem();
        pi.setId(UUID.randomUUID());
        pi.setTestCode(code);
        pi.setTestName(name);
        pi.setUnit(unit);
        pi.setSortOrder(0);
        return pi;
    }
}

