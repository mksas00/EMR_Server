package com.example.emr_server.service;

import com.example.emr_server.entity.Prescription;
import com.example.emr_server.entity.PrescriptionMedication;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PrescriptionRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrescriptionPdfService {

    private final PrescriptionRepository prescriptionRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PrescriptionPdfService(PrescriptionRepository prescriptionRepository,
                                  AuthorizationService authorizationService,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> generatePdfForVisible(UUID id) {
        User current = SecurityUtil.getCurrentUser(userRepository).orElse(null);
        return prescriptionRepository.findById(id)
                .filter(p -> authorizationService.canReadPatient(current, p.getPatient()))
                .map(p -> {
                    // audyt pobrania PDF
                    try { auditService.logPatient(current, p.getPatient(), "PDF_DOWNLOAD", "prescriptionId=" + p.getId()); } catch (Exception ignored) {}
                    return render(p);
                });
    }

    private static final class RenderCtx implements AutoCloseable {
        final PDDocument doc;
        final PDRectangle box;
        final float margin;
        final float startY;
        PDPageContentStream cs;
        float y;
        final PDFont regular;
        final PDFont bold;
        final PDFont italic;
        final float leading = 14f; // odstęp między liniami

        RenderCtx(PDDocument doc, PDRectangle box, float margin) throws IOException {
            this.doc = doc;
            this.box = box;
            this.margin = margin;
            this.startY = box.getHeight() - margin;
            this.y = startY;
            this.regular = loadFontOrDefault(doc, "fonts/NotoSans-Regular.ttf", PDType1Font.HELVETICA);
            this.bold = loadFontOrDefault(doc, "fonts/NotoSans-Bold.ttf", PDType1Font.HELVETICA_BOLD);
            this.italic = loadFontOrDefault(doc, "fonts/NotoSans-Italic.ttf", PDType1Font.HELVETICA_OBLIQUE);
            newPage();
        }

        private PDFont loadFontOrDefault(PDDocument doc, String classpath, PDFont fallback) {
            try {
                ClassPathResource res = new ClassPathResource(classpath);
                if (res.exists()) {
                    try (InputStream is = res.getInputStream()) {
                        return PDType0Font.load(doc, is, true);
                    }
                }
            } catch (Exception ignored) {}
            return fallback;
        }

        void newPage() throws IOException {
            if (cs != null) cs.close();
            PDPage page = new PDPage(box);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = startY;
        }

        void ensureSpace(float needed) throws IOException {
            float minY = 60f;
            if (y - needed < minY) {
                newPage();
            }
        }

        void writeLine(PDFont font, int size, float x, float y, String text) throws IOException {
            String t = text != null ? text : "";
            try {
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(x, y);
                cs.showText(t);
                cs.endText();
            } catch (IllegalArgumentException iae) {
                String ascii = Normalizer.normalize(t, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                        .replaceAll("[^\\x20-\\x7E]", "?");
                try { cs.endText(); } catch (Exception ignore) {}
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(x, y);
                cs.showText(ascii);
                cs.endText();
            }
        }

        void titleBar(String text) throws IOException {
            float height = 28f;
            cs.setNonStrokingColor(new Color(240,240,240));
            cs.addRect(margin - 5, y - height + 6, box.getWidth() - 2*margin + 10, height);
            cs.fill();
            cs.setNonStrokingColor(Color.BLACK);
            writeLine(bold, 18, margin, y, text);
            y -= height;
        }

        void rule() throws IOException {
            cs.setStrokingColor(new Color(200,200,200));
            cs.moveTo(margin, y);
            cs.lineTo(box.getWidth() - margin, y);
            cs.stroke();
            cs.setStrokingColor(Color.BLACK);
        }

        List<String> wrapText(PDFont font, int fontSize, String text, float maxWidth) throws IOException {
            String t = text == null ? "" : text.replace('\n', ' ');
            List<String> lines = new ArrayList<>();
            String[] words = t.split(" ");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                String candidate = line.length() == 0 ? w : line + " " + w;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;
                if (width <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (line.length() > 0) {
                        lines.add(line.toString());
                        line = new StringBuilder(w);
                    } else {
                        // słowo dłuższe niż kolumna – hard split
                        lines.add(w);
                        line = new StringBuilder();
                    }
                }
            }
            if (line.length() > 0) lines.add(line.toString());
            return lines;
        }

        void table(String[] headers, float[] widths, List<String[]> rows) throws IOException {
            float tableWidth = 0;
            for (float w : widths) tableWidth += w;
            float x = margin;

            // Header background
            ensureSpace(40); // więcej miejsca na nagłówek
            float headerHeight = 20f;
            cs.setNonStrokingColor(new Color(230,230,230));
            cs.addRect(x, y - headerHeight + 2, tableWidth, headerHeight);
            cs.fill();
            cs.setNonStrokingColor(Color.BLACK);

            // Header texts (nieco niżej, by lepiej centrować w belce)
            float cellX = x;
            for (int i=0;i<headers.length;i++) {
                writeLine(bold, 12, cellX + 6, y - 6, headers[i]);
                cellX += widths[i];
            }
            // duży odstęp pod nagłówkiem, aby pierwszy wiersz nie nachodził
            y -= (headerHeight + 22); // = 42px

            // Rows
            for (String[] row : rows) {
                // wrap columns 0 and last
                List<String> col0 = wrapText(regular, 12, row[0], widths[0] - 8);
                List<String> col1 = List.of(row[1]);
                List<String> col2 = List.of(row[2]);
                List<String> col3 = wrapText(regular, 12, row[3], widths[3] - 8);
                int lines = Math.max(Math.max(col0.size(), col1.size()), Math.max(col2.size(), col3.size()));
                float rowHeight = Math.max(lines * leading, 16f);
                ensureSpace(rowHeight + 10);

                // cell borders
                float cx = x;
                cs.setStrokingColor(new Color(210,210,210));
                for (float w : widths) {
                    cs.addRect(cx, y, w, rowHeight + 8);
                    cs.stroke();
                    cx += w;
                }
                cs.setStrokingColor(Color.BLACK);

                // draw text lines (zwiększony padding pionowy)
                for (int li = 0; li < lines; li++) {
                    float lx = x + 6;
                    float ly = y + rowHeight + 4 - li * leading;
                    writeLine(regular, 12, lx, ly, li < col0.size() ? col0.get(li) : "");
                    lx += widths[0];
                    writeLine(regular, 12, lx + 6, ly, li < col1.size() ? col1.get(li) : "");
                    lx += widths[1];
                    writeLine(regular, 12, lx + 6, ly, li < col2.size() ? col2.get(li) : "");
                    lx += widths[2];
                    writeLine(regular, 12, lx + 6, ly, li < col3.size() ? col3.get(li) : "");
                }

                y -= (rowHeight + 8);
            }
        }

        @Override public void close() throws IOException { if (cs != null) cs.close(); }
    }

    private byte[] render(Prescription p) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDRectangle box = PDRectangle.A4;
            try (RenderCtx r = new RenderCtx(doc, box, 50)) {
                DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

                // Title bar
                r.titleBar("Recepta");

                // Patient and dates
                String patientName = p.getPatient()!=null ? (safe(p.getPatient().getFirstName())+" "+safe(p.getPatient().getLastName())) : "";
                r.writeLine(r.regular, 12, r.margin, r.y, "Pacjent: " + patientName);
                r.y -= 14;
                r.writeLine(r.regular, 12, r.margin, r.y, "PESEL: " + (p.getPatient()!=null? safe(p.getPatient().getPesel()): ""));
                r.y -= 14;
                r.writeLine(r.regular, 12, r.margin, r.y, "Wystawiono: " + (p.getIssuedDate()!=null? p.getIssuedDate().format(df): "-"));
                r.y -= 14;
                r.writeLine(r.regular, 12, r.margin, r.y, "Ważna do: " + (p.getExpirationDate()!=null? p.getExpirationDate().format(df): "-"));
                r.y -= 18;

                if (p.getDosageInfo()!=null && !p.getDosageInfo().isBlank()) {
                    r.writeLine(r.italic, 11, r.margin, r.y, "Zalecenia ogólne: " + safe(p.getDosageInfo()));
                    r.y -= 18;
                }

                r.rule();
                r.y -= 10;

                // Items table
                String[] headers = new String[]{"Lek", "Ilość", "Jedn.", "Dawkowanie"};
                float total = box.getWidth() - 2*r.margin;
                float[] widths = new float[]{ total * 0.35f, total * 0.12f, total * 0.12f, total * 0.41f };
                List<String[]> rows = new ArrayList<>();
                if (p.getItems()!=null) {
                    for (PrescriptionMedication it : p.getItems()) {
                        String medName = (it.getMedication()!=null && it.getMedication().getName()!=null)
                                ? it.getMedication().getName()
                                : (it.getMedication()!=null? it.getMedication().getId().toString(): "-");
                        rows.add(new String[]{
                                medName,
                                it.getQuantity()!=null? String.valueOf(it.getQuantity()): "-",
                                safe(it.getUnit()),
                                safe(it.getDosageInfo())
                        });
                    }
                }
                r.table(headers, widths, rows);

                r.y -= 20;
                r.rule();
                r.y -= 10;

                r.writeLine(r.regular, 12, r.margin, r.y, "Lekarz: " + (p.getDoctor()!=null? safe(p.getDoctor().getUsername()): "-"));
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private String safe(String v) { return v==null? "": v; }
}
