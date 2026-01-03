package com.invoice.generation.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.invoice.generation.DTOs.InvoiceDTO;
import com.invoice.generation.DTOs.ItemDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class PdfService {

    private final TemplateEngine templateEngine;

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String generatePdf(InvoiceDTO invoice, double amount) {

        System.out.println("========================================");
        System.out.println("🔵 STEP 1: Starting PDF Generation");
        System.out.println("========================================");

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        // Calculate subtotal
        double subtotal = 0;
        for (ItemDTO item : invoice.items) {
            double itemTotal = item.price * item.quantity;

            if ("PERCENT".equals(item.discountType)) {
                itemTotal -= (itemTotal * item.discount / 100);
            } else {
                itemTotal -= item.discount;
            }
            subtotal += itemTotal;
        }

        System.out.println("🔵 STEP 2: Creating Thymeleaf context");
        Context context = new Context();
        context.setVariable("subtotal", subtotal);
        context.setVariable("applyOverallDiscount", invoice.applyOverallDiscount);
        context.setVariable("overallDiscount", invoice.overallDiscount);
        context.setVariable("overallDiscountType", invoice.overallDiscountType);
        context.setVariable("adjustmentAmount", invoice.adjustmentAmount);
        context.setVariable("adjustmentAmountType", invoice.adjustmentAmountType);
        context.setVariable("paymentMethod", invoice.paymentMethod);
        context.setVariable("paymentDetails", invoice.paymentDetails);
        context.setVariable("issuedBy", invoice.issuedBy);
        context.setVariable("items", invoice.items);
        context.setVariable("amount", amount);
        context.setVariable("name", invoice.customerName);
        context.setVariable("phone", invoice.customerPhone);
        context.setVariable("address", invoice.customerAddress);
        context.setVariable("date", date);
        context.setVariable("invoiceStatus", invoice.invoiceStatus);
        context.setVariable("ownerMessage", invoice.ownerMessage);

        System.out.println("🔵 STEP 3: Processing Thymeleaf template");
        String html = templateEngine.process("invoice", context);
        System.out.println("✅ HTML generated: " + html.length() + " characters");

        // Use system temp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "invoice_" + System.currentTimeMillis() + ".pdf";
        String path = tempDir + File.separator + fileName;

        System.out.println("🔵 STEP 4: PDF path: " + path);

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(path);
            System.out.println("✅ FileOutputStream created");

            PdfRendererBuilder builder = new PdfRendererBuilder();
            String baseUrl = getClass().getResource("/").toExternalForm();
            System.out.println("🔵 STEP 5: Base URL for resources: " + baseUrl);
            
            builder.withHtmlContent(html, baseUrl);
            builder.toStream(os);
            
            System.out.println("🔵 STEP 6: Running PDF builder...");
            builder.run();
            System.out.println("✅ PDF builder completed");

            // ⚠️ CRITICAL: Explicitly flush and close the stream
            System.out.println("🔵 STEP 7: Flushing stream...");
            os.flush();
            System.out.println("✅ Stream flushed");
            
            os.close();
            os = null; // Prevent double-close in finally block
            System.out.println("✅ Stream closed");

        } catch (Exception e) {
            System.err.println("❌❌❌ PDF generation FAILED at runtime ❌❌❌");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                    System.out.println("🔵 Stream closed in finally block");
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to close stream in finally: " + e.getMessage());
                }
            }
        }

        // ✅ VALIDATE: Check file after stream is closed
        System.out.println("🔵 STEP 8: Validating PDF file...");
        File pdfFile = new File(path);
        
        if (!pdfFile.exists()) {
            System.err.println("❌ PDF file does NOT exist at: " + path);
            throw new RuntimeException("PDF file was not created");
        }
        System.out.println("✅ PDF file exists");

        long fileSize = pdfFile.length();
        System.out.println("🔵 STEP 9: PDF file size: " + fileSize + " bytes");
        
        if (fileSize == 0) {
            System.err.println("❌ PDF file is EMPTY (0 bytes)");
            throw new RuntimeException("PDF file is empty");
        }
        
        System.out.println("✅✅✅ PDF GENERATION COMPLETE ✅✅✅");
        System.out.println("📄 Path: " + path);
        System.out.println("📊 Size: " + fileSize + " bytes");
        System.out.println("========================================");

        return path;
    }
}