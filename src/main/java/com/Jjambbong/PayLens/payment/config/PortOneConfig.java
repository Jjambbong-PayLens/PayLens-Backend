package com.Jjambbong.PayLens.payment.config;

import com.siot.IamportRestClient.IamportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortOneConfig {

    // application.yml에 적어둔 포트원 Key와 Secret을 가져온다
    @Value("${portone.api.key}")
    private String apiKey;

    @Value("${portone.api.secret}")
    private String apiSecret;

    // IamportClient를 스프링 빈으로 등록해서 어디서든 쓸 수 있게 한다
    @Bean
    public IamportClient iamportClient() {
        return new IamportClient(apiKey, apiSecret);
    }
}