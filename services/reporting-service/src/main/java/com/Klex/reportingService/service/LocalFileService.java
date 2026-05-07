package com.Klex.reportingService.service;

import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class LocalFileService implements InputSourceService {

    @Override
    public InputStream getInputStream(String path) throws IOException {
        if (Files.exists(Paths.get(path))) {
            return new FileInputStream(path);
        } else {
            // Fallback to checking classpath if not found on filesystem,
            // though the requirement implies local filesystem path.
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                throw new IOException("File not found at path: " + path);
            }
            return is;
        }
    }

    @Override
    public InputSourceType getSourceType() {
        return InputSourceType.LOCAL;
    }
}
