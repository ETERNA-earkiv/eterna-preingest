package se.eterna.commons.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBuffer;
import se.eterna.commons.ingest.IngestOptions;

import java.io.InputStream;
import java.util.List;

public interface EternaClient {

    TransferResource uploadZip(String filename, InputStream zip);

    Mono<TransferResource> uploadZipReactive(String filename, Flux<DataBuffer> data);

    IngestJob createJob(List<String> transferIds, IngestOptions options);

    IngestJob getJob(String jobId);

    boolean aipExists(String aipId);

    void deleteTransfer(String transferId);
}
