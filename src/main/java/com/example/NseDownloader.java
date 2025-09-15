package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class NseDownloader {

    private static final String BASE_URL = "https://www.nseindia.com/";
    private static final String API_URL = "https://www.nseindia.com/api/option-chain-indices?symbol=NIFTY";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NseDownloader() {
        // Create a cookie manager to handle session cookies
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        // Build the OkHttpClient with the cookie jar
        this.client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();
    }

    public NseResponse fetchData() throws IOException {
        // First, make a "warm-up" request to the base URL to get the session cookies
        Request warmupRequest = new Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        try (Response warmupResponse = client.newCall(warmupRequest).execute()) {
            if (!warmupResponse.isSuccessful()) {
                throw new IOException("Warm-up request failed with code " + warmupResponse);
            }
            // We don't need the body, just the cookies which are now stored in the cookie jar.
        }

        // Now, make the actual API request. OkHttp will automatically include the cookies.
        Request apiRequest = new Request.Builder()
                .url(API_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        try (Response response = client.newCall(apiRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed with code " + response);
            }
            if (response.body() == null) {
                throw new IOException("Response body is null");
            }
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, NseResponse.class);
        }
    }
}
