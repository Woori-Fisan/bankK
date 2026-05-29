package com.woorifisan.platform.domain.rag.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RagChatResponse {
    /** LLM이 생성한 답변 */
    private String answer;
    
    /** 답변 생성에 참고한 문서 조각(청크) 리스트 */
    private List<String> references;
}
