package com.Jjambbong.PayLens;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"LOCAL_DB_URL=jdbc:h2:mem:paylens-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"LOCAL_DB_USERNAME=sa",
		"LOCAL_DB_PASSWORD=",
		"KAKAO_REST_API_KEY=test-kakao-client",
		"KAKAO_CLIENT_SECRET=test-kakao-secret",
		"GOOGLE_CLIENT_ID=test-google-client",
		"GOOGLE_CLIENT_SECRET=test-google-secret",
		"JWT_SECRET=test-jwt-secret-key-for-ci-only-32-bytes-minimum",
		"AWS_ACCESS_KEY=test-access-key",
		"AWS_SECRET_KEY=test-secret-key",
		"AWS_S3_BUCKET=test-bucket",
		"AWS_S3_PATH=documents",
		"OCR_INTERNAL_TOKEN=test-ocr-internal-token",
		"GEMINI_API_KEY=test-gemini-api-key",
		"PORTONE_STORE_ID=test-store-id",
		"PORTONE_API_KEY=test-portone-api-key",
		"PORTONE_API_SECRET=test-portone-api-secret"
})
class PayLensApplicationTests {

	@Test
	void contextLoads() {
	}

}
