package se.eterna.ingest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaDefinition.FieldDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Genererar ETERNA config overlay-filer från schema-definitionen.
 * Körs vid startup om ingest.auto-deploy-overlay=true.
 *
 * Genererade filer:
 *   schemas/{type}.xsd
 *   crosswalks/ingest/{type}.xslt
 *   crosswalks/dissemination/html/{type}.xslt
 *   templates/{type}.xml.hbs
 *   i18n/ServerMessages.properties
 *   i18n/ServerMessages_sv_SE.properties
 */
@Component
public class OverlayGenerator {

    private static final Logger log = LoggerFactory.getLogger(OverlayGenerator.class);

    public void generate(SchemaDefinition schema, Path overlayDir) throws IOException {
        if (schema.record() != null) {
            generateForType(schema.record(), schema.recordFields(), overlayDir);
        }
        if (schema.item() != null) {
            generateForType(schema.item(), schema.itemFields(), overlayDir);
        }
        generateI18n(schema, overlayDir);
        log.info("ETERNA config overlay genererat i: {}", overlayDir);
    }

    private void generateForType(
        SchemaDefinition.TypeConfig type,
        List<FieldDefinition> fields,
        Path overlayDir
    ) throws IOException {
        String mt = type.metadataType();
        ensureDir(overlayDir.resolve("schemas"));
        ensureDir(overlayDir.resolve("crosswalks/ingest"));
        ensureDir(overlayDir.resolve("crosswalks/dissemination/html"));
        ensureDir(overlayDir.resolve("templates"));

        writeFile(overlayDir.resolve("schemas/" + mt + ".xsd"), buildXsd(mt, fields));
        writeFile(overlayDir.resolve("crosswalks/ingest/" + mt + ".xslt"), buildIngestXslt(mt, fields));
        writeFile(overlayDir.resolve("crosswalks/dissemination/html/" + mt + ".xslt"), buildHtmlXslt(mt, type, fields));
        writeFile(overlayDir.resolve("templates/" + mt + ".xml.hbs"), buildHandlebars(mt, fields));
    }

    private String buildXsd(String metadataType, List<FieldDefinition> fields) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n");
        sb.append("           xmlns=\"urn:eterna:ingest:metadata:1.0\"\n");
        sb.append("           targetNamespace=\"urn:eterna:ingest:metadata:1.0\"\n");
        sb.append("           elementFormDefault=\"qualified\">\n\n");
        sb.append("  <xs:element name=\"").append(metadataType).append("\">\n");
        sb.append("    <xs:complexType>\n");
        sb.append("      <xs:sequence>\n");
        if (fields != null) {
            for (FieldDefinition f : fields) {
                String xsType = toXsType(f.type());
                String minOccurs = f.required() ? "1" : "0";
                sb.append("        <xs:element name=\"").append(f.name()).append("\"")
                  .append(" type=\"xs:").append(xsType).append("\"")
                  .append(" minOccurs=\"").append(minOccurs).append("\"")
                  .append(" maxOccurs=\"1\"/>\n");
            }
        }
        sb.append("      </xs:sequence>\n");
        sb.append("    </xs:complexType>\n");
        sb.append("  </xs:element>\n");
        sb.append("</xs:schema>\n");
        return sb.toString();
    }

    private String buildIngestXslt(String metadataType, List<FieldDefinition> fields) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<xsl:stylesheet version=\"2.0\"\n");
        sb.append("  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n");
        sb.append("  xmlns:md=\"urn:eterna:ingest:metadata:1.0\">\n\n");
        sb.append("  <xsl:output method=\"xml\" indent=\"yes\"/>\n\n");
        sb.append("  <xsl:template match=\"/\">\n");
        sb.append("    <doc>\n");
        if (fields != null) {
            for (FieldDefinition f : fields) {
                String suffix = isDateField(f.type()) ? "_dt" : "_txt";
                sb.append("      <field name=\"").append(f.name()).append(suffix).append("\">\n");
                sb.append("        <xsl:value-of select=\"//md:").append(f.name()).append("\"/>\n");
                sb.append("      </field>\n");
            }
        }
        sb.append("      <field name=\"metadataType_txt\">").append(metadataType).append("</field>\n");
        sb.append("    </doc>\n");
        sb.append("  </xsl:template>\n");
        sb.append("</xsl:stylesheet>\n");
        return sb.toString();
    }

    private String buildHtmlXslt(
        String metadataType,
        SchemaDefinition.TypeConfig type,
        List<FieldDefinition> fields
    ) {
        String label = type.label() != null ? type.label().sv() : metadataType;
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<xsl:stylesheet version=\"2.0\"\n");
        sb.append("  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n");
        sb.append("  xmlns:md=\"urn:eterna:ingest:metadata:1.0\">\n\n");
        sb.append("  <xsl:output method=\"html\" indent=\"yes\"/>\n\n");
        sb.append("  <xsl:template match=\"/\">\n");
        sb.append("    <div class=\"metadata-panel\">\n");
        sb.append("      <h4>").append(escapeXml(label)).append("</h4>\n");
        sb.append("      <table class=\"table\">\n");
        if (fields != null) {
            for (FieldDefinition f : fields) {
                String fieldLabel = f.label() != null ? f.label().sv() : f.name();
                sb.append("        <tr>\n");
                sb.append("          <th>").append(escapeXml(fieldLabel)).append("</th>\n");
                sb.append("          <td><xsl:value-of select=\"//md:")
                  .append(f.name()).append("\"/></td>\n");
                sb.append("        </tr>\n");
            }
        }
        sb.append("      </table>\n");
        sb.append("    </div>\n");
        sb.append("  </xsl:template>\n");
        sb.append("</xsl:stylesheet>\n");
        return sb.toString();
    }

    private String buildHandlebars(String metadataType, List<FieldDefinition> fields) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<").append(metadataType).append(" xmlns=\"urn:eterna:ingest:metadata:1.0\">\n");
        if (fields != null) {
            for (FieldDefinition f : fields) {
                sb.append("  <").append(f.name()).append(">")
                  .append("{{").append(f.name()).append("}}")
                  .append("</").append(f.name()).append(">\n");
            }
        }
        sb.append("</").append(metadataType).append(">\n");
        return sb.toString();
    }

    private void generateI18n(SchemaDefinition schema, Path overlayDir) throws IOException {
        ensureDir(overlayDir.resolve("i18n"));
        var enSb = new StringBuilder();
        var svSb = new StringBuilder();

        for (var typeAndFields : List.of(
            java.util.Map.entry(schema.record(), schema.recordFields()),
            java.util.Map.entry(schema.item(), schema.itemFields())
        )) {
            var type = typeAndFields.getKey();
            var fields = typeAndFields.getValue();
            if (type == null || fields == null) continue;
            String mt = type.metadataType();
            for (FieldDefinition f : fields) {
                String enLabel = f.label() != null && f.label().en() != null ? f.label().en() : f.name();
                String svLabel = f.label() != null && f.label().sv() != null ? f.label().sv() : f.name();
                enSb.append("metadataField.").append(mt).append(".").append(f.name())
                    .append("=").append(enLabel).append("\n");
                svSb.append("metadataField.").append(mt).append(".").append(f.name())
                    .append("=").append(svLabel).append("\n");
            }
        }

        writeFile(overlayDir.resolve("i18n/ServerMessages.properties"), enSb.toString());
        writeFile(overlayDir.resolve("i18n/ServerMessages_sv_SE.properties"), svSb.toString());
    }

    private String toXsType(SchemaDefinition.FieldType type) {
        return switch (type) {
            case date -> "date";
            case datetime -> "dateTime";
            case integer -> "integer";
            case decimal -> "decimal";
            case bool -> "boolean";
            default -> "string";
        };
    }

    private boolean isDateField(SchemaDefinition.FieldType type) {
        return type == SchemaDefinition.FieldType.date || type == SchemaDefinition.FieldType.datetime;
    }

    private String escapeXml(String v) {
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void ensureDir(Path dir) throws IOException {
        Files.createDirectories(dir);
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
        log.debug("Genererat: {}", path);
    }
}
