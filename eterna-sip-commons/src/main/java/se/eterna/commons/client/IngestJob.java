package se.eterna.commons.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestJob(
    String id,
    String name,
    String state,
    double percentageCompleted,
    JobStats jobStats
) {

    public IngestJob(String id, String name, String state, double percentageCompleted) {
        this(id, name, state, percentageCompleted, null);
    }

    @Override
    public double percentageCompleted() {
        return jobStats != null ? jobStats.completionPercentage() : percentageCompleted;
    }

    public boolean isTerminal() {
        return switch (state) {
            case "COMPLETED", "COMPLETED_WITH_PROBLEMS", "FAILED", "FAILED_TO_COMPLETE",
                 "FAILED_DURING_CREATION", "FAILED_DURING_INGEST", "STOPPED",
                 "PENDING_APPROVAL", "REJECTED", "SCHEDULED" -> true;
            default -> false;
        };
    }

    public boolean isSuccess() {
        return "COMPLETED".equals(state) || "COMPLETED_WITH_PROBLEMS".equals(state);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobStats(double completionPercentage) {}
}
