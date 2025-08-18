package com.aiaca.btop;

import com.aiaca.btop.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class BtopApplication {

	public static void main(String[] args) {
		SpringApplication.run(BtopApplication.class, args);
	}

}
