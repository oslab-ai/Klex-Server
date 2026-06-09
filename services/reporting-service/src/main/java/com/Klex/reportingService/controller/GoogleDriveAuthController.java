package com.Klex.reportingService.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drive")
@Tag(name = "Drive Auth", description = "Google Drive OAuth 2.0 authorization for report delivery")
public class GoogleDriveAuthController {

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String TOKENS_DIRECTORY_PATH =
            System.getProperty("drive.tokens.dir", "tokens");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping("/auth-url")
    @Operation(summary = "Get OAuth authorization URL",
               description = "Returns the Google OAuth URL. Accepts optional redirectUri (the callback URL " +
                             "registered in Google Cloud Console) and successRedirect (a frontend URL to " +
                             "redirect the browser to after successful authorization).")
    public ResponseEntity<Map<String, Object>> getAuthUrl(
            @RequestParam(value = "redirectUri", required = false) String redirectUri,
            @RequestParam(value = "successRedirect", required = false) String successRedirect,
            HttpServletRequest request) {
        try {
            if (isServiceAccount()) {
                return badRequest("credentials.json is a service account key — OAuth setup is not needed. " +
                        "Service accounts work headless with Shared Drives.");
            }

            GoogleClientSecrets clientSecrets = loadClientSecrets();
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            if (redirectUri == null || redirectUri.isEmpty()) {
                redirectUri = request.getScheme() + "://" + request.getServerName()
                        + ":" + request.getServerPort() + "/api/drive/callback";
            }

            String authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .build();

            if (successRedirect != null && !successRedirect.isEmpty()) {
                authUrl += "&state=" + java.net.URLEncoder.encode(successRedirect, "UTF-8");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("authUrl", authUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @GetMapping("/callback")
    @Operation(summary = "OAuth callback endpoint",
               description = "Handles the OAuth redirect from Google. Exchanges the authorization code for " +
                             "a token and stores it. Redirects to the URL passed in the state parameter, " +
                             "or shows a success page.")
    public ResponseEntity<String> handleOAuthCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request) {
        try {
            if (isServiceAccount()) {
                return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML)
                        .body(errorHtml("Service account does not need OAuth setup."));
            }

            String callbackUri = request.getRequestURL().toString();

            GoogleClientSecrets clientSecrets = loadClientSecrets();
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(callbackUri)
                    .execute();
            flow.createAndStoreCredential(tokenResponse, "user");

            if (state != null && !state.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                String separator = state.contains("?") ? "&" : "?";
                headers.setLocation(URI.create(state + separator + "drive=connected"));
                return new ResponseEntity<>(headers, org.springframework.http.HttpStatus.FOUND);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(successHtml());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml(e.getMessage()));
        }
    }

    @PostMapping("/auth-code")
    @Operation(summary = "Exchange authorization code for token (API)",
               description = "Accepts the authorization code from the OAuth redirect, exchanges it for " +
                             "access and refresh tokens, and stores them.")
    public ResponseEntity<Map<String, Object>> exchangeAuthCode(@RequestBody Map<String, String> body) {
        try {
            String code = body.get("code");
            if (code == null || code.isEmpty()) {
                return badRequest("Authorization code is required.");
            }

            if (isServiceAccount()) {
                return badRequest("credentials.json is a service account key — OAuth setup is not needed.");
            }

            GoogleClientSecrets clientSecrets = loadClientSecrets();
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri("http://localhost")
                    .execute();
            flow.createAndStoreCredential(tokenResponse, "user");

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Credential stored successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Check Drive auth status",
               description = "Returns the current Google Drive authentication status.")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");

            if (isServiceAccount()) {
                response.put("credentialType", "service_account");
                response.put("authType", "headless");
                response.put("configured", true);
                return ResponseEntity.ok(response);
            }

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = loadClientSecrets();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            Credential credential = flow.loadCredential("user");
            boolean hasToken = credential != null;
            boolean valid = hasToken && (credential.getExpiresInSeconds() == null
                    || credential.getExpiresInSeconds() > 60);

            response.put("credentialType", "oauth");
            response.put("authType", "interactive");
            response.put("configured", hasToken && valid);
            response.put("hasToken", hasToken);
            response.put("expiresInSeconds", hasToken ? credential.getExpiresInSeconds() : null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private boolean isServiceAccount() throws IOException {
        try (InputStream in = GoogleDriveAuthController.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                return false;
            }
            byte[] bytes = toByteArray(in);
            JsonNode root = OBJECT_MAPPER.readTree(bytes);
            return root.has("type") && "service_account".equals(root.get("type").asText());
        }
    }

    private GoogleClientSecrets loadClientSecrets() throws IOException {
        try (InputStream in = GoogleDriveAuthController.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private static String successHtml() {
        return "<!DOCTYPE html>" +
                "<html><head><title>Drive Connected</title>" +
                "<style>" +
                "body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;" +
                "height:100vh;margin:0;background:#f5f5f5}" +
                ".card{background:white;padding:40px;border-radius:12px;" +
                "box-shadow:0 2px 12px rgba(0,0,0,0.1);text-align:center}" +
                ".check{width:48px;height:48px;background:#10b981;color:white;" +
                "border-radius:50%;display:flex;align-items:center;justify-content:center;" +
                "margin:0 auto 16px;font-size:24px}" +
                "h2{color:#1f2937;margin:0 0 8px}p{color:#6b7280;margin:0}" +
                "</style></head>" +
                "<body><div class=\"card\"><div class=\"check\">&#10003;</div>" +
                "<h2>Google Drive Connected</h2>" +
                "<p>Authorization successful. You can close this tab.</p></div></body></html>";
    }

    private static String errorHtml(String message) {
        return "<!DOCTYPE html>" +
                "<html><head><title>Authorization Failed</title>" +
                "<style>" +
                "body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;" +
                "height:100vh;margin:0;background:#f5f5f5}" +
                ".card{background:white;padding:40px;border-radius:12px;" +
                "box-shadow:0 2px 12px rgba(0,0,0,0.1);text-align:center}" +
                ".x{width:48px;height:48px;background:#ef4444;color:white;" +
                "border-radius:50%;display:flex;align-items:center;justify-content:center;" +
                "margin:0 auto 16px;font-size:24px}" +
                "h2{color:#1f2937;margin:0 0 8px}p{color:#6b7280;margin:0}" +
                "</style></head>" +
                "<body><div class=\"card\"><div class=\"x\">&#10007;</div>" +
                "<h2>Authorization Failed</h2>" +
                "<p>" + message + "</p></div></body></html>";
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    private static ResponseEntity<Map<String, Object>> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", message);
        return ResponseEntity.internalServerError().body(response);
    }
}
