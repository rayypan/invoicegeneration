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
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Primary
public class MailerooEmailService {

    @Value("${maileroo.api.url:https://smtp.maileroo.com/api/v2/emails}")
    private String mailerooApiUrl;

    @Value("${maileroo.api.key}")
    private String apiKey;

    @Value("${email.from}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public void sendEmailWithInvoice(String to, String pdfPath, String customerName,
            String invoiceStatus, String date) {

        System.out.println("\n========================================");
        System.out.println("📧 MAILEROO EMAIL SEND - START");
        System.out.println("========================================");

        try {
            // STEP 1: Validate PDF
            System.out.println("🔵 STEP 1: Validating PDF file");
            System.out.println("   Path: " + pdfPath);
            
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                throw new IllegalArgumentException("PDF not found: " + pdfPath);
            }
            
            long fileSize = pdfFile.length();
            if (fileSize == 0) {
                throw new IllegalArgumentException("PDF is empty");
            }
            System.out.println("✅ PDF Valid: " + fileSize + " bytes");

            // STEP 2: Read and encode PDF
            System.out.println("🔵 STEP 2: Encoding PDF to Base64");
            byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            System.out.println("✅ Base64 Length: " + base64Content.length());

            // STEP 3: Build JSON payload - EXACT Maileroo format
            System.out.println("🔵 STEP 3: Building Maileroo API payload");
            Map<String, Object> payload = new HashMap<>();

            // FROM (required) - object with address and display_name
            Map<String, String> fromObject = new HashMap<>();
            if (from.contains("<")) {
                String displayName = from.substring(0, from.indexOf("<")).trim();
                String address = from.substring(from.indexOf("<") + 1, from.indexOf(">")).trim();
                fromObject.put("address", address);
                fromObject.put("display_name", displayName);
            } else {
                fromObject.put("address", from);
                fromObject.put("display_name", "The Tinkori Tales");
            }
            payload.put("from", fromObject);
            System.out.println("   from: " + fromObject);

            // TO (required) - array of objects
            List<Map<String, String>> toArray = new ArrayList<>();
            Map<String, String> toObject = new HashMap<>();
            toObject.put("address", to);
            toObject.put("display_name", customerName);
            toArray.add(toObject);
            payload.put("to", toArray);
            System.out.println("   to: " + toArray);

            // CC (optional) - can be object or array
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
                System.out.println("   cc: " + cc);
            }

            // BCC (optional) - can be object or array
            if (bcc != null && !bcc.isBlank()) {
                String[] bccEmails = bcc.split(",");
                List<Map<String, String>> bccArray = new ArrayList<>();
                for (String email : bccEmails) {
                    Map<String, String> bccObject = new HashMap<>();
                    bccObject.put("address", email.trim());
                    bccArray.add(bccObject);
                }
                payload.put("bcc", bccArray);
                System.out.println("   bcc: " + bcc);
            }

            // SUBJECT (required) - string
            payload.put("subject", "Invoice - " + invoiceStatus);
            System.out.println("   subject: Invoice - " + invoiceStatus);

            // HTML (required) - string
            String htmlBody = String.format(
                "<html><body>" +
                "<p>Hi %s,</p>" +
                "<p>Your invoice (%s) dated %s is attached.</p>" +
                "<p>Thank you for using The Tinkori Tales!</p>" +
                "<p>Best regards,<br/>The Tinkori Tales Team</p>" +
                "</body></html>",
                customerName, invoiceStatus, date
            );
            payload.put("html", htmlBody);

            // PLAIN (optional but recommended) - string
            String plainBody = String.format(
                "Hi %s,\n\n" +
                "Your invoice (%s) dated %s is attached.\n\n" +
                "Thank you for using The Tinkori Tales!\n\n" +
                "Best regards,\n" +
                "The Tinkori Tales Team",
                customerName, invoiceStatus, date
            );
            payload.put("plain", plainBody);
            System.out.println("   html & plain: Added");

            // ATTACHMENTS (optional) - array of objects
            List<Map<String, Object>> attachmentsArray = new ArrayList<>();
            Map<String, Object> attachmentObject = new HashMap<>();
            attachmentObject.put("file_name", "invoice_" + invoiceStatus + ".pdf");
            attachmentObject.put("content_type", "application/pdf");
            attachmentObject.put("content", base64Content);
            attachmentsArray.add(attachmentObject);
            payload.put("attachments", attachmentsArray);
            System.out.println("   attachments: invoice_" + invoiceStatus + ".pdf");

            // STEP 4: Convert to JSON and log
            System.out.println("🔵 STEP 4: Converting payload to JSON");
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            System.out.println("📋 JSON PAYLOAD (first 500 chars):");
            System.out.println(jsonPayload.substring(0, Math.min(500, jsonPayload.length())));
            System.out.println("   [... truncated for readability]");

            // STEP 5: Setup headers
            System.out.println("🔵 STEP 5: Setting up headers");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            System.out.println("   Content-Type: application/json");
            System.out.println("   X-API-Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "NOT SET"));

            // STEP 6: Create HTTP request
            System.out.println("🔵 STEP 6: Creating HTTP request");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // STEP 7: Send to Maileroo
            System.out.println("🔵 STEP 7: Sending POST to Maileroo");
            System.out.println("   URL: " + mailerooApiUrl);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                mailerooApiUrl,
                request,
                String.class
            );

            // STEP 8: Process response
            System.out.println("🔵 STEP 8: Response received");
            System.out.println("   Status: " + response.getStatusCode());
            System.out.println("   Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.CREATED ||
                response.getStatusCode() == HttpStatus.ACCEPTED) {
                System.out.println("✅✅✅ EMAIL SENT SUCCESSFULLY ✅✅✅");
                
                // Delete temp PDF
                if (pdfFile.delete()) {
                    System.out.println("🗑️  Temp PDF deleted");
                }
            } else {
                throw new RuntimeException("Unexpected status: " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("\n❌❌❌ MAILEROO API ERROR ❌❌❌");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());
            System.err.println("\n💡 Troubleshooting:");
            System.err.println("   1. Check MAILEROO_API_KEY env variable");
            System.err.println("   2. Verify sender domain in Maileroo dashboard");
            System.err.println("   3. Check email format is valid");
            System.err.println("========================================\n");
            throw new RuntimeException("Maileroo error: " + e.getResponseBodyAsString(), e);
            
        } catch (IOException e) {
            System.err.println("\n❌ FILE ERROR: " + e.getMessage());
            throw new RuntimeException("PDF read failed: " + e.getMessage(), e);
            
        } catch (Exception e) {
            System.err.println("\n❌ UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Email failed: " + e.getMessage(), e);
        }

        System.out.println("========================================\n");
    }
}