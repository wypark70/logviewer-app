package org.atsoft.logviewerapp.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.atsoft.logviewerapp.model.FileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogViewerController {
    @GetMapping("/read-last-lines")
    public ResponseEntity<FileResponse> readLastLines(@RequestParam String filePath, @RequestParam(defaultValue = "100") int lines) {
        try {
            List<String> lastLines = readLastLinesFromFile(filePath, lines);
            FileResponse response = FileResponse.builder().filePath(filePath).totalLinesRead(lastLines.size()).content(lastLines).build();
            return ResponseEntity.ok(response);
        } catch (FileNotFoundException e) {
            log.error("파일을 찾을 수 없습니다: {}", filePath, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
        } catch (Exception e) {
            log.error("파일 읽기 오류: {}", filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 읽기 중 오류가 발생했습니다");
        }
    }

    private List<String> readLastLinesFromFile(String filePath, int lines) throws IOException {
        File file = new File(filePath);
        LinkedList<String> lastLines = new LinkedList<>();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && lastLines.size() < lines) {
                lastLines.add(line);
            }
        }

        Collections.reverse(lastLines);
        return lastLines;
    }

    @GetMapping("/hello")
    public ResponseEntity hello(@RequestParam("action") String action) {
        try {
            if (action.equals("hello")) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "hello!!!");
                return ResponseEntity.ok(response);
            } else {
                throw new RuntimeException("RuntimeException!!!");
            }

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestParam String filePath) {
        SseEmitter emitter = new SseEmitter(-1L); // no timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    emitter.completeWithError(new FileNotFoundException("File not found: " + filePath));
                    return;
                }

                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    // 기존 로그 내용 전송
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitter.send(line);
                    }

                    // 파일 끝에서 새로운 로그 모니터링
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r")) {
                        long filePointer = randomAccessFile.length();

                        while (!Thread.currentThread().isInterrupted()) {
                            long length = randomAccessFile.length();
                            if (length < filePointer) {
                                // 파일이 새로 생성된 경우
                                filePointer = length;
                            } else if (length > filePointer) {
                                // 새로운 로그가 추가된 경우
                                randomAccessFile.seek(filePointer);
                                String newLine = randomAccessFile.readLine();
                                if (newLine != null) {
                                    emitter.send(newLine);
                                }
                                filePointer = randomAccessFile.getFilePointer();
                            }
                            Thread.sleep(1000); // 1초마다 체크
                        }
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                emitter.complete();
                executor.shutdown();
            }
        });

        return emitter;
    }
}
