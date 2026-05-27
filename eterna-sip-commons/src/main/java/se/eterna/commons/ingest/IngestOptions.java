package se.eterna.commons.ingest;

import java.util.HashMap;
import java.util.Map;

public record IngestOptions(
    String parentAipId,
    String sipToAipPlugin,
    boolean virusCheck,
    boolean formatIdentification,
    boolean metadataValidation,
    boolean producerAuthCheck,
    boolean applyDisposalRules,
    boolean createSubmission,
    boolean forceParentId,
    int totalSteps,
    String emailNotification
) {
    private static final String DEFAULT_PLUGIN =
        "org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin";

    public static IngestOptions defaults(String parentAipId) {
        return new IngestOptions(
            parentAipId, DEFAULT_PLUGIN,
            false, true, true, true, true, true, true, 8, null
        );
    }

    public Map<String, String> toPluginParameters() {
        var map = new HashMap<String, String>();
        map.put("parameter.parent_id", parentAipId);
        map.put("parameter.force_parent_id", String.valueOf(forceParentId));
        map.put("parameter.do_virus_check", String.valueOf(virusCheck));
        map.put("parameter.do_file_format_identification", String.valueOf(formatIdentification));
        map.put("parameter.do_descriptive_metadata_validation", String.valueOf(metadataValidation));
        map.put("parameter.do_producer_authorization_check", String.valueOf(producerAuthCheck));
        map.put("parameter.do_apply_disposal_rules", String.valueOf(applyDisposalRules));
        map.put("parameter.create_submission", String.valueOf(createSubmission));
        map.put("parameter.total_steps", String.valueOf(totalSteps));
        map.put("parameter.sip_to_aip_class", sipToAipPlugin);
        map.put("parameter.create.premis.skeleton", "true");
        map.put("parameter.reporting_class", "true");
        map.put("parameter.accept", "true");
        if (emailNotification != null && !emailNotification.isBlank()) {
            map.put("parameter.email_notification", emailNotification);
        }
        return map;
    }
}
