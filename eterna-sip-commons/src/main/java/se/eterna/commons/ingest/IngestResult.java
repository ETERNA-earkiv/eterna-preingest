package se.eterna.commons.ingest;

import java.util.List;

public sealed interface IngestResult permits IngestResult.Success, IngestResult.Failure {

    record Success(String jobId, String aipId) implements IngestResult {}

    record Failure(String jobId, String errorMessage, List<String> details)
        implements IngestResult {

        public Failure(String jobId, String errorMessage) {
            this(jobId, errorMessage, List.of());
        }
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }
}
