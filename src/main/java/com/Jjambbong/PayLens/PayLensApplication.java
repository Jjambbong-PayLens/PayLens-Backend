package com.Jjambbong.PayLens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PayLensApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayLensApplication.class, args);
	}

}
