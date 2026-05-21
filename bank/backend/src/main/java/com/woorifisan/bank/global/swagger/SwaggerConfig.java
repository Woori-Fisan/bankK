package com.woorifisan.bank.global.swagger;

import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.response.ErrorResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8081");
        localServer.setDescription("Local Development Server");

        Info info = new Info()
                .title("BankBridge Bank API")
                .version("v1.0.0")
                .description("BankBridge 은행 API 명세서");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }

    /**
     * CustomExceptionDescription 어노테이션을 기반으로 에러 응답 예시를 Swagger에 추가
     */
    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            CustomExceptionDescription annotation = handlerMethod.getMethodAnnotation(CustomExceptionDescription.class);
            if (annotation != null) {
                generateResponseByErrorCode(operation, annotation.value());
            }
            return operation;
        };
    }

    private void generateResponseByErrorCode(Operation operation, SwaggerResponseDescription description) {
        ApiResponses responses = operation.getResponses();

        // 에러 코드들을 HTTP 상태 코드별로 그룹화
        Map<Integer, List<ExampleHolder>> examplesByStatus = description.getErrorCodes().stream()
                .map(this::getExampleHolder)
                .collect(Collectors.groupingBy(ExampleHolder::getCode));

        // 상태 코드별로 ApiResponse 생성 및 추가
        examplesByStatus.forEach((status, holders) -> {
            Content content = new Content();
            MediaType mediaType = new MediaType();
            holders.forEach(holder -> mediaType.addExamples(holder.getName(), holder.getExample()));
            content.addMediaType("application/json", mediaType);

            ApiResponse apiResponse = new ApiResponse().content(content);
            responses.addApiResponse(String.valueOf(status), apiResponse);
        });
    }

    private ExampleHolder getExampleHolder(ErrorCode errorCode) {
        Example example = new Example();
        example.setValue(ErrorResponse.of(errorCode));
        return ExampleHolder.builder()
                .example(example)
                .name(errorCode.name())
                .code(errorCode.getHttpStatus().value())
                .build();
    }
}
