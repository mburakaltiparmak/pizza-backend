package com.example.pizza.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public Health health() {
        try {
            log.debug("Checking Elasticsearch cluster health...");

            // Query cluster health
            HealthResponse healthResponse = elasticsearchClient.cluster()
                    .health(h -> h.timeout(t -> t.time("5s")));

            String status = healthResponse.status().toString();
            int numberOfNodes = healthResponse.numberOfNodes();
            int activeShards = healthResponse.activeShards();
            int activePrimaryShards = healthResponse.activePrimaryShards();
            int relocatingShards = healthResponse.relocatingShards();
            int initializingShards = healthResponse.initializingShards();
            int unassignedShards = healthResponse.unassignedShards();

            log.info("Elasticsearch cluster status: {}, nodes: {}, active shards: {}",
                    status, numberOfNodes, activeShards);

            // Build health status
            // NOTE: YELLOW is acceptable for single-node clusters (replicas cannot be allocated)

            Health.Builder healthBuilder;

            switch (status.toUpperCase()) {
                case "GREEN":
                    healthBuilder = Health.up();
                    log.debug("Elasticsearch health: GREEN - All shards active");
                    break;
                case "YELLOW":
                    // YELLOW is HEALTHY for single-node development/testing
                    healthBuilder = Health.up();
                    if (numberOfNodes == 1) {
                        log.info(
                                "Elasticsearch health: YELLOW (expected for single-node) - {} unassigned replica shards",
                                unassignedShards);
                    } else {
                        log.warn("Elasticsearch health: YELLOW - {} replicas unassigned on {}-node cluster",
                                unassignedShards, numberOfNodes);
                        healthBuilder.withDetail("warning", "Some replicas are unassigned");
                    }
                    break;
                case "RED":
                    healthBuilder = Health.down()
                            .withDetail("error", "Cluster status is RED - some primary shards are unassigned");
                    log.error("Elasticsearch health: RED - Primary shards unassigned!");
                    break;
                default:
                    healthBuilder = Health.unknown();
                    log.warn("Elasticsearch health: UNKNOWN status '{}'", status);
            }

            return healthBuilder
                    .withDetail("cluster", healthResponse.clusterName())
                    .withDetail("status", status)
                    .withDetail("nodes", numberOfNodes)
                    .withDetail("activeShards", activeShards)
                    .withDetail("activePrimaryShards", activePrimaryShards)
                    .withDetail("relocatingShards", relocatingShards)
                    .withDetail("initializingShards", initializingShards)
                    .withDetail("unassignedShards", unassignedShards)
                    .build();

        } catch (Exception e) {
            log.error("Elasticsearch health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }
}