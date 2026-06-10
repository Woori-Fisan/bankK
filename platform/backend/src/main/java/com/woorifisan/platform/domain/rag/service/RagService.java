package com.woorifisan.platform.domain.rag.service;

import com.woorifisan.platform.domain.rag.dto.response.RagChatResponse;
import com.woorifisan.platform.domain.rag.repository.QdrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final QdrantRepository qdrantRepository;
    private final ChatModel chatModel;

    private static final String RAG_PROMPT_TEMPLATE = """
            당신은 은행 대리업 중계 플랫폼의 전문 상담 보조 AI입니다.
            제공된 [컨텍스트]를 바탕으로 사용자의 [질문]에 대해 정확하고 친절하게 답변해 주세요.
            
            반드시 제공된 컨텍스트 내의 정보만을 바탕으로 답변해야 하며, 만약 컨텍스트에서 정보를 찾을 수 없다면 "제공된 문서에서 해당 정보를 찾을 수 없습니다."라고 답변해 주세요.
            금융 관련 용어는 정확하게 사용하며, 상담원에게 도움이 될 수 있도록 전문적인 톤을 유지해 주세요.
            
            [컨텍스트]
            {context}
            
            [질문]
            {question}
            
            [답변]
            """;

    private static final String KNOWLEDGE_BASE_DOCUMENT_ID = "knowledge-base";
    private static final String KNOWLEDGE_BASE_PATH = "rag/knowledge-base.md";

    @EventListener(ApplicationReadyEvent.class)
    public void loadKnowledgeBase() {
        ClassPathResource resource = new ClassPathResource(KNOWLEDGE_BASE_PATH);
        if (!resource.exists()) {
            log.warn("지식 베이스 파일을 찾을 수 없습니다: {}", KNOWLEDGE_BASE_PATH);
            return;
        }
        try {
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("지식 베이스 로드 시작. 파일: {}, 크기: {} bytes", KNOWLEDGE_BASE_PATH, text.length());

            qdrantRepository.deleteByDocumentId(KNOWLEDGE_BASE_DOCUMENT_ID);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", "knowledge-base.md");
            metadata.put("source", "classpath");

            qdrantRepository.saveDocuments(KNOWLEDGE_BASE_DOCUMENT_ID, text, metadata);
            log.info("지식 베이스 인덱싱 완료.");
        } catch (Exception e) {
            log.error("지식 베이스 로드 중 오류 발생. RAG 기능이 비활성화될 수 있습니다.", e);
        }
    }

    public String processPdf(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 존재하지 않습니다.");
        }
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

    public RagChatResponse ask(String question) {
        log.info("RAG 질의 시작: {}", question);

        // 1. 유사도 검색 (Context 확보)
        List<String> contextChunks = qdrantRepository.searchSimilar(question);
        String context = contextChunks.stream().collect(Collectors.joining("\n\n"));

        // 2. 프롬프트 구성
        PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        Map<String, Object> model = new HashMap<>();
        model.put("context", context);
        model.put("question", question);

        Prompt prompt = promptTemplate.create(model);

        // 3. LLM 호출
        log.info("Gemini LLM 호출 중...");
        var chatResponse = chatModel.call(prompt);
        
        String answer = "답변을 생성할 수 없습니다. (응답이 비어있거나 안전 정책에 의해 차단되었을 수 있습니다.)";
        if (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            answer = chatResponse.getResult().getOutput().getText();
        }

        return RagChatResponse.builder()
                .answer(answer)
                .references(contextChunks)
                .build();
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
