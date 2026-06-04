package se.eterna.commons.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "eterna")
public record EternaClientProperties(
    String url,
    @DefaultValue("admin") String username,
    @DefaultValue("eterna") String password,
    // Konfigurerbart API-prefix — löser /api/v2 vs /controller/v1 beroende på ETERNA-version
    @DefaultValue("/api/v2") String apiPath,
    @DefaultValue("60s") Duration readTimeout,
    @DefaultValue("60s") Duration writeTimeout,
    @DefaultValue("false") boolean verifySsl,
    // Extra headers, t.ex. AF-SystemId för Preingest01
    Map<String, String> extraHeaders
) {}
