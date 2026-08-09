package com.financebook.controller;

import com.financebook.dto.response.ImportCsvSummary;
import com.financebook.service.ImportService;
import com.opencsv.exceptions.CsvValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Import", description = "Data import endpoints")
@RestController
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @Operation(summary = "Import CSV", description = "Import payment items from CSV file")
    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportCsvSummary> importCsv(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException, CsvValidationException {

        ImportCsvSummary summary = importService.importCsv(file, authentication.getName());

        return ResponseEntity.ok(summary);
    }
}
