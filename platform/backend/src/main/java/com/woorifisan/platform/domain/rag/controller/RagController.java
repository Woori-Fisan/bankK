package com.woorifisan.platform.domain.rag.controller;

import com.woorifisan.platform.domain.rag.dto.response.RagChatResponse;
import com.woorifisan.platform.domain.rag.service.RagService;
import com.woorifisan.platform.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadPdf(@RequestPart("file") MultipartFile file) {
        log.info("PDF 업로드 요청: {}", file.getOriginalFilename());

        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("올바른 PDF 파일을 업로드해주세요.");
        }

        String documentId = ragService.processPdf(file);
        return ApiResponse.success("업로드 및 처리 완료. ID: " + documentId);
    }

    @GetMapping("/ask")
    public ApiResponse<RagChatResponse> ask(@RequestParam("question") String question) {
        log.info("RAG 질의 요청: {}", question);
        return ApiResponse.success(ragService.ask(question));
    }

    @GetMapping("/search")
    public ApiResponse<List<String>> search(@RequestParam("query") String query) {
        log.info("RAG 검색 요청: {}", query);
        return ApiResponse.success(ragService.search(query));
    }
}
