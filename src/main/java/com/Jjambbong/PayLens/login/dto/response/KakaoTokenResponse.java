package com.Jjambbong.PayLens.login.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

}
