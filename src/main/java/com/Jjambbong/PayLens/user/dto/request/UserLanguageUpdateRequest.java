package com.Jjambbong.PayLens.user.dto.request;

import com.Jjambbong.PayLens.user.domain.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLanguageUpdateRequest {

    @NotNull(message = "언어 코드는 비워둘 수 없습니다.")
    @Schema(description = "선호 언어 (예: KO, EN, VI)", example = "EN")
    private Language language;
}
