package com.certforge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends certificate emails via Resend HTTP API.
 *
 * WHY RESEND (not Gmail SMTP)?
 *   Render.com free tier blocks SMTP ports 25/465/587.
 *   Resend uses HTTPS port 443 — works everywhere, no card needed.
 *   Free tier: 3,000 emails/month, 100/day. No credit card required.
 *
 * SETUP (3 minutes — see deployment guide for screenshots):
 *   1. Go to https://resend.com  → Sign up FREE (no credit card)
 *   2. Dashboard → API Keys → Create API Key → copy it
 *   3. Dashboard → Domains → Add Domain (OR use resend's test domain for now)
 *   4. Add to Render environment variables:
 *        RESEND_API_KEY    = re_xxxxxxxxxxxxxxxxxxxx
 *        RESEND_FROM_EMAIL = onboarding@resend.dev      ← use this for testing (no domain needed)
 *        RESEND_FROM_NAME  = CertForge Pro
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String resendFromEmail;

    @Value("${resend.from.name:CertForge Pro}")
    private String resendFromName;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendCertificateEmail(String toEmail,
                                         String recipientName,
                                         String fromName,
                                         String fromMailAddr,
                                         String subject,
                                         String bodyMessage,
                                         String certBase64) {

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("❌ RESEND_API_KEY not set. Add it to Render environment variables.");
            return false;
        }

        try {
            // Clean base64 (remove data:image/png;base64, prefix if present)
            String b64 = certBase64.contains(",") ? certBase64.split(",")[1] : certBase64;

            String safeFilename = (recipientName != null
                ? recipientName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                : "Recipient") + "_Certificate.png";

            String displaySender = (fromName != null && !fromName.isBlank()) ? fromName : resendFromName;
            String replyTo = (fromMailAddr != null && fromMailAddr.contains("@")) ? fromMailAddr : null;

            String body = (bodyMessage != null && !bodyMessage.isBlank())
                ? bodyMessage
                : "Dear " + recipientName + ",\n\nCongratulations! Please find your certificate attached.\n\nBest regards,\n" + displaySender;

            String fromHeader = displaySender + " <" + resendFromEmail + ">";

            // Resend API payload
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("from", fromHeader);
            payload.put("to", List.of(toEmail));
            payload.put("subject", (subject != null && !subject.isBlank()) ? subject : "Your Certificate 🎓");
            payload.put("html", buildHtml(recipientName, body, displaySender));
            if (replyTo != null) payload.put("reply_to", replyTo);
            payload.put("attachments", List.of(Map.of(
                "content", b64,
                "filename", safeFilename
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email sent via Resend to {}", toEmail);
                return true;
            } else {
                log.error("❌ Resend API error: {} — {}", response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Failed sending to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String buildHtml(String name, String body, String senderName) {
        String safe = body
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\n", "<br>");
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><style>
              body{font-family:Segoe UI,Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
              .wrap{max-width:560px;margin:28px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 18px rgba(0,0,0,.1)}
              .hdr{background:linear-gradient(135deg,#C9A84C,#8B6914);padding:26px 30px;text-align:center}
              .hdr h1{color:#fff;margin:0;font-size:22px;font-weight:700}
              .hdr p{color:rgba(255,255,255,.8);margin:5px 0 0;font-size:12px}
              .body{padding:26px 30px}
              .body p{color:#444;font-size:14px;line-height:1.7;margin:0 0 12px}
              .attach{background:#fffbf0;border-left:3px solid #C9A84C;padding:11px 15px;border-radius:4px;font-size:13px;color:#666;margin:16px 0}
              .foot{background:#f9f9f9;padding:14px 30px;text-align:center;font-size:11px;color:#999;border-top:1px solid #eee}
            </style></head>
            <body><div class="wrap">
              <div class="hdr"><h1>🎓 CertForge Pro</h1><p>Certificate Generation System</p></div>
              <div class="body">
                <p>Dear <strong>%s</strong>,</p>
                <p>%s</p>
                <div class="attach">📎 Your certificate is attached as a PNG image.</div>
                <p style="margin-top:18px">Best regards,<br><strong>%s</strong></p>
              </div>
              <div class="foot">Sent via CertForge Pro</div>
            </div></body></html>
            """.formatted(name, safe, senderName);
    }
}
