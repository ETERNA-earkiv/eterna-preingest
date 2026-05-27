package se.eterna.ingest.dto;

public record RecordStatusResponse(
    String jobId,
    String status,
    Double percentageCompleted,
    String aipId    // Fylls när ingestionen är klar
) {}
