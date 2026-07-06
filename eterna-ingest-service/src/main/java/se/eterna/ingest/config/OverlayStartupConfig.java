package se.eterna.ingest.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import se.eterna.ingest.service.OverlayGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "ingest.auto-deploy-overlay", havingValue = "true", matchIfMissing = true)
public class OverlayStartupConfig {

    private static final Logger log = LoggerFactory.getLogger(OverlayStartupConfig.class);

    private final SchemaLoader schemaLoader;
    private final OverlayGenerator overlayGenerator;

    @Value("${ingest.overlay-dir:/config/overlay}")
    private String overlayDir;

    public OverlayStartupConfig(SchemaLoader schemaLoader, OverlayGenerator overlayGenerator) {
        this.schemaLoader = schemaLoader;
        this.overlayGenerator = overlayGenerator;
    }

    //@PostConstruct
    public void generateOverlay() throws Exception {
        Path dir = Path.of(overlayDir);
        Files.createDirectories(dir);
        overlayGenerator.generate(schemaLoader.getSchema(), dir);
        log.info("ETERNA config overlay redo i: {} — kopiera till ETERNA:s config-katalog", dir);
    }
}
