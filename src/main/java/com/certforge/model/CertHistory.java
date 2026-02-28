package com.certforge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "cert_history")
public class CertHistory {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String username;
    private String sessionName;
    private int totalCertificates;
    private boolean emailSent;
    private int emailedCount;
    private String contentTemplate;
    private String issuedBy;      // ★ NEW — institution/organisation name e.g. "Anna University"
    private List<CertRecord> certificates;
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    public int getTotalCertificates() { return totalCertificates; }
    public void setTotalCertificates(int t) { this.totalCertificates = t; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean b) { this.emailSent = b; }
    public int getEmailedCount() { return emailedCount; }
    public void setEmailedCount(int c) { this.emailedCount = c; }
    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String s) { this.contentTemplate = s; }
    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String s) { this.issuedBy = s; }
    public List<CertRecord> getCertificates() { return certificates; }
    public void setCertificates(List<CertRecord> c) { this.certificates = c; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public static class CertRecord {

        private String certificateNumber;   // e.g. CERT-20240115-0001
        private String recipientName;       // Issued TO
        private String recipientEmail;
        private String courseName;          // ★ NEW — what they completed e.g. "Python Programming"
        private String issuedBy;            // ★ NEW — who issued it e.g. "Anna University"
        private boolean emailed;
        private String emailStatus;
        private LocalDateTime issuedAt;
        private LocalDateTime emailedAt;

        public String getCertificateNumber() { return certificateNumber; }
        public void setCertificateNumber(String v) { this.certificateNumber = v; }
        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String v) { this.recipientName = v; }
        public String getRecipientEmail() { return recipientEmail; }
        public void setRecipientEmail(String v) { this.recipientEmail = v; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String v) { this.courseName = v; }
        public String getIssuedBy() { return issuedBy; }
        public void setIssuedBy(String v) { this.issuedBy = v; }
        public boolean isEmailed() { return emailed; }
        public void setEmailed(boolean v) { this.emailed = v; }
        public String getEmailStatus() { return emailStatus; }
        public void setEmailStatus(String v) { this.emailStatus = v; }
        public LocalDateTime getIssuedAt() { return issuedAt; }
        public void setIssuedAt(LocalDateTime v) { this.issuedAt = v; }
        public LocalDateTime getEmailedAt() { return emailedAt; }
        public void setEmailedAt(LocalDateTime v) { this.emailedAt = v; }
    }
}
