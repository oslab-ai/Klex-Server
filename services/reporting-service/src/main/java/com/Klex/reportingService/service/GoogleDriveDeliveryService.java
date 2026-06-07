package com.Klex.reportingService.service;

import com.google.api.client.auth.oauth2.Credential;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GoogleDriveDeliveryService {

    private static final String APPLICATION_NAME = "JrlDemo";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH =
            System.getProperty("drive.tokens.dir", "tokens");

    Drive driveService;

    public void uploadReport(byte[] reportBytes, String fileName, String folderId) throws IOException {
        if (driveService == null) {
            initDriveService();
        }

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));

        com.google.api.client.http.InputStreamContent mediaContent =
                new com.google.api.client.http.InputStreamContent(getMimeType(fileName), new ByteArrayInputStream(reportBytes));

        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, parents")
                .execute();

        log.info("Uploaded report {} to Google Drive folder {} with file ID: {}", fileName, folderId, uploadedFile.getId());
    }

    private String getMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "pdf":
                return "application/pdf";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv":
                return "text/csv";
            case "html":
                return "text/html";
            case "rtf":
                return "application/rtf";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "odt":
                return "application/vnd.oasis.opendocument.text";
            case "ods":
                return "application/vnd.oasis.opendocument.spreadsheet";
            default:
                return "application/octet-stream";
        }
    }

    private void initDriveService() throws IOException {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = loadCredential(HTTP_TRANSPORT);
            if (credential == null) {
                throw new IOException(
                    "No stored credential found. Run GoogleDriveAuthSetup first to authorize " +
                    "your Google Drive access, or set -Ddrive.tokens.dir to point to the tokens directory.");
            }
            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to initialize Google Drive service: " + e.getMessage(), e);
        }
    }

    private Credential loadCredential(NetHttpTransport httpTransport) throws IOException {
        try (InputStream in = GoogleDriveDeliveryService.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            Credential credential = flow.loadCredential("user");
            if (credential != null) {
                if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
                    credential.refreshToken();
                }
                return credential;
            }
            return null;
        }
    }
}
