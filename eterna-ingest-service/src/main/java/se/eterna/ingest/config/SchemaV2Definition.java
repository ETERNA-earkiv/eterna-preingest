package se.eterna.ingest.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Modell för schema_v2.yaml — styr vilka fält som accepteras i API och UI.
 */
@JsonIgnoreProperties({"_defs"})
public record SchemaV2Definition(
    List<RecordGroup> recordsList
) {
    public record RecordGroup(
            List<TypeConfig> records
    ) {}

    public record TypeConfig(
            String metadataType,
            Label label,
            String rootElement,
            String wrapperElement,
            String namespace,
            List<SchemaDefinition.FieldDefinition> fields
    ) {}

    public record Label(String sv, String en) {
        public String forLocale(String lang) {
            return "sv".equals(lang) ? sv : en;
        }
    }

}
