package com.Jjambbong.PayLens.global;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "health", description = "health check 관련 API")
@RestController
public class RootController {
    @Operation(summary = "CI/CD 확인", description = "CI/CD가 정상적으로 작동하고 있는지 확인하는 메서드입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON_200", description = "Success"),
    })
    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }
}