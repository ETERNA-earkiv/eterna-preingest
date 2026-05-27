package se.eterna.commons.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestJob(String id, String name, String state, double percentageCompleted) {

    public boolean isTerminal() {
        return switch (state) {
            case "COMPLETED", "FAILED_DURING_INGEST", "FAILED",
                 "COMPLETED_WITH_PROBLEMS", "STOPPED" -> true;
            default -> false;
        };
    }

    public boolean isSuccess() {
        return "COMPLETED".equals(state) || "COMPLETED_WITH_PROBLEMS".equals(state);
    }
}
