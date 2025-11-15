package digital.shraddha.config;

import digital.shraddha.util.ApiEndPointsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwaggerUiAggregatorConfig {

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";  // gateway URL
    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    @EventListener(HeartbeatEvent.class)
    public void refreshSwaggerUrls(HeartbeatEvent event) {

        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = discoveryClient.getServices().stream()
                .filter(service -> Arrays.stream(ApiEndPointsUtil.getNotRegisterServices())
                        .noneMatch(service::equalsIgnoreCase))
                .flatMap(service -> discoveryClient.getInstances(service).stream()
                        .map(instance -> {
                            String contextPath = instance.getMetadata().getOrDefault("contextPath", "").trim();
                            String apiDocsPath = instance.getMetadata().getOrDefault("apiDocsPath", "/api-docs");

                            // Build URL through Gateway
                            String url = GATEWAY_BASE_URL + "/" + service  + "/api/v1" + apiDocsPath;

                            log.info("Discovered Swagger URL for service {}: {}", service, url);

                            return new AbstractSwaggerUiConfigProperties.SwaggerUrl(service, url, service);
                        }))
                .collect(Collectors.toSet());

        swaggerUiConfigProperties.setUrls(urls);
        log.info("Refreshed Gateway-based Swagger URLs: {}", urls);
    }
}
