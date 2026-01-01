package com.invoice.generation.Service;

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

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        // ✅ calculate subtotal (after item discount, before overall)
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
        context.setVariable("items", invoice.items);
        context.setVariable("amount", amount);
        context.setVariable("name", invoice.customerName);
        context.setVariable("phone", invoice.customerPhone);
        context.setVariable("address", invoice.customerAddress);
        context.setVariable("date", date);
        context.setVariable("invoiceStatus", invoice.invoiceStatus);
        context.setVariable("ownerMessage", invoice.ownerMessage);

        String html = templateEngine.process("invoice", context);
        String path = "invoice.pdf";

        try (FileOutputStream os = new FileOutputStream(path)) {

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, getClass().getResource("/").toExternalForm());

            builder.toStream(os);
            builder.run();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }
}
