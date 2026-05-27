package se.eterna.commons.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import se.eterna.commons.ingest.IngestOptions;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EternaClientImpl implements EternaClient {

    private static final Logger log = LoggerFactory.getLogger(EternaClientImpl.class);

    private final WebClient webClient;
    private final String apiPath;

    public EternaClientImpl(EternaClientProperties props) {
        this.apiPath = props.apiPath();

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.writeTimeout().toMillis())
            .responseTimeout(props.readTimeout());

        var builder = WebClient.builder()
            .baseUrl(props.url())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeaders(h -> h.setBasicAuth(props.username(), props.password()));

        if (props.extraHeaders() != null) {
            props.extraHeaders().forEach((k, v) -> builder.defaultHeader(k, v));
        }

        this.webClient = builder.build();
    }

    @Override
    public TransferResource uploadZip(String filename, InputStream zip) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("upl", new NamedInputStreamResource(filename, zip));

        return webClient.post()
            .uri(apiPath + "/transfers")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header("X-Request-Id", UUID.randomUUID().toString())
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .bodyToMono(TransferResource.class)
            .block(Duration.ofMinutes(10));
    }

    @Override
    public Mono<TransferResource> uploadZipReactive(String filename, Flux<DataBuffer> data) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("upl", BodyInserters.fromPublisher(data, DataBuffer.class));

        return webClient.post()
            .uri(apiPath + "/transfers")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .bodyToMono(TransferResource.class);
    }

    @Override
    public IngestJob createJob(List<String> transferIds, IngestOptions options) {
        Map<String, Object> sourceObjects = new HashMap<>();
        sourceObjects.put("type", "list");
        sourceObjects.put("ids", transferIds);
        sourceObjects.put("selectedClass", "org.roda.core.data.v2.ip.TransferredResource");

        Map<String, Object> jobBody = new HashMap<>();
        jobBody.put("name", "Ingest via eterna-sip-commons");
        jobBody.put("plugin", options.sipToAipPlugin());
        jobBody.put("pluginType", "INGEST");
        jobBody.put("sourceObjects", sourceObjects);
        jobBody.put("priority", "MEDIUM");
        jobBody.put("parallelism", "NORMAL");
        jobBody.put("pluginParameters", options.toPluginParameters());

        return webClient.post()
            .uri(apiPath + "/jobs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(jobBody)
            .retrieve()
            .bodyToMono(IngestJob.class)
            .block(Duration.ofSeconds(30));
    }

    @Override
    public IngestJob getJob(String jobId) {
        return webClient.get()
            .uri(apiPath + "/jobs/{id}", jobId)
            .retrieve()
            .bodyToMono(IngestJob.class)
            .block(Duration.ofSeconds(30));
    }

    @Override
    public boolean aipExists(String aipId) {
        try {
            webClient.get()
                .uri(apiPath + "/aips/{id}", aipId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));
            return true;
        } catch (WebClientResponseException.NotFound e) {
            return false;
        }
    }

    @Override
    public void deleteTransfer(String transferId) {
        webClient.delete()
            .uri(apiPath + "/transfers/{id}", transferId)
            .retrieve()
            .toBodilessEntity()
            .block(Duration.ofSeconds(30));
        log.debug("Deleted transfer: {}", transferId);
    }

    // Hjälpklass för att namnge InputStream-resursen i multipart-uppladdning
    private static final class NamedInputStreamResource extends InputStreamResource {
        private final String filename;

        NamedInputStreamResource(String filename, InputStream stream) {
            super(stream);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
