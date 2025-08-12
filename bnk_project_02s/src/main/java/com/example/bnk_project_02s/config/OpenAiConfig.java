package com.example.bnk_project_02s.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.theokanning.openai.service.OpenAiService;

@Configuration
public class OpenAiConfig {
  @Bean
  OpenAiService openAi(@Value("${openai.api.key}") String key){
    return new OpenAiService(key, Duration.ofSeconds(60));
  }
}
