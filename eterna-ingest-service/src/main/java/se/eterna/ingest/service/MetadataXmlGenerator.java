package se.eterna.ingest.service;

import org.springframework.stereotype.Component;
import se.eterna.ingest.config.SchemaDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Genererar metadata-XML från fältvärden och schema-definition.
 * Ingen template-fil behövs — XML-strukturen härledas direkt ur schemat.
 */
@Component
public class MetadataXmlGenerator {

    public Path generate(
        String rootElement,
        Map<String, String> fields,
        List<SchemaDefinition.FieldDefinition> fieldDefs,
        Path outputDir
    ) throws IOException {
        String xml = buildXml(rootElement, fields, fieldDefs);
        Path file = outputDir.resolve(rootElement + ".xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        return file;
    }

    private String buildXml(
        String rootElement,
        Map<String, String> fields,
        List<SchemaDefinition.FieldDefinition> fieldDefs
    ) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<").append(rootElement).append(
            " xmlns=\"urn:eterna:ingest:metadata:1.0\">\n"
        );

        for (SchemaDefinition.FieldDefinition def : fieldDefs) {
            String value = fields.get(def.name());
            if (value != null && !value.isBlank()) {
                sb.append("  <").append(def.name()).append(">")
                  .append(escapeXml(value))
                  .append("</").append(def.name()).append(">\n");
            }
        }

        sb.append("</").append(rootElement).append(">\n");
        return sb.toString();
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
