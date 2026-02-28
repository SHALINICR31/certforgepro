package com.certforge.controller;

import com.certforge.model.CertHistory;
import com.certforge.model.User;
import com.certforge.repository.UserRepository;
import com.certforge.service.EmailService;
import com.certforge.service.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private static final Logger log = LoggerFactory.getLogger(CertificateController.class);

    @Autowired private EmailService emailService;
    @Autowired private HistoryService historyService;
    @Autowired private UserRepository userRepository;

    // ── Send email ────────────────────────────────────────────────────────
    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody SendEmailRequest req, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        if (req.getToEmail() == null || !req.getToEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid recipient email address"));
        }
        if (req.getCertificateBase64() == null || req.getCertificateBase64().length() < 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Certificate image is missing or invalid"));
        }
        boolean sent = emailService.sendCertificateEmail(
            req.getToEmail(), req.getRecipientName(),
            req.getFromName(), req.getFromEmail(),
            req.getSubject(), req.getBodyMessage(),
            req.getCertificateBase64()
        );
        return ResponseEntity.ok(Map.of("success", sent, "message", sent ? "Sent" : "Failed to send email"));
    }

    // ── Save history ──────────────────────────────────────────────────────
    @PostMapping("/save-history")
    public ResponseEntity<?> saveHistory(@RequestBody SaveHistoryRequest req, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        try {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();

            CertHistory h = new CertHistory();
            h.setUserId(user.getId());
            h.setUsername(auth.getName());
            h.setSessionName(req.getSessionName() != null ? req.getSessionName()
                : "Session " + LocalDateTime.now().toString().substring(0, 16));
            h.setTotalCertificates(req.getTotalCertificates());
            h.setEmailSent(req.isEmailSent());
            h.setEmailedCount(req.getEmailedCount());
            h.setContentTemplate(req.getContentTemplate());
            h.setIssuedBy(req.getIssuedBy());          // ★ save issuer
            h.setCreatedAt(LocalDateTime.now());

            List<CertHistory.CertRecord> records = new ArrayList<>();
            if (req.getCertificates() != null) {
                for (CertItem c : req.getCertificates()) {
                    CertHistory.CertRecord r = new CertHistory.CertRecord();
                    r.setCertificateNumber(c.getCertificateNumber());
                    r.setRecipientName(c.getName());
                    r.setRecipientEmail(c.getEmail() != null ? c.getEmail() : "");
                    r.setCourseName(c.getCourseName());    // ★ save course
                    r.setIssuedBy(req.getIssuedBy());      // ★ save issuer per-cert
                    r.setEmailed(c.isEmailed());
                    r.setEmailStatus(c.getEmailStatus() != null ? c.getEmailStatus() : "skipped");
                    r.setIssuedAt(LocalDateTime.now());
                    if (c.isEmailed()) r.setEmailedAt(LocalDateTime.now());
                    records.add(r);
                    log.info("  Saved: {} → {} | course: {} | by: {}",
                        c.getCertificateNumber(), c.getName(), c.getCourseName(), req.getIssuedBy());
                }
            }
            h.setCertificates(records);

            CertHistory saved = historyService.save(h);
            log.info("✅ History saved — {} certs, id={}", records.size(), saved.getId());

            return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "certCount", records.size(),
                "message", "Saved successfully"
            ));

        } catch (Exception e) {
            log.error("❌ save-history error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Save failed: " + e.getMessage()));
        }
    }

    // ── PUBLIC verify ─────────────────────────────────────────────────────
    @GetMapping("/verify/{certNumber}")
    public ResponseEntity<?> verify(@PathVariable String certNumber) {
        log.info("Verify request: '{}'", certNumber);
        String cleaned = certNumber.trim().toUpperCase();

        Optional<CertHistory.CertRecord> opt = historyService.findByCertNumber(cleaned);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Certificate not found in database"));
        }

        CertHistory.CertRecord r = opt.get();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("valid", true);
        res.put("certificateNumber", r.getCertificateNumber());
        res.put("issuedTo", r.getRecipientName() != null ? r.getRecipientName() : "");       // ★ "Issued To"
        res.put("issuedBy", r.getIssuedBy() != null ? r.getIssuedBy() : "");                  // ★ "Issued By"
        res.put("courseName", r.getCourseName() != null ? r.getCourseName() : "");            // ★ "Course / Programme"
        res.put("issuedAt", r.getIssuedAt() != null ? r.getIssuedAt().toString() : "");
        // Keep recipientName for backward compatibility
        res.put("recipientName", r.getRecipientName() != null ? r.getRecipientName() : "");
        log.info("✅ Verified: {} → {} (by: {})", cleaned, r.getRecipientName(), r.getIssuedBy());
        return ResponseEntity.ok(res);
    }

    // ── History CRUD ──────────────────────────────────────────────────────
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(historyService.getHistoryForUser(user.getId()));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable String id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean owned = historyService.sessionBelongsToUser(id, user.getId());
        if (!owned) {
            log.warn("SECURITY: User {} tried to delete session {} they don't own", auth.getName(), id);
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        historyService.deleteSession(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        historyService.deleteAllForUser(user.getId());
        return ResponseEntity.ok(Map.of("message", "Cleared"));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────

    public static class SendEmailRequest {
        private String toEmail, recipientName, fromName, fromEmail, subject, bodyMessage, certificateBase64;
        public String getToEmail() { return toEmail; }
        public void setToEmail(String v) { this.toEmail = v; }
        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String v) { this.recipientName = v; }
        public String getFromName() { return fromName; }
        public void setFromName(String v) { this.fromName = v; }
        public String getFromEmail() { return fromEmail; }
        public void setFromEmail(String v) { this.fromEmail = v; }
        public String getSubject() { return subject; }
        public void setSubject(String v) { this.subject = v; }
        public String getBodyMessage() { return bodyMessage; }
        public void setBodyMessage(String v) { this.bodyMessage = v; }
        public String getCertificateBase64() { return certificateBase64; }
        public void setCertificateBase64(String v) { this.certificateBase64 = v; }
    }

    public static class SaveHistoryRequest {
        private String sessionName, contentTemplate, issuedBy;   // ★ issuedBy added
        private int totalCertificates, emailedCount;
        private boolean emailSent;
        private List<CertItem> certificates;
        public String getSessionName() { return sessionName; }
        public void setSessionName(String v) { this.sessionName = v; }
        public String getContentTemplate() { return contentTemplate; }
        public void setContentTemplate(String v) { this.contentTemplate = v; }
        public String getIssuedBy() { return issuedBy; }
        public void setIssuedBy(String v) { this.issuedBy = v; }
        public int getTotalCertificates() { return totalCertificates; }
        public void setTotalCertificates(int v) { this.totalCertificates = v; }
        public int getEmailedCount() { return emailedCount; }
        public void setEmailedCount(int v) { this.emailedCount = v; }
        public boolean isEmailSent() { return emailSent; }
        public void setEmailSent(boolean v) { this.emailSent = v; }
        public List<CertItem> getCertificates() { return certificates; }
        public void setCertificates(List<CertItem> v) { this.certificates = v; }
    }

    public static class CertItem {
        private String name, email, emailStatus, certificateNumber, courseName;  // ★ courseName added
        private boolean emailed;
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { this.email = v; }
        public String getEmailStatus() { return emailStatus; }
        public void setEmailStatus(String v) { this.emailStatus = v; }
        public String getCertificateNumber() { return certificateNumber; }
        public void setCertificateNumber(String v) { this.certificateNumber = v; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String v) { this.courseName = v; }
        public boolean isEmailed() { return emailed; }
        public void setEmailed(boolean v) { this.emailed = v; }
    }
}
