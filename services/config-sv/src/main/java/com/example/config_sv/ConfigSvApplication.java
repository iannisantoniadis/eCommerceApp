package com.example.config_sv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigSvApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigSvApplication.class, args);
	}

}
