package com.example.api.config;

import com.example.api.annotation.DisableBaseResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Makes the generated document describe what the server actually sends.
 *
 * <p>springdoc derives a response schema from the controller's return type. This API does
 * not send that: {@code GlobalResponseHandler} is a {@code ResponseBodyAdvice} that wraps
 * every success into {@code {code, status, msg, data}} <em>after</em> the method returns, so
 * the document claimed {@code GET /api/commodity} answers with an array of CommodityVo while
 * the server answers with an envelope containing one.
 *
 * <p>That gap is not cosmetic. It is the reason a client generated from this document would
 * fail to parse every single response - it would look for the array where the envelope is -
 * and it makes "OpenAPI as the single source of truth" untrue while it stands.
 *
 * <p>So the two follow one rule. This customizer applies the same decision the advice makes:
 * wrap unless the handler opted out with {@link DisableBaseResponse}, and never wrap a 204,
 * which carries no body at all.
 */
@Configuration
public class EnvelopeOpenApiCustomizer {

    /** Marks operations the advice will not wrap, so the pass below can skip them. */
    private static final String OPT_OUT = "x-loggi-no-envelope";

    /**
     * Records the opt-outs.
     *
     * <p>An {@link OperationCustomizer} is the only hook that sees the handler method, and
     * the annotation is the only way to know an endpoint bypasses the advice. Nothing in
     * production uses it today; it is honoured anyway, because the alternative is a document
     * that silently becomes wrong the first time somebody does.
     */
    @Bean
    public OperationCustomizer envelopeOptOutMarker() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(DisableBaseResponse.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(DisableBaseResponse.class)) {
                operation.addExtension(OPT_OUT, true);
            }
            return operation;
        };
    }

    /**
     * Wraps every success response, and registers a named schema for each wrapper.
     *
     * <p>Named rather than inline: a generator turns an inline object into an anonymous model
     * with a name derived from its position, so the client ends up with types called things
     * like {@code InlineResponse200}. {@code ResponseResultCommodityVoList} says what it is.
     */
    @Bean
    public OpenApiCustomizer envelopeCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Components components = openApi.getComponents();

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> wrap(operation, components)));
        };
    }

    private void wrap(Operation operation, Components components) {
        if (operation.getExtensions() != null && operation.getExtensions().remove(OPT_OUT) != null) {
            return;
        }
        if (operation.getResponses() == null) {
            return;
        }
        operation.getResponses().forEach((status, response) -> {
            // 204 is the one response with no envelope, because a body contradicting a
            // No Content status is worse than an inconsistency in the convention.
            if (!status.startsWith("2") || "204".equals(status)) {
                return;
            }
            wrapResponse(response, components);
        });
    }

    private void wrapResponse(ApiResponse response, Components components) {
        Content content = response.getContent();
        if (content == null || content.isEmpty()) {
            return;
        }
        content.forEach((mediaTypeName, mediaType) -> {
            Schema<?> data = mediaType.getSchema();
            if (data == null || isAlreadyWrapped(data)) {
                return;
            }
            String name = wrapperNameFor(data);
            if (name != null && !components.getSchemas().containsKey(name)) {
                components.addSchemas(name, envelopeSchema(data));
            }
            mediaType.setSchema(name == null
                    ? envelopeSchema(data)
                    : new Schema<>().$ref("#/components/schemas/" + name));
        });
    }

    private boolean isAlreadyWrapped(Schema<?> schema) {
        String ref = schema.get$ref();
        return ref != null && ref.contains("ResponseResult");
    }

    /**
     * A name for the wrapper, derived from what it wraps.
     *
     * <p>Returns null for anything without a referenced type - a bare string or number - and
     * the caller inlines those. Naming them would produce {@code ResponseResultString}, which
     * reads like a type this codebase has and does not.
     */
    private String wrapperNameFor(Schema<?> data) {
        String ref = data.get$ref();
        if (ref != null) {
            return "ResponseResult" + simpleName(ref);
        }
        if (data instanceof ArraySchema array && array.getItems() != null
                && array.getItems().get$ref() != null) {
            return "ResponseResult" + simpleName(array.getItems().get$ref()) + "List";
        }
        return null;
    }

    private static String simpleName(String ref) {
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    private Schema<Object> envelopeSchema(Schema<?> data) {
        ObjectSchema envelope = new ObjectSchema();
        envelope.setDescription("""
                The standard response envelope. `code` repeats the HTTP status; `status` is \
                false only on a failure, where `msg` explains it and `data` is null.""");
        envelope.addProperty("code", new IntegerSchema().description("Repeats the HTTP status."));
        envelope.addProperty("status", new BooleanSchema().description("False on a failure."));
        envelope.addProperty("msg", new StringSchema().description("Set on a failure.").nullable(true));
        envelope.addProperty("data", data);
        return envelope.properties(Map.copyOf(envelope.getProperties()));
    }
}
