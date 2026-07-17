package br.com.dialogosistemas.chat_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileUploadProperties.class)
@EnableScheduling
public class
ChatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);
	}

}




































