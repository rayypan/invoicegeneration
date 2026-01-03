package com.invoice.generation.Service;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailApiClient {
     @Value("${email.api.url}")
    private String emailApiUrl;

    @Value("${email.smtp.host}")
    private String smtpHost;

    @Value("${email.smtp.port}")
    private String smtpPort;

    @Value("${email.smtp.user}")
    private String smtpUser;

    @Value("${email.smtp.password}")
    private String smtpPassword;

    @Value("${email.from}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public String sendEmail(
            String filePath,
            String subject,
            String html
    ) {

        WebClient client = WebClient.create();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("from", smtpUser); // John Doe <johndoe@example.com>
        body.add("subject", subject);
        body.add("html", html);

        body.add("emailHost", smtpHost);
        body.add("emailPort", smtpPort);
        body.add("emailUser", smtpUser);
        body.add("emailPassword", smtpPassword);

        body.add("file", new FileSystemResource(Path.of(filePath)));

        return client.post()
                .uri("https://generic-email-service.vercel.app/api/v1/email")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
