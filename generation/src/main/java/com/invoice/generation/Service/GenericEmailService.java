package com.invoice.generation.Service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class GenericEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(GenericEmailService.class);

    @Value("${email.api.url}")
    private String apiUrl;

    @Value("${email.smtp.host}")
    private String smtpHost;

    @Value("${email.smtp.port}")
    private String smtpPort;

    @Value("${email.smtp.user}")
    private String smtpUser;

    @Value("${email.smtp.password}")
    private String smtpPassword; // NEVER log

    @Value("${email.from.address}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public void sendEmail(
            String to,
            String customerName,
            String invoiceStatus,
            String date,
            File attachment
    ) {

        log.info("========== EMAIL FLOW START ==========");

        /* STEP 1: INPUT */
        log.info("STEP 1 → Inputs");
        log.info("to={}, customerName={}, invoiceStatus={}, date={}",
                to, customerName, invoiceStatus, date);
        log.info("attachment={}",
                attachment != null ? attachment.getAbsolutePath() : "null");

        /* STEP 2: VALIDATION */
        log.info("STEP 2 → Validation");
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email (to) is required");
        }

        /* STEP 3: SUBJECT & BODY */
        log.info("STEP 3 → Building subject & body");

        String subject = "Thank You | "
                + customerName + " | "
                + invoiceStatus + " | "
                + date;

        String textBody =
                "Dear " + customerName + ",\n\n"
                + "Thank you for choosing The Tinkori Tales.\n"
                + "Invoice Status: " + invoiceStatus + "\n\n"
                + "Best regards,\n"
                + "The Tinkori Tales\n\n"
                + "Diptimoy Hazra\n"
                + "Finance & Accounts\n"
                + "For support email us at thetinkoritales@gmail.com";

        /* STEP 4: BUILD MULTIPART FORM */
        log.info("STEP 4 → Building multipart form-data");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("to", textPart(to));
        body.add("from", textPart("The Tinkori Tales <" + from + ">"));
        body.add("subject", textPart(subject));
        body.add("text", textPart(textBody));

        body.add("emailHost", textPart(smtpHost));
        body.add("emailPort", textPart(smtpPort));
        body.add("emailUser", textPart(smtpUser));
        body.add("emailPassword", textPart(smtpPassword));

        if (cc != null && !cc.isBlank()) {
            for (String c : cc.split(",")) {
                body.add("cc", textPart(c.trim()));
            }
        }

        if (bcc != null && !bcc.isBlank()) {
            for (String b : bcc.split(",")) {
                body.add("bcc", textPart(b.trim()));
            }
        }

        if (attachment != null && attachment.exists() && attachment.length() > 0) {
            body.add("file", new FileSystemResource(attachment));
        }

        /* STEP 5: LOG FINAL FORM DATA */
        log.info("STEP 5 → FINAL FORM DATA BEFORE SENDING");

        body.forEach((key, values) -> {
            for (Object value : values) {
                if (value instanceof FileSystemResource file) {
                    try {
                        log.info("FORM → {} = FILE[name={}, size={} bytes]",
                                key, file.getFilename(), file.contentLength());
                    } catch (Exception e) {
                        log.info("FORM → {} = FILE[name={}]", key, file.getFilename());
                    }
                } else if (value instanceof HttpEntity<?> entity) {
                    if ("emailPassword".equals(key)) {
                        log.info("FORM → {} = ****** (hidden)", key);
                    } else {
                        log.info("FORM → {} = {}", key, entity.getBody());
                    }
                }
            }
        });

        /* STEP 6: REQUEST */
        log.info("STEP 6 → Preparing HTTP request");

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        /* STEP 7: REST TEMPLATE */
        log.info("STEP 7 → Configuring RestTemplate");

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().clear();
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        restTemplate.getMessageConverters().add(new ResourceHttpMessageConverter());

        /* STEP 8: SEND */
        log.info("STEP 8 → Sending request");
        log.info("External API URL → {}", apiUrl);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(apiUrl, request, String.class);

            /* STEP 9: RESPONSE */
            log.info("STEP 9 → Response received");
            log.info("Status={}, Body={}",
                    response.getStatusCode(),
                    response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Email failed: " + response.getBody());
            }

            log.info("STEP 10 → Email sent successfully");

        } catch (Exception e) {
            log.error("STEP 10 → Email sending FAILED", e);
            throw new RuntimeException("Email sending failed", e);
        }

        log.info("========== EMAIL FLOW END ==========");
    }

    /* ================= HELPER ================= */

    private HttpEntity<String> textPart(String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(value, headers);
    }
}
