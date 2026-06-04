package se.eterna.commons.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import se.eterna.commons.ingest.BatchIngestService;
import se.eterna.commons.ingest.BatchIngestServiceImpl;
import se.eterna.commons.ingest.IngestService;
import se.eterna.commons.sip.EternaUploadTarget;
import se.eterna.commons.sip.SipOutputTarget;
import se.eterna.commons.sip.SipPackager;
import se.eterna.commons.sip.domain.RecordSipBuilder;

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
    public BatchIngestService batchIngestService(IngestService ingestService) {
        return new BatchIngestServiceImpl(ingestService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SipPackager sipPackager() {
        return new SipPackager();
    }

    @Bean
    @ConditionalOnMissingBean
    public RecordSipBuilder recordSipBuilder(SipPackager sipPackager) {
        return new RecordSipBuilder(sipPackager);
    }

    @Bean
    @ConditionalOnMissingBean(SipOutputTarget.class)
    public SipOutputTarget sipOutputTarget(EternaClient eternaClient) {
        return new EternaUploadTarget(eternaClient);
    }
}
