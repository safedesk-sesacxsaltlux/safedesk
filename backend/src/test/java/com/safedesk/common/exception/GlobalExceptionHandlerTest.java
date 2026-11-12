package com.safedesk.common.exception;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safedesk.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {

        @GetMapping("/test/ok")
        ApiResponse<String> ok() {
            return ApiResponse.success("hello", "조회되었습니다.");
        }

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(ErrorCode.CHAT_NOT_FOUND);
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("DB 비밀 정보");
        }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody TestRequest request) {
        }
    }

    record TestRequest(@NotBlank String content) {
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void 성공_응답에는_errorCode가_없다() throws Exception {
        mockMvc.perform(get("/test/ok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value("hello"))
            .andExpect(jsonPath("$.message").value("조회되었습니다."))
            .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    void BusinessException은_ErrorCode의_상태와_메시지로_응답한다() throws Exception {
        mockMvc.perform(get("/test/business"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.message").value("상담을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.errorCode").value("CHAT_NOT_FOUND"));
    }

    @Test
    void 검증_실패는_400_INVALID_INPUT() throws Exception {
        mockMvc.perform(post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void 예상못한_예외는_500이고_내부_메시지를_숨긴다() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message", not(containsString("DB 비밀 정보"))));
    }
}
