package digital.shraddha.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwaggerUiAggregatorConfig {

	@Value ("${spring.application.name}")
	private String selfServiceName;

	private final DiscoveryClient discoveryClient;
	private final SwaggerUiConfigProperties swaggerUiConfigProperties;

	@EventListener (HeartbeatEvent.class)
	public void refreshSwaggerUrls(HeartbeatEvent event) {
		Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = discoveryClient.getServices().stream()
				.filter(service -> ! service.equalsIgnoreCase(selfServiceName))
				.flatMap(service -> discoveryClient.getInstances(service).stream()
						.map(instance -> {
							String contextPath = instance.getMetadata().getOrDefault("contextPath", "").trim();
							String apiDocsPath = instance.getMetadata().getOrDefault("apiDocsPath", "/v3/api-docs");
							String url = instance.getUri() + contextPath + apiDocsPath;
							return new AbstractSwaggerUiConfigProperties.SwaggerUrl(service, url, service);
						}))
				.collect(Collectors.toSet());

		swaggerUiConfigProperties.setUrls(urls);
		log.info("Refreshed Swagger URLs: {}", urls);
	}
}
