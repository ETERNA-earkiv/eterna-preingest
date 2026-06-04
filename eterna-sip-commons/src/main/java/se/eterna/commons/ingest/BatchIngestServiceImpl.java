package se.eterna.commons.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BatchIngestServiceImpl implements BatchIngestService {

    private static final Logger log = LoggerFactory.getLogger(BatchIngestServiceImpl.class);

    private final IngestService ingestService;

    public BatchIngestServiceImpl(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @Override
    public List<IngestResult> ingestAll(List<SipJob> jobs) {
        var results = new ArrayList<IngestResult>(jobs.size());
        for (int i = 0; i < jobs.size(); i++) {
            SipJob job = jobs.get(i);
            log.info("Batch-ingest {}/{}: {}", i + 1, jobs.size(), job.filename());
            try {
                IngestResult result = ingestService.ingest(job.filename(), job.zipPath(), job.options());
                results.add(result);
                if (result instanceof IngestResult.Failure f) {
                    log.warn("Jobb {} misslyckades: {}", job.filename(), f.errorMessage());
                }
            } catch (Exception e) {
                log.error("Oväntat fel vid ingest av {}: {}", job.filename(), e.getMessage(), e);
                results.add(new IngestResult.Failure(
                    job.filename(), "Oväntat fel: " + e.getMessage(), List.of()));
            }
        }
        return results;
    }
}
