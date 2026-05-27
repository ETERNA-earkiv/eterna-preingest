package se.eterna.ingest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaLoader;
import se.eterna.ingest.service.JsonSchemaConverter;

import java.util.Map;

@RestController
@RequestMapping("/api/schema")
@Tag(name = "Schema", description = "Hämta schemadefinitionen för UI-formulärsrendering")
public class SchemaController {

    private final SchemaLoader schemaLoader;
    private final JsonSchemaConverter jsonSchemaConverter;

    public SchemaController(SchemaLoader schemaLoader, JsonSchemaConverter jsonSchemaConverter) {
        this.schemaLoader = schemaLoader;
        this.jsonSchemaConverter = jsonSchemaConverter;
    }

    @GetMapping
    @Operation(summary = "Returnerar JSON Schema för poster och handlingar")
    public Map<String, Object> getSchema() {
        SchemaDefinition schema = schemaLoader.getSchema();
        return jsonSchemaConverter.convert(schema);
    }
}
