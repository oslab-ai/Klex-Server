package com.Klex.reportingService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                .setSupportsAllDrives(true)
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
            byte[] credBytes = readCredentialsBytes();
            JsonNode credJson = OBJECT_MAPPER.readTree(credBytes);

            if (credJson.has("type") && "service_account".equals(credJson.get("type").asText())) {
                driveService = buildServiceAccountDrive(HTTP_TRANSPORT, credBytes);
            } else if (credJson.has("installed") || credJson.has("web")) {
                driveService = buildOAuthDrive(HTTP_TRANSPORT, credBytes);
            } else {
                throw new IOException("Unrecognized credential type in " + CREDENTIALS_FILE_PATH
                        + ". Expected 'type: service_account' or 'installed'/'web' key.");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to initialize Google Drive service: " + e.getMessage(), e);
        }
    }

    private Drive buildServiceAccountDrive(NetHttpTransport httpTransport, byte[] credBytes) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(credBytes))
                .createScoped(SCOPES);
        log.info("Initialized Google Drive with service account auth");
        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Drive buildOAuthDrive(NetHttpTransport httpTransport, byte[] credBytes) throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(new ByteArrayInputStream(credBytes)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        Credential credential = flow.loadCredential("user");
        if (credential == null) {
            throw new IOException(
                    "No stored OAuth credential found. Use the Drive auth API to set up authorization:\n" +
                    "  1) GET /api/drive/auth-url  →  open in browser  →  authorize\n" +
                    "  2) POST /api/drive/auth-code with the authorization code");
        }
        if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
            credential.refreshToken();
        }
        log.info("Initialized Google Drive with OAuth 2.0 auth");
        return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private byte[] readCredentialsBytes() throws IOException {
        try (InputStream in = GoogleDriveDeliveryService.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            return toByteArray(in);
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
}
