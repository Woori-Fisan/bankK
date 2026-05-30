package com.woorifisan.platform.domain.rag.controller;

import com.woorifisan.platform.domain.rag.dto.response.RagChatResponse;
import com.woorifisan.platform.domain.rag.service.RagService;
import com.woorifisan.platform.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "RAG", description = "문서 기반 질의응답(RAG) API")
public class RagController {

    private final RagService ragService;

    @Operation(summary = "PDF 문서 업로드 및 인덱싱", description = "PDF 파일을 업로드하여 텍스트를 추출하고 Qdrant 벡터 스토어에 저장합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadPdf(
            @Parameter(description = "업로드할 PDF 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart("file") MultipartFile file) {
        log.info("PDF 업로드 요청: {}", file.getOriginalFilename());

        // 파일 검증 강화: 확장자 및 MIME 타입 체크
        boolean isValidExtension = file.getOriginalFilename() != null && 
                                   file.getOriginalFilename().toLowerCase().endsWith(".pdf");
        boolean isValidMimeType = "application/pdf".equals(file.getContentType());

        if (file.isEmpty() || !isValidExtension || !isValidMimeType) {
            log.warn("유효하지 않은 파일 업로드 시도: name={}, type={}", file.getOriginalFilename(), file.getContentType());
            throw new IllegalArgumentException("올바른 PDF 파일(application/pdf)을 업로드해주세요.");
        }

        String documentId = ragService.processPdf(file);
        return ApiResponse.success("업로드 및 처리 완료. ID: " + documentId);
    }

    @Operation(summary = "RAG 질의 (Gemini)", description = "질문을 입력하면 등록된 문서에서 관련 내용을 찾아 Gemini AI가 답변을 생성합니다.")
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
