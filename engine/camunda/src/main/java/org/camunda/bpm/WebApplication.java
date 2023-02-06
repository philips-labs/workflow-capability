package org.camunda.bpm;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableProcessApplication
@EnableAsync
public class WebApplication {
	
	public static void main(String... args) {
		SpringApplication.run(WebApplication.class, args);
	}
	
}
