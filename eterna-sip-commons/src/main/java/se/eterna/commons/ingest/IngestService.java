package se.eterna.commons.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.eterna.commons.client.EternaClient;
import se.eterna.commons.client.IngestJob;
import se.eterna.commons.client.TransferResource;
import se.eterna.commons.exception.EternaClientException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);

    private final EternaClient client;

    public IngestService(EternaClient client) {
        this.client = client;
    }

    public IngestResult ingest(String filename, Path zipPath, IngestOptions options)
            throws IOException {
        TransferResource transfer = uploadWithRetry(filename, zipPath, options);
        log.info("Uploaded SIP: transferId={}", transfer.transferId());

        IngestJob job = client.createJob(List.of(transfer.transferId()), options);
        log.info("Ingest job started: jobId={}", job.id());

        return awaitCompletion(job.id());
    }

    public IngestResult awaitCompletion(String jobId) {
        return awaitCompletion(jobId, DEFAULT_POLL_INTERVAL, DEFAULT_TIMEOUT);
    }

    public IngestResult awaitCompletion(String jobId, Duration pollInterval, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            IngestJob job = client.getJob(jobId);
            log.debug("Job {}: state={}, progress={}%", jobId, job.state(), job.percentageCompleted());

            if (job.isTerminal()) {
                if (job.isSuccess()) {
                    return new IngestResult.Success(jobId, null);
                } else {
                    return new IngestResult.Failure(jobId,
                        "Jobb avslutades med tillstånd: " + job.state());
                }
            }

            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new IngestResult.Failure(jobId, "Avbrutet under polling");
            }
        }
        return new IngestResult.Failure(jobId,
            "Timeout: ingest-jobb avslutades inte inom " + timeout);
    }

    private TransferResource uploadWithRetry(String filename, Path zipPath, IngestOptions options)
            throws IOException {
        int maxAttempts = Math.max(1, options.maxRetries() + 1);
        EternaClientException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.uploadZip(filename, zipPath);
            } catch (EternaClientException e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    log.warn("Upload misslyckades (försök {}/{}): {} — försöker igen om {}",
                        attempt, maxAttempts, e.getMessage(), options.retryDelay());
                    sleep(options.retryDelay());
                }
            }
        }
        throw new EternaClientException(
            "Upload misslyckades efter " + maxAttempts + " försök", lastException);
    }

    private void sleep(Duration duration) {
        if (duration.isZero()) return;
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
