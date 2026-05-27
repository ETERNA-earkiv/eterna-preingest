package se.eterna.ingest.service;

import org.springframework.stereotype.Component;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaDefinition.FieldDefinition;
import se.eterna.ingest.config.SchemaDefinition.FieldType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SchemaValidator {

    public List<String> validate(
        Map<String, String> fields,
        List<FieldDefinition> fieldDefs,
        String contextLabel
    ) {
        List<String> errors = new ArrayList<>();

        for (FieldDefinition def : fieldDefs) {
            String value = fields.get(def.name());

            if (def.required() && (value == null || value.isBlank())) {
                errors.add(contextLabel + ": obligatoriskt fält saknas: " + def.name());
                continue;
            }

            if (value != null && !value.isBlank()) {
                validateType(def, value, contextLabel, errors);
            }
        }

        // Okända fält ger varning men inte fel
        for (String key : fields.keySet()) {
            boolean known = fieldDefs.stream().anyMatch(f -> f.name().equals(key));
            if (!known) {
                errors.add(contextLabel + ": okänt fält ignoreras: " + key);
            }
        }

        return errors;
    }

    private void validateType(
        FieldDefinition def,
        String value,
        String ctx,
        List<String> errors
    ) {
        switch (def.type()) {
            case date -> {
                try { LocalDate.parse(value); }
                catch (DateTimeParseException e) {
                    errors.add(ctx + ": fält '" + def.name() + "' är inte ett giltigt datum (YYYY-MM-DD): " + value);
                }
            }
            case integer -> {
                try { Long.parseLong(value); }
                catch (NumberFormatException e) {
                    errors.add(ctx + ": fält '" + def.name() + "' är inte ett heltal: " + value);
                }
            }
            case decimal -> {
                try { Double.parseDouble(value); }
                catch (NumberFormatException e) {
                    errors.add(ctx + ": fält '" + def.name() + "' är inte ett decimaltal: " + value);
                }
            }
            case enumeration -> {
                if (!def.values().contains(value)) {
                    errors.add(ctx + ": fält '" + def.name() + "' har ogiltigt värde '" + value +
                               "'. Tillåtna: " + def.values());
                }
            }
            case bool -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    errors.add(ctx + ": fält '" + def.name() + "' måste vara 'true' eller 'false': " + value);
                }
            }
            default -> {} // string, text, email, url — ingen ytterligare validering
        }
    }
}
