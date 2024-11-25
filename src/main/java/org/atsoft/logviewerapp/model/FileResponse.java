package org.atsoft.logviewerapp.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FileResponse {
    private String filePath;
    private int totalLinesRead;
    private List<String> content;
}