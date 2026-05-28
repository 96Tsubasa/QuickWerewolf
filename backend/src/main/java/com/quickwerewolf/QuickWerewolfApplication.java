package com.quickwerewolf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuickWerewolfApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickWerewolfApplication.class, args);
	}

}
