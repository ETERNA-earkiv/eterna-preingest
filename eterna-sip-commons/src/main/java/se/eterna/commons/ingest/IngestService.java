package se.eterna.commons.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.eterna.commons.client.EternaClient;
import se.eterna.commons.client.IngestJob;
import se.eterna.commons.client.TransferResource;

import java.io.IOException;
import java.io.InputStream;
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

    public IngestResult ingest(String filename, InputStream zip, IngestOptions options)
        throws IOException {
        TransferResource transfer = client.uploadZip(filename, zip);
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
                    // ETERNA rapporter innehåller aipId — returnera jobId för nu,
                    // anroparen kan hämta aipId separat via getJob-polling i sin kontext
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
        return new IngestResult.Failure(jobId, "Timeout efter " + timeout);
    }
}
