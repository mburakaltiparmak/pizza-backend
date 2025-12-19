package com.example.pizza.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.pizza.repository.search")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.connection-timeout:10s}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private String socketTimeout;

    @Value("${elasticsearch.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * Creates RestClient bean for low-level Elasticsearch communication
     *
     * Best Practices:
     * - Connection pooling (default: 30 connections per route)
     * - Configurable timeouts
     * - Supports authentication (if needed)
     * - Health check ready
     *
     * @return configured RestClient instance
     */
    @Bean
    public RestClient restClient() {
        log.info("Initializing Elasticsearch RestClient with URI: {}", elasticsearchUri);

        try {
            // Parse URI (format: http://host:port or https://host:port)
            String[] parts = elasticsearchUri.replace("http://", "")
                    .replace("https://", "")
                    .split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            String scheme = elasticsearchUri.startsWith("https") ? "https" : "http";

            log.debug("Elasticsearch connection details - Host: {}, Port: {}, Scheme: {}", host, port, scheme);

            RestClient client = RestClient.builder(new HttpHost(host, port, scheme))
                    .setRequestConfigCallback(requestConfigBuilder ->
                            requestConfigBuilder
                                    .setConnectTimeout(parseTimeoutToMillis(connectionTimeout))
                                    .setSocketTimeout(parseTimeoutToMillis(socketTimeout)))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        // Optional: Add authentication if needed
                        // CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        // credentialsProvider.setCredentials(AuthScope.ANY,
                        //     new UsernamePasswordCredentials("username", "password"));
                        // return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        return httpClientBuilder;
                    })
                    .build();

            log.info("Elasticsearch RestClient initialized successfully");
            return client;

        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch RestClient", e);
            throw new RuntimeException("Elasticsearch configuration failed", e);
        }
    }

    // Creates ElasticsearchTransport bean (Jackson-based JSON mapping)

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        log.info("Creating ElasticsearchTransport with JacksonJsonpMapper");
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }


    // Creates ElasticsearchClient bean (main API for ES operations)

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        log.info("Creating ElasticsearchClient");
        ElasticsearchClient client = new ElasticsearchClient(transport);

        if (syncEnabled) {
            log.info("Elasticsearch sync is ENABLED");
        } else {
            log.warn("Elasticsearch sync is DISABLED - search features may be limited");
        }

        return client;
    }

    private int parseTimeoutToMillis(String timeout) {
        if (timeout.endsWith("s")) {
            return Integer.parseInt(timeout.replace("s", "")) * 1000;
        } else if (timeout.endsWith("ms")) {
            return Integer.parseInt(timeout.replace("ms", ""));
        } else if (timeout.endsWith("m")) {
            return Integer.parseInt(timeout.replace("m", "")) * 60 * 1000;
        }
        // Default: assume milliseconds
        return Integer.parseInt(timeout);
    }
}