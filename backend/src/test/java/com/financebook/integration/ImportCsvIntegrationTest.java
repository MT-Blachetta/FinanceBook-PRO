package com.financebook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CSV import: happy path, reuse of existing recipients/categories, and
 * resilience against malformed rows (skipped and counted, never fatal).
 */
class ImportCsvIntegrationTest extends IntegrationTestBase {

    private static final String HEADER =
            "amount;date;description;Recipient name;Recipient address;standard_category name;periodic\r\n";

    @Test
    void importsRowsCreatesEntitiesAndSkipsBadLines() throws Exception {
        String token = registerAndLogin("import_user");

        String csv = HEADER
                + "-12.50;2026-01-10;Groceries;Corner Shop;Main St 1;Food;false\r\n"
                + "-30.00;2026-01-11;More groceries;Corner Shop;;Food;false\r\n"
                + "not-a-number;2026-01-12;Broken;X;;Y;false\r\n"
                + "-1.00;2026-01-13\r\n"
                + "2500.00;2026-01-31;Salary;Employer AG;Business Park 2;Income;true\r\n";

        MvcResult result = mockMvc.perform(multipart("/import-csv")
                        .file(new MockMultipartFile(
                                "file", "export.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode summary = parse(result);
        assertThat(summary.get("created_payments").asInt()).isEqualTo(3);
        assertThat(summary.get("created_recipients").asInt()).isEqualTo(2); // Corner Shop, Employer AG
        assertThat(summary.get("created_categories").asInt()).isEqualTo(2); // Food, Income
        assertThat(summary.get("skipped_rows").asInt()).isEqualTo(2);

        // Payments really exist
        MvcResult items = mockMvc.perform(get("/payment-items")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(items)).hasSize(3);

        // Importing the same file again must reuse recipients/categories
        MvcResult secondRun = mockMvc.perform(multipart("/import-csv")
                        .file(new MockMultipartFile(
                                "file", "export.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode secondSummary = parse(secondRun);
        assertThat(secondSummary.get("created_recipients").asInt()).isZero();
        assertThat(secondSummary.get("created_categories").asInt()).isZero();
        assertThat(secondSummary.get("created_payments").asInt()).isEqualTo(3);
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        String token = registerAndLogin("import_empty");

        mockMvc.perform(multipart("/import-csv")
                        .file(new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/import-csv")
                        .file(new MockMultipartFile("file", "x.csv", "text/csv", "a;b".getBytes())))
                .andExpect(status().isUnauthorized());
    }
}
