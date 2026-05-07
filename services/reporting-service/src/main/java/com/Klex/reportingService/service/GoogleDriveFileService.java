package com.Klex.reportingService.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveFileService implements InputSourceService {

    private static final String APPLICATION_NAME = "JrlDemo";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json"; // Expecting in resources

    private Drive driveService;

    @Override
    public InputStream getInputStream(String path) throws IOException {
        // Path is treated as File ID for Google Drive
        if (driveService == null) {
            initDriveService();
        }
        try {
            return driveService.files().get(path).executeMediaAsInputStream();
        } catch (Exception e) {
            throw new IOException("Failed to download file from Google Drive: " + e.getMessage(), e);
        }
    }

    private void initDriveService() throws IOException {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            InputStream in = GoogleDriveFileService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(SCOPES);

            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new IOException("Failed to initialize Google Drive service: " + e.getMessage(), e);
        }
    }

    @Override
    public InputSourceType getSourceType() {
        return InputSourceType.GOOGLE_DRIVE;
    }
}
