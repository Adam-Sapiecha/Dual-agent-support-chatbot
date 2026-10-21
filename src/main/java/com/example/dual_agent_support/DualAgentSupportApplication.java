package com.example.dual_agent_support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example")
@org.springframework.scheduling.annotation.EnableScheduling
public class DualAgentSupportApplication {

	public static void main(String[] args) {
		SpringApplication.run(DualAgentSupportApplication.class, args);
	}

}
