package com.example.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on the auditing listener that fills {@link com.example.api.model.entity.Auditable}.
 *
 * <p>Its own class rather than an annotation on the application class, because {@code @DataJpaTest}
 * does not load the application class: a slice test would silently get null timestamps and the test
 * written to check them would fail for a reason that has nothing to do with the entity. Importing
 * this one configuration is the fix, and having somewhere to import is why it exists separately.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
