package com.invoice.generation.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MailerooEmailService {

    @Value("${maileroo.api.url}")
    private String mailerooApiUrl;

    @Value("${maileroo.api.key}")
    private String apiKey;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.display}")
    private String fromDisplay;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public void sendEmailWithInvoice(String to, String pdfPath, String customerName,
            String invoiceStatus, String date) {

        try {
            // Validate PDF file
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists() || pdfFile.length() == 0) {
                throw new IllegalArgumentException("Invalid PDF file: " + pdfPath);
            }

            // Read and encode PDF to Base64
            byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            // Build JSON payload
            Map<String, Object> payload = new HashMap<>();

            // FROM
            Map<String, String> fromObject = new HashMap<>();
            fromObject.put("address", fromAddress);
            fromObject.put("display_name", fromDisplay);
            payload.put("from", fromObject);

            // TO
            List<Map<String, String>> toArray = new ArrayList<>();
            Map<String, String> toObject = new HashMap<>();
            toObject.put("address", to);
            toArray.add(toObject);
            payload.put("to", toArray);

            // CC (optional)
            if (cc != null && !cc.isBlank()) {
                String[] ccEmails = cc.split(",");
                if (ccEmails.length == 1) {
                    Map<String, String> ccObject = new HashMap<>();
                    ccObject.put("address", ccEmails[0].trim());
                    payload.put("cc", ccObject);
                } else {
                    List<Map<String, String>> ccArray = new ArrayList<>();
                    for (String email : ccEmails) {
                        Map<String, String> ccObject = new HashMap<>();
                        ccObject.put("address", email.trim());
                        ccArray.add(ccObject);
                    }
                    payload.put("cc", ccArray);
                }
            }

            // BCC (optional)
            if (bcc != null && !bcc.isBlank()) {
                String[] bccEmails = bcc.split(",");
                List<Map<String, String>> bccArray = new ArrayList<>();
                for (String email : bccEmails) {
                    Map<String, String> bccObject = new HashMap<>();
                    bccObject.put("address", email.trim());
                    bccArray.add(bccObject);
                }
                payload.put("bcc", bccArray);
            }

            // SUBJECT
            String subject = "Thank You | " + customerName + " | " + invoiceStatus + " | " + date;
            payload.put("subject", subject);

            // PLAIN TEXT BODY
            String plainBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for choosing The Tinkori Tales.\n" +
                "Invoice Status: %s\n\n" +
                "Best regards,\n" +
                "The Tinkori Tales\n\n" +
                "Diptimoy Hazra\n" +
                "Finance & Accounts\n" +
                "For support email us at thetinkoritales@gmail.com",
                customerName, invoiceStatus
            );
            payload.put("plain", plainBody);

            // TRACKING
            payload.put("tracking", true);

            // ATTACHMENTS
            List<Map<String, Object>> attachmentsArray = new ArrayList<>();
            Map<String, Object> attachmentObject = new HashMap<>();
            attachmentObject.put("file_name", "invoice_" + invoiceStatus + ".pdf");
            attachmentObject.put("content_type", "application/pdf");
            attachmentObject.put("content", base64Content);
            attachmentsArray.add(attachmentObject);
            payload.put("attachments", attachmentsArray);

            // Setup headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            // Create HTTP request
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Send to Maileroo
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                mailerooApiUrl,
                request,
                String.class
            );

            // Check response
            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.CREATED ||
                response.getStatusCode() == HttpStatus.ACCEPTED) {
                
                System.out.println("Email sent successfully to: " + to);
                
                // Delete temp PDF
                pdfFile.delete();
            } else {
                throw new RuntimeException("Email sending failed with status: " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Maileroo API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to send email via Maileroo API", e);
            
        } catch (IOException e) {
            System.err.println("PDF file error: " + e.getMessage());
            throw new RuntimeException("Failed to read PDF file", e);
            
        } catch (Exception e) {
            System.err.println("Email sending error: " + e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }
}