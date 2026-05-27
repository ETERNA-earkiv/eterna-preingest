package se.eterna.commons.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.eterna.commons.ingest.IngestOptions;
import se.eterna.commons.ingest.IngestResult;
import se.eterna.commons.ingest.IngestService;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EternaClientImplTest {

    @Mock
    private EternaClient mockClient;

    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        ingestService = new IngestService(mockClient);
    }

    @Test
    void ingest_successfulFlow_returnsSuccess() throws Exception {
        var transfer = new TransferResource("id1", "test.zip", "/path", "uuid-1");
        var pendingJob = new IngestJob("job-1", "Test", "CREATED", 0.0);
        var completedJob = new IngestJob("job-1", "Test", "COMPLETED", 100.0);
        var options = IngestOptions.defaults("parent-aip-id");

        when(mockClient.uploadZip(eq("test.zip"), any())).thenReturn(transfer);
        when(mockClient.createJob(List.of("uuid-1"), options)).thenReturn(pendingJob);
        when(mockClient.getJob("job-1"))
            .thenReturn(pendingJob)
            .thenReturn(completedJob);

        var zip = new ByteArrayInputStream(new byte[]{});
        IngestResult result = ingestService.ingest("test.zip", zip, options);

        assertThat(result).isInstanceOf(IngestResult.Success.class);
        assertThat(((IngestResult.Success) result).jobId()).isEqualTo("job-1");
        verify(mockClient).uploadZip(eq("test.zip"), any());
        verify(mockClient).createJob(List.of("uuid-1"), options);
    }

    @Test
    void ingest_failedJob_returnsFailure() throws Exception {
        var transfer = new TransferResource("id1", "test.zip", "/path", "uuid-1");
        var failedJob = new IngestJob("job-2", "Test", "FAILED_DURING_INGEST", 50.0);
        var options = IngestOptions.defaults("parent-aip-id");

        when(mockClient.uploadZip(any(), any())).thenReturn(transfer);
        when(mockClient.createJob(any(), any())).thenReturn(
            new IngestJob("job-2", "Test", "STARTED", 0.0)
        );
        when(mockClient.getJob("job-2")).thenReturn(failedJob);

        IngestResult result = ingestService.ingest("test.zip",
            new ByteArrayInputStream(new byte[]{}), options);

        assertThat(result).isInstanceOf(IngestResult.Failure.class);
        assertThat(((IngestResult.Failure) result).jobId()).isEqualTo("job-2");
    }

    @Test
    void ingestOptions_toPluginParameters_containsRequiredKeys() {
        var opts = IngestOptions.defaults("parent-123");
        var params = opts.toPluginParameters();

        assertThat(params).containsKey("parameter.parent_id");
        assertThat(params.get("parameter.parent_id")).isEqualTo("parent-123");
        assertThat(params).containsKey("parameter.do_file_format_identification");
        assertThat(params).containsKey("parameter.sip_to_aip_class");
    }

    @Test
    void ingestJob_terminalStates_identifiedCorrectly() {
        assertThat(new IngestJob("j", "n", "COMPLETED", 100).isTerminal()).isTrue();
        assertThat(new IngestJob("j", "n", "FAILED", 0).isTerminal()).isTrue();
        assertThat(new IngestJob("j", "n", "FAILED_DURING_INGEST", 0).isTerminal()).isTrue();
        assertThat(new IngestJob("j", "n", "STARTED", 50).isTerminal()).isFalse();
        assertThat(new IngestJob("j", "n", "CREATED", 0).isTerminal()).isFalse();
    }

    @Test
    void awaitCompletion_timeout_returnsFailure() {
        when(mockClient.getJob("job-3"))
            .thenReturn(new IngestJob("job-3", "Test", "STARTED", 50.0));

        IngestResult result = ingestService.awaitCompletion(
            "job-3", Duration.ofMillis(10), Duration.ofMillis(50)
        );

        assertThat(result).isInstanceOf(IngestResult.Failure.class);
        assertThat(((IngestResult.Failure) result).errorMessage()).contains("Timeout");
    }
}
