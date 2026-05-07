package com.Klex.reportingService.service;

import java.io.IOException;
import java.io.InputStream;

public interface InputSourceService {
    InputStream getInputStream(String path) throws IOException;

    InputSourceType getSourceType();
}
