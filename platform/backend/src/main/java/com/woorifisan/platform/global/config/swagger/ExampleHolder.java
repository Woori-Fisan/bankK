package com.woorifisan.platform.global.config;

import io.swagger.v3.oas.models.examples.Example;
import lombok.Builder;
import lombok.Getter;

/**
 * Swagger Example을 담기 위한 홀더 클래스
 */
@Getter
@Builder
public class ExampleHolder {
    private Example example;
    private String name;
    private int code;
}
