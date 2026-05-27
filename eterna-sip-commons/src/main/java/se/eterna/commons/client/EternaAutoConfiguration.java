package se.eterna.commons.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import se.eterna.commons.ingest.IngestService;
import se.eterna.commons.sip.SipPackager;

@AutoConfiguration
@ConditionalOnProperty("eterna.url")
@EnableConfigurationProperties(EternaClientProperties.class)
public class EternaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EternaClient eternaClient(EternaClientProperties properties) {
        return new EternaClientImpl(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IngestService ingestService(EternaClient eternaClient) {
        return new IngestService(eternaClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public SipPackager sipPackager() {
        return new SipPackager();
    }
}
