package org.atsoft.logviewerapp.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileInfo {
    private String name;
    private String path;
    private boolean isDirectory;
    private long size;
    private LocalDateTime lastModified;
}