package se.eterna.ingest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaLoader;
import se.eterna.ingest.config.SchemaV2Definition;
import se.eterna.ingest.config.SchemaV2Loader;
import se.eterna.ingest.service.JsonSchemaConverter;
import se.eterna.ingest.service.JsonSchemaV2Converter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schemav2")
@Tag(name = "Schema v2", description = "Hämta schemadefinitionen v2 för UI-formulärsrendering")
public class SchemaV2Controller {

    private final SchemaV2Loader schemaLoader;
    private final JsonSchemaV2Converter jsonSchemaConverter;

    public SchemaV2Controller(SchemaV2Loader schemaLoader, JsonSchemaV2Converter jsonSchemaConverter) {
        this.schemaLoader = schemaLoader;
        this.jsonSchemaConverter = jsonSchemaConverter;
    }

    @GetMapping
    @Operation(summary = "Returnerar JSON Schema för poster och handlingar")
    public List<List<Map<String, Object>>> getSchema() {
        SchemaV2Definition schema = schemaLoader.getSchema();
        return jsonSchemaConverter.convert(schema);
    }
}
