package com.woorifisan.platform.domain.rag.service;

import com.woorifisan.platform.domain.rag.repository.QdrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final QdrantRepository qdrantRepository;

    public String processPdf(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String documentId = UUID.randomUUID().toString();

        log.info("PDF 문서 처리 시작. 파일: {}, ID: {}", originalFilename, documentId);

        try {
            // 1. PDF 텍스트 추출
            String text = extractTextFromPdf(file);
            log.info("텍스트 추출 완료. 길이: {}", text.length());

            // 2. 메타데이터 구성
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", originalFilename);
            metadata.put("source", "upload");

            // 3. Qdrant 저장 (청킹 및 임베딩 포함)
            qdrantRepository.saveDocuments(documentId, text, metadata);

            return documentId;
        } catch (IOException e) {
            log.error("PDF 처리 중 오류 발생", e);
            throw new RuntimeException("PDF 텍스트 추출 실패", e);
        }
    }

    public List<String> search(String query) {
        return qdrantRepository.searchSimilar(query);
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
