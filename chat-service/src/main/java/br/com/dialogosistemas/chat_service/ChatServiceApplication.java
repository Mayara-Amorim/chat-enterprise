package br.com.dialogosistemas.chat_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import br.com.dialogosistemas.chat_service.infra.config.FileUploadProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileUploadProperties.class)
public class
ChatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);
	}

}




































