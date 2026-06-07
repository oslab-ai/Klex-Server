package com.Klex.reportingService.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class GoogleDriveAuthSetup {

    private static final String APPLICATION_NAME = "JrlDemo";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH =
            System.getProperty("drive.tokens.dir", "tokens");
    private static final String REDIRECT_URI = "http://localhost";

    public static void main(String[] args) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleClientSecrets clientSecrets;
        try (InputStream in = GoogleDriveAuthSetup.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                System.err.println("credentials.json not found on classpath at " + CREDENTIALS_FILE_PATH);
                System.err.println("Place your installed-app OAuth credentials at src/main/resources/credentials.json");
                System.exit(1);
                return;
            }
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        Credential existingCredential = flow.loadCredential("user");
        if (existingCredential != null) {
            System.out.println("Valid stored credential found in " + TOKENS_DIRECTORY_PATH + "/");
            if (existingCredential.getExpiresInSeconds() != null && existingCredential.getExpiresInSeconds() < 60) {
                existingCredential.refreshToken();
                System.out.println("Token refreshed.");
            }
            testDriveAccess(HTTP_TRANSPORT, existingCredential);
            return;
        }

        String authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(REDIRECT_URI)
                .build();

        System.out.println("=".repeat(70));
        System.out.println("1. Open this URL in your browser:");
        System.out.println("   " + authorizationUrl);
        System.out.println("2. Sign in to your Google account and grant access.");
        System.out.println("3. After authorizing, your browser will try to open");
        System.out.println("   http://localhost/?code=...  (it will fail to load — that's OK).");
        System.out.println("4. COPY the authorization code (the 'code' parameter value from the URL)");
        System.out.println("   and re-run with:");
        System.out.println("   -Ddrive.auth.code=YOUR_CODE");
        System.out.println("=".repeat(70));

        String code = System.getProperty("drive.auth.code");
        if (code == null || code.isEmpty()) {
            System.out.println("No -Ddrive.auth.code provided. Run again with the code from step 4.");
            System.exit(0);
            return;
        }

        TokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(REDIRECT_URI)
                .execute();
        Credential credential = flow.createAndStoreCredential(tokenResponse, "user");

        System.out.println("Credential stored successfully in " + TOKENS_DIRECTORY_PATH + "/");
        testDriveAccess(HTTP_TRANSPORT, credential);
    }

    private static String extractCode(String url) {
        int codeIdx = url.indexOf("code=");
        if (codeIdx == -1) return null;
        int start = codeIdx + 5;
        int end = url.indexOf('&', start);
        return (end == -1) ? url.substring(start) : url.substring(start, end);
    }

    private static void testDriveAccess(NetHttpTransport httpTransport, Credential credential) throws Exception {
        Drive driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        FileList result = driveService.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        System.out.println("Drive access confirmed.");
        if (files != null && !files.isEmpty()) {
            System.out.println("Recent files:");
            for (File file : files) {
                System.out.println("  - " + file.getName() + " (" + file.getId() + ")");
            }
        }
    }
}
