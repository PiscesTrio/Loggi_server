package com.example.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Two things that are only wrong until someone puts them back.
 *
 * <p>Neither is caught by any other test: a dependency nobody imports still ships, and a
 * debug switch that defaults to on still starts.
 */
class ConfigurationSafetyTest {

    @Test
    @DisplayName("fastjson is not on the classpath")
    void fastjson_isGone() {
        // com.alibaba:fastjson 1.2.73 was declared in the pom with zero imports anywhere
        // in the source - an unused dependency, which is exactly the kind that survives
        // for years because removing it fixes nothing visible. It carries the autoType
        // deserialization history, and being on the classpath is the precondition for
        // every gadget-chain attack against it; not calling it is not a defence, it is a
        // property of today's code.
        //
        // A grep would pass just as well against a pom that reintroduced it. This does not.
        assertThatThrownBy(() -> Class.forName("com.alibaba.fastjson.JSON"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    @DisplayName("SQL logging and SMTP debug are off unless explicitly asked for")
    void debugSwitches_defaultToOff() throws Exception {
        // Both shipped as a literal `true`.
        //
        // show-sql wrote every statement and its parameters to stdout on every request,
        // the audit log's own INSERTs included - so an account and IP that the schema
        // treats as a record end up in a stream nobody treats as sensitive. mail.debug
        // prints the whole SMTP conversation, and that conversation contains the AUTH
        // line: the mail password, in the server log, on every send.
        //
        // Placeholders rather than a dev/prod profile split on purpose - profiles are
        // S08's subject, and this slice should not do that work twice with a different
        // answer.
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"));
        assertThat(sources).isNotEmpty();

        PropertySource<?> yaml = sources.get(0);
        assertThat(yaml.getProperty("spring.jpa.show-sql"))
                .as("show-sql must be a placeholder defaulting to false")
                .isEqualTo("${JPA_SHOW_SQL:false}");
        assertThat(yaml.getProperty("spring.mail.properties.mail.debug"))
                .as("SMTP debug must be a placeholder defaulting to false")
                .isEqualTo("${MAIL_DEBUG:false}");
    }
}
