package com.financebook.service;

import com.financebook.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @Test
    void plainFilenamesAreKept() {
        assertThat(FileStorageService.sanitizeFilename("icon.svg")).isEqualTo("icon.svg");
        assertThat(FileStorageService.sanitizeFilename("my-icon_2.png")).isEqualTo("my-icon_2.png");
    }

    @Test
    void directoryComponentsAreStripped() {
        assertThat(FileStorageService.sanitizeFilename("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileStorageService.sanitizeFilename("/absolute/path/icon.png")).isEqualTo("icon.png");
        assertThat(FileStorageService.sanitizeFilename("C:\\Users\\evil\\icon.png")).isEqualTo("icon.png");
    }

    @Test
    void unsafeCharactersAreReplaced() {
        assertThat(FileStorageService.sanitizeFilename("wei rd nä me!.svg"))
                .matches("[A-Za-z0-9._-]+");
    }

    @Test
    void traversalOnlyNamesAreRejected() {
        assertThatThrownBy(() -> FileStorageService.sanitizeFilename(".."))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> FileStorageService.sanitizeFilename("../"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> FileStorageService.sanitizeFilename("."))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankNamesAreRejected() {
        assertThatThrownBy(() -> FileStorageService.sanitizeFilename(null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> FileStorageService.sanitizeFilename("   "))
                .isInstanceOf(ValidationException.class);
    }
}
