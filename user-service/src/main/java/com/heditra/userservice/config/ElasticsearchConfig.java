package com.heditra.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.heditra.userservice.repository")
public class ElasticsearchConfig {
}
