package org.example.ssj3pj;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
class Ssj3PjApplicationTests {

	@MockBean
	private S3Client s3Client;

	@MockBean
	private S3Presigner s3Presigner;

	@Test
	void contextLoads() {
	}

}
