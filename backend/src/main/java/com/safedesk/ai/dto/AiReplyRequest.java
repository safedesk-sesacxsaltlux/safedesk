package com.safedesk.ai.dto;

/**
 * 보내는 질문.
 *
 * @param faqAnswer 질문에 매칭된 FAQ 답변 (없으면 null). 목업 AI 가 NORMAL 모드에서 이 답변을 돌려준다.
 *                  실제 AI 로 바꾸면 답변 근거(RAG 문서)로 넘기는 자리다.
 */
public record AiReplyRequest(Long chatId,
                             String content,
                             String faqAnswer) {

    public AiReplyRequest(Long chatId, String content) {
        this(chatId, content, null);
    }
}
