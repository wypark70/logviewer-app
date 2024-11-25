package org.atsoft.logviewerapp.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.atsoft.logviewerapp.model.FileInfo;
import org.atsoft.logviewerapp.model.FileResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class FileController {
    @GetMapping("/explore")
    public ResponseEntity<List<FileInfo>> exploreDirectory(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                return ResponseEntity.badRequest().build();
            }

            List<FileInfo> files = new ArrayList<>();
            exploreFiles(directory, files, recursive);

            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void exploreFiles(File directory, List<FileInfo> files, boolean recursive) {
        File[] fileList = directory.listFiles();
        if (fileList == null) return;

        for (File file : fileList) {
            files.add(FileInfo.builder()
                    .name(file.getName())
                    .path(file.getAbsolutePath())
                    .isDirectory(file.isDirectory())
                    .size(file.length())
                    .lastModified(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(file.lastModified()),
                            ZoneId.systemDefault()))
                    .build());

            if (recursive && file.isDirectory()) {
                exploreFiles(file, files, true);
            }
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path.toFile());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(path.getFileName().toString(), "UTF-8") + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(path));
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(path)));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
