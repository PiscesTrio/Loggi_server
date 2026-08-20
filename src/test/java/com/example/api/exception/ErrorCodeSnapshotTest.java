package com.example.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The list of error codes, pinned against a committed file.
 *
 * <p>Not a test of behaviour — a test that a decision was made. {@link ErrorCode} is read by a
 * second repository, and the app's own contract test reads a copy of the OpenAPI document rather
 * than this enum, so it is structurally incapable of noticing a code added here: its copy is simply
 * older. Nothing in this build can fail that one.
 *
 * <p>What this can do is make the change visible on the side where it happens. Adding a constant
 * turns this red, and the file it points at carries the three steps the other repository needs.
 *
 * <p>Order is part of the comparison. Not because order means anything on the wire — it does not —
 * but because a snapshot that ignores it stops reading like the enum, and the point of the file is
 * that someone reads it.
 */
class ErrorCodeSnapshotTest {

    @Test
    @DisplayName("The committed list matches the enum, in order")
    void snapshotMatches() throws IOException {
        List<String> snapshot =
                Files.readAllLines(Path.of("src", "test", "resources", "error-codes.txt")).stream()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();

        List<String> actual = Arrays.stream(ErrorCode.values()).map(Enum::name).toList();

        assertThat(actual)
                .as(
                        "ErrorCode changed. Update src/test/resources/error-codes.txt, and do the"
                                + " three things its header lists — the app cannot say a code it has"
                                + " not been told about, and it will not fail its own build over one"
                                + " it has never heard of.")
                .isEqualTo(snapshot);
    }
}
