package com.invoice.generation;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvoice(String to, String pdfPath, String customerName,String invoiceStatus, String date) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String subject
                    = "Thank You | " +customerName+" | "+invoiceStatus +" | " + date;

            helper.setTo(to);
            helper.setCc("thetinkoritales@gmail.com");
            helper.setBcc(new String[]{
                "sandipanray1123@gmail.com",
                "nilanjanadebnath04@gmail.com",
                "diptimoy2003@gmail.com",
                
            });

            helper.setSubject(subject);
            helper.setText(
                    "Hi " + customerName + ",\n\n"
                    + "Thank you for choosing The Tinkori Tales.\n"+
                    "Invoice Status: "+invoiceStatus+"\n"
                    + "Your invoice is attached with this email.\n\n"
                    + "Warm regards,\n"
                    + "The Tinkori Tales \n\n"
                    + "Finance & Acccounts\n"
                    +"Diptimoy Hazra"
            );

            helper.addAttachment("invoice.pdf", new File(pdfPath));
            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
