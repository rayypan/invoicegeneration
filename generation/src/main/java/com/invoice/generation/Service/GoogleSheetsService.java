package com.invoice.generation.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Service
public class GoogleSheetsService {

    @org.springframework.beans.factory.annotation.Value("${google.spreadsheet.id}")
    private String SHEET_ID;

    private static final String BASE_URL
            = "https://sheets.googleapis.com/v4/spreadsheets/";

    public void writeRow(String sheetName, String range, List<Object> row) {
        try {
            String base64 = System.getenv("GOOGLE_CREDENTIALS_BASE64");

            if (base64 == null || base64.isEmpty()) {
                throw new RuntimeException("GOOGLE_CREDENTIALS_BASE64 not set");
            }

            byte[] decoded = java.util.Base64.getDecoder().decode(base64);
            InputStream in = new java.io.ByteArrayInputStream(decoded);

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(in)
                    .createScoped(List.of(
                            "https://www.googleapis.com/auth/spreadsheets"
                    ));

            HttpRequestFactory factory
                    = new NetHttpTransport()
                            .createRequestFactory(
                                    new HttpCredentialsAdapter(credentials));

            GenericUrl url = new GenericUrl(
                    BASE_URL + SHEET_ID
                    + "/values/" + sheetName + "!" + range
                    + ":append?valueInputOption=USER_ENTERED"
            );

            Map<String, Object> body = Map.of(
                    "values", List.of(row)
            );

            HttpRequest request
                    = factory.buildPostRequest(
                            url,
                            new JsonHttpContent(
                                    JacksonFactory.getDefaultInstance(),
                                    body
                            )
                    );

            request.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
