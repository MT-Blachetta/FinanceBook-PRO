package com.financebook.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    @Test
    void nullInputBecomesEmptyString() {
        assertThat(NameNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void emptyAndWhitespaceOnlyBecomeEmpty() {
        assertThat(NameNormalizer.normalize("")).isEmpty();
        assertThat(NameNormalizer.normalize("   ")).isEmpty();
        assertThat(NameNormalizer.normalize("\t\n  \r")).isEmpty();
    }

    @Test
    void leadingAndTrailingWhitespaceIsStripped() {
        assertThat(NameNormalizer.normalize("  Groceries  ")).isEqualTo("Groceries");
    }

    @Test
    void internalWhitespaceRunsCollapseToSingleSpace() {
        assertThat(NameNormalizer.normalize("Monthly \t\t rent \n payment"))
                .isEqualTo("Monthly rent payment");
    }

    @Test
    void alreadyNormalizedNamesAreUnchanged() {
        assertThat(NameNormalizer.normalize("Insurance 2026")).isEqualTo("Insurance 2026");
    }

    @Test
    void unicodeContentIsPreserved() {
        assertThat(NameNormalizer.normalize("  Kaffee & Küche  ")).isEqualTo("Kaffee & Küche");
    }
}
