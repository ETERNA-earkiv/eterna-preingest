package se.eterna.ingest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.eterna.ingest.dto.RecordStatusResponse;
import se.eterna.ingest.dto.SubmitRecordRequest;
import se.eterna.ingest.dto.SubmitRecordResponse;
import se.eterna.ingest.service.RecordService;
import se.eterna.ingest.service.ValidationException;

import java.util.Map;

@RestController
@RequestMapping("/api/records")
@Tag(name = "Records", description = "Skicka poster och handlingar för arkivering i ETERNA")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Skicka en post eller handling för arkivering")
    public SubmitRecordResponse submit(@Valid @RequestBody SubmitRecordRequest request)
        throws Exception {
        return recordService.submit(request);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Hämta status för ett arkiveringsjobb")
    public RecordStatusResponse getStatus(@PathVariable String jobId) {
        return recordService.getStatus(jobId);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Valideringsfel",
            "details", ex.getErrors()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of(
            "error", ex.getMessage()
        ));
    }
}
