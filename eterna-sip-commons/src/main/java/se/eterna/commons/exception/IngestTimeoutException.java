package se.eterna.commons.exception;

import java.time.Duration;

public class IngestTimeoutException extends EternaClientException {

    public IngestTimeoutException(String jobId, Duration timeout) {
        super("Ingest-jobb " + jobId + " avslutades inte inom " + timeout);
    }
}
