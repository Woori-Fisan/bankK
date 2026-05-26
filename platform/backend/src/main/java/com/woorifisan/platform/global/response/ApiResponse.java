package com.woorifisan.platform.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 JSON 제외
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    // 정상 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 데이터 없는 정상 응답
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    // 에러 응답
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, ErrorResponse.of(errorCode));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(false, null, ErrorResponse.of(errorCode, customMessage));
    }
}