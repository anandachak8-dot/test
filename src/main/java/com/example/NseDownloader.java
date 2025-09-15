package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class NseDownloader {

    private static final String API_URL = "https://www.nseindia.com/api/option-chain-indices?symbol=NIFTY";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NseResponse fetchData() throws IOException {
        Request request = new Request.Builder()
                .url(API_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() == null) {
                throw new IOException("Response body is null");
            }
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, NseResponse.class);
        }
    }
}
