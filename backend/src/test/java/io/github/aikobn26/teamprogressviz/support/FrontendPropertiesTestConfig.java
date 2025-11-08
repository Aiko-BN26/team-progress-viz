package io.github.aikobn26.teamprogressviz.support;

import java.net.URI;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;

@TestConfiguration
public class FrontendPropertiesTestConfig {

    @Bean
    FrontendProperties frontendProperties() {
        return new FrontendProperties(URI.create("https://frontend.test"), "/success", "/error");
    }
}
