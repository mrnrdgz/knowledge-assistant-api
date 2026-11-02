package io.github.mrnrdgz.knowledgeassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KnowledgeAssistantApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowledgeAssistantApiApplication.class, args);
	}

}
