package com.Jjambbong.PayLens;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"LOCAL_DB_URL=jdbc:h2:mem:paylens-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"LOCAL_DB_USERNAME=sa",
		"LOCAL_DB_PASSWORD=",
		"KAKAO_REST_API_KEY=test-kakao-client",
		"KAKAO_CLIENT_SECRET=test-kakao-secret",
		"JWT_SECRET=test-jwt-secret-key-for-ci-only-32-bytes-minimum",
		"AWS_ACCESS_KEY=test-access-key",
		"AWS_SECRET_KEY=test-secret-key",
		"AWS_S3_BUCKET=test-bucket",
		"AWS_S3_PATH=documents"
})
class PayLensApplicationTests {

	@Test
	void contextLoads() {
	}

}
