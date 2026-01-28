package com.btech.project.adaptive_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // This turns on the 5-second timer
public class AdaptiveGatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(AdaptiveGatewayApplication.class, args);
	}
}