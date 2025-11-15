package digital.shraddha.config;

import digital.shraddha.util.ApiEndPointsUtil;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerEurekaConfig {
    private final DiscoveryClient discoveryClient;

    public SwaggerEurekaConfig(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Bean
    public List<GroupedOpenApi> apis() {
        return discoveryClient.getServices().stream()
                .filter(service -> Arrays.stream(ApiEndPointsUtil.getNotRegisterServices()).noneMatch(service::equalsIgnoreCase))
                .map(service -> {
                    // Get first instance of service
                    List<ServiceInstance> instances = discoveryClient.getInstances(service);
                    String contextPath = "";

                    if (!instances.isEmpty()) {
                        // read from metadata if available
                        contextPath = instances.getFirst().getMetadata().getOrDefault("contextPath", "");
                    }

                    String finalContextPath = contextPath.endsWith("/") ? contextPath : contextPath + "/";

                    return GroupedOpenApi.builder()
                            .group(service)
                            .pathsToMatch("/" + service.toLowerCase() + finalContextPath + "**")
                            .build();
                })
                .toList();
    }
}
