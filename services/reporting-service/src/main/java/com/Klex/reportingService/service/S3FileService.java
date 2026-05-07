package com.Klex.reportingService.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;

@Service
public class S3FileService implements InputSourceService {

    private S3Client s3Client;

    public S3FileService() {
        // Initialization moved to lazy load
    }

    private synchronized S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.create();
        }
        return s3Client;
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        // Expected path format: "bucketName/key"
        String[] parts = path.split("/", 2);
        if (parts.length < 2) {
            throw new IOException("Invalid S3 path. Expected format: bucketName/key");
        }
        String bucketName = parts[0];
        String key = parts[1];

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = getS3Client().getObject(getObjectRequest);
            return s3Object;
        } catch (Exception e) {
            throw new IOException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public InputSourceType getSourceType() {
        return InputSourceType.S3;
    }
}
