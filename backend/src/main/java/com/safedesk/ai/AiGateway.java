package com.safedesk.ai;

import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;

public interface AiGateway {
    AiResult reply(AiReplyRequest request);   // "질문을 주면 결과를 돌려준다"는 약속
}
