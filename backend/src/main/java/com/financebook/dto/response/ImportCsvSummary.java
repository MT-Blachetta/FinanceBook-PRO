package com.financebook.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportCsvSummary {
    private int created_payments;
    private int created_recipients;
    private int updated_recipients;
    private int created_categories;
    private int skipped_rows;
}
