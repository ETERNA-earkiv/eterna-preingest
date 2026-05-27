package se.eterna.ingest;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import se.eterna.ingest.config.SchemaLoader;
import se.eterna.ingest.service.OverlayGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class IngestApplication {

    private static final Logger log = LoggerFactory.getLogger(IngestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IngestApplication.class, args);
    }
}
