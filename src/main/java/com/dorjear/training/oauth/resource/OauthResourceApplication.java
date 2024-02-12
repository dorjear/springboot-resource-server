package com.dorjear.training.oauth.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.dorjear.training.oauth")
public class OauthResourceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OauthResourceApplication.class, args);
	}

}

