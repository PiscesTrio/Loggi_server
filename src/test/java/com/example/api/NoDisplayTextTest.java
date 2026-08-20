package com.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * No CJK string literal in {@code src/main/java}.
 *
 * <p>The count is zero and this keeps it there. It was 141 before this slice: Chinese in
 * {@code @Log(module=...)}, in {@link com.example.api.utils.BrowserUtil}, in the enums, in every
 * {@code BizException}, and in fifty-two Bean Validation messages. All of it ended up in one of two
 * places — a database column, or the {@code msg} field of a response — and both were the wrong
 * place for it.
 *
 * <p>The columns became identifiers (V9, V10). The responses gained an {@code errorCode}, which is
 * what a client turns into words in its reader's language (A3). Once both had happened, {@code msg}
 * stopped being something a user reads: what is left of it is a line in the server log and a
 * fallback for a client that does not know a code. That is developer-facing text, and this project
 * writes developer-facing text in English — so the last step was not a translation, it was noticing
 * that the audience had changed.
 *
 * <p>Comments are not checked. They are not sent anywhere, and several of them quote the Chinese
 * values these changes replaced, which is exactly where that text belongs now.
 *
 * <p>Neither is SQL. {@code V9} and {@code V10} must name the old values in order to convert them,
 * and {@code data.sql} carries Japanese warehouse names and addresses, which are content.
 */
class NoDisplayTextTest {

    private static final Pattern CJK_LITERAL =
            Pattern.compile("\"[^\"\\n]*[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}][^\"\\n]*\"");

    @Test
    @DisplayName("No source file under main/ contains a CJK string literal")
    void mainHoldsNoDisplayText() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String trimmed = line.strip();
                    // Comments may quote whatever they are explaining.
                    if (trimmed.startsWith("//")
                            || trimmed.startsWith("*")
                            || trimmed.startsWith("/*")) {
                        continue;
                    }
                    String code = line.split("//")[0];
                    var matcher = CJK_LITERAL.matcher(code);
                    while (matcher.find()) {
                        offenders.add(file + ":" + (i + 1) + "  " + matcher.group());
                    }
                }
            }
        }

        assertThat(offenders)
                .as(
                        "A message here reaches a reader whose language this server does not know."
                                + " Give the failure an ErrorCode and let the client say it; if it is"
                                + " genuinely for a developer, write it in English like the rest of"
                                + " the source.")
                .isEmpty();
    }
}
