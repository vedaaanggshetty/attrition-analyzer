package com.example.APIGateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges every business service's own OpenAPI document into the Gateway's,
 * so http://localhost:8080/swagger-ui.html is a single unified page covering
 * every endpoint the frontend actually calls - not a dropdown of separate
 * pages. Each service still generates and can serve its own doc
 * independently (see each service's own OpenApiConfig); this only reads
 * those documents and copies their content in, it doesn't change them.
 *
 * How it works, per service:
 * 1. Fetch its raw {@code /v3/api-docs} JSON via a load-balanced RestTemplate
 *    (Eureka resolves the service name, same as the Gateway's own routes).
 * 2. Prefix its schema names (e.g. {@code ErrorResponse} -> {@code
 *    AuthErrorResponse}) and rewrite every {@code $ref} pointing at them,
 *    since multiple services independently define same-named DTOs
 *    (every service has its own {@code ErrorResponse}) that would otherwise
 *    silently collide when merged into one document.
 * 3. Rename each service's tag(s) to the single group name it should appear
 *    under (e.g. User Profile Service's separate "Profile"/"Registration"
 *    tags both become "User Profile").
 * 4. Copy paths, schemas, tags, and security schemes into the Gateway's
 *    document - except {@code /internal/**} paths, which are deliberately
 *    excluded because the Gateway has no route for them and never proxies
 *    them; documenting them here would misrepresent what's actually
 *    reachable through this Gateway.
 *
 * If a service is unreachable when the doc is being built, it's skipped
 * (logged, not thrown) so one down service doesn't blank out the whole page.
 */
@Component
public class AggregatedOpenApiCustomizer implements GlobalOpenApiCustomizer {

    private static final Logger log = LoggerFactory.getLogger(AggregatedOpenApiCustomizer.class);
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final String INTERNAL_PATH_PREFIX = "/internal";

    private record Upstream(String serviceName, String schemaPrefix, Map<String, String> tagRenames) {
    }

    private static final List<Upstream> UPSTREAMS = List.of(
            new Upstream("authentication-service", "Auth", Map.of("Authentication", "Authentication")),
            new Upstream("user-profile-service", "Profile", Map.of(
                    "Registration", "User Profile",
                    "Profile", "User Profile")),
            new Upstream("employee-service", "Employee", Map.of("Employees", "Employee")),
            new Upstream("notification-service", "Notification", Map.of("Notifications", "Notifications"))
    );

    private final RestTemplate restTemplate;

    public AggregatedOpenApiCustomizer(RestTemplate loadBalancedRestTemplate) {
        this.restTemplate = loadBalancedRestTemplate;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }

        // Keyed by final (possibly merged/renamed) tag name, so User Profile
        // Service's two source tags collapse into one entry here.
        Map<String, Tag> mergedTags = new LinkedHashMap<>();

        for (Upstream upstream : UPSTREAMS) {
            try {
                mergeUpstream(openApi, upstream, mergedTags);
            } catch (Exception ex) {
                log.warn("Could not merge OpenAPI docs from '{}' into the aggregated Gateway doc - "
                        + "it may be starting up or unreachable. Its endpoints won't appear until "
                        + "the doc is rebuilt (e.g. on the Gateway's next restart or cache refresh).",
                        upstream.serviceName(), ex);
            }
        }

        // Drop any tag with no surviving path - e.g. "Internal Credentials",
        // whose only endpoint was excluded above since the Gateway never
        // proxies /internal/**. Without this, an orphaned tag would still
        // show up in Swagger UI's sidebar with nothing under it.
        Set<String> usedTagNames = new HashSet<>();
        openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    if (operation.getTags() != null) {
                        usedTagNames.addAll(operation.getTags());
                    }
                }));
        mergedTags.keySet().retainAll(usedTagNames);

        openApi.setTags(List.copyOf(mergedTags.values()));

        markAccessLevels(openApi);
    }

    private static final String GUEST_PATH_PREFIX = "/employees/analysis";

    /**
     * Prepends a plain-language access-level label to every operation's
     * description, computed from its actual (post-merge) security
     * requirement - never hand-typed, so it can't drift from what the
     * Gateway/services really enforce. Guest-accessible endpoints (the
     * attrition-analysis routes, called from the public landing page) are
     * called out separately from "Public" even though both need no token,
     * since the frontend treats them as a distinct, more limited surface.
     */
    private void markAccessLevels(OpenAPI openApi) {
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
            boolean requiresAuth = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
            String label = requiresAuth
                    ? "**Requires Bearer JWT (HR-only).**"
                    : path.startsWith(GUEST_PATH_PREFIX)
                            ? "**Guest-accessible - no token required.**"
                            : "**Public - no token required.**";
            String existing = operation.getDescription();
            operation.setDescription(existing == null || existing.isBlank()
                    ? label
                    : label + "\n\n" + existing);
        }));
    }

    private void mergeUpstream(OpenAPI target, Upstream upstream, Map<String, Tag> mergedTags) throws Exception {
        String rawJson = restTemplate.getForObject(
                "http://" + upstream.serviceName() + "/v3/api-docs", String.class);
        if (rawJson == null) {
            return;
        }

        ObjectNode root = (ObjectNode) Json31.mapper().readTree(rawJson);

        Map<String, String> schemaRenames = renameSchemas(root, upstream.schemaPrefix());
        rewriteSchemaRefs(root, schemaRenames);
        renameTags(root, upstream.tagRenames());

        mergePaths(target, root);
        mergeSchemas(target, root);
        mergeTags(root, mergedTags);
        mergeSecuritySchemes(target, root);
    }

    /** Renames every entry under components.schemas with the service's prefix, in place. Returns old-&gt;new name map. */
    private Map<String, String> renameSchemas(ObjectNode root, String prefix) {
        Map<String, String> renames = new LinkedHashMap<>();
        JsonNode schemas = root.path("components").path("schemas");
        if (!schemas.isObject()) {
            return renames;
        }
        ObjectNode schemasNode = (ObjectNode) schemas;

        List<String> originalNames = List.copyOf(collectFieldNames(schemasNode));
        for (String name : originalNames) {
            String newName = prefix + name;
            renames.put(name, newName);
            JsonNode value = schemasNode.remove(name);
            schemasNode.set(newName, value);
        }
        return renames;
    }

    private List<String> collectFieldNames(ObjectNode node) {
        return node.properties().stream().map(Map.Entry::getKey).toList();
    }

    /** Rewrites every "$ref": "#/components/schemas/OldName" anywhere in the tree to the renamed schema. */
    private void rewriteSchemaRefs(JsonNode node, Map<String, String> renames) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            JsonNode ref = obj.get("$ref");
            if (ref != null && ref.isTextual() && ref.asText().startsWith(SCHEMA_REF_PREFIX)) {
                String oldName = ref.asText().substring(SCHEMA_REF_PREFIX.length());
                String newName = renames.get(oldName);
                if (newName != null) {
                    obj.put("$ref", SCHEMA_REF_PREFIX + newName);
                }
            }
            obj.elements().forEachRemaining(child -> rewriteSchemaRefs(child, renames));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> rewriteSchemaRefs(child, renames));
        }
    }

    /** Renames every operation's tag entries (and the top-level tag declarations) per the given map, in place. */
    private void renameTags(ObjectNode root, Map<String, String> tagRenames) {
        if (tagRenames.isEmpty()) {
            return;
        }
        root.path("paths").elements().forEachRemaining(pathItem ->
                pathItem.elements().forEachRemaining(operation -> {
                    JsonNode tagsNode = operation.get("tags");
                    if (tagsNode != null && tagsNode.isArray()) {
                        for (int i = 0; i < tagsNode.size(); i++) {
                            String original = tagsNode.get(i).asText();
                            String renamed = tagRenames.get(original);
                            if (renamed != null) {
                                ((com.fasterxml.jackson.databind.node.ArrayNode) tagsNode).set(i, renamed);
                            }
                        }
                    }
                }));

        JsonNode tagsArray = root.get("tags");
        if (tagsArray != null && tagsArray.isArray()) {
            for (JsonNode tagNode : tagsArray) {
                if (tagNode instanceof ObjectNode tagObj) {
                    JsonNode nameNode = tagObj.get("name");
                    if (nameNode != null) {
                        String renamed = tagRenames.get(nameNode.asText());
                        if (renamed != null) {
                            tagObj.put("name", renamed);
                        }
                    }
                }
            }
        }
    }

    /** Copies every path except /internal/** into the target document. */
    private void mergePaths(OpenAPI target, ObjectNode root) throws Exception {
        JsonNode pathsNode = root.path("paths");
        if (!pathsNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = pathsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String path = entry.getKey();
            if (path.startsWith(INTERNAL_PATH_PREFIX)) {
                continue;
            }
            PathItem pathItem = Json31.mapper().treeToValue(entry.getValue(), PathItem.class);
            target.getPaths().addPathItem(path, pathItem);
        }
    }

    private void mergeSchemas(OpenAPI target, ObjectNode root) throws Exception {
        JsonNode schemasNode = root.path("components").path("schemas");
        if (!schemasNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = schemasNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Schema<?> schema = Json31.mapper().treeToValue(entry.getValue(), Schema.class);
            target.getComponents().addSchemas(entry.getKey(), schema);
        }
    }

    private void mergeTags(ObjectNode root, Map<String, Tag> mergedTags) throws Exception {
        JsonNode tagsArray = root.get("tags");
        if (tagsArray == null || !tagsArray.isArray()) {
            return;
        }
        for (JsonNode tagNode : tagsArray) {
            Tag tag = Json31.mapper().treeToValue(tagNode, Tag.class);
            mergedTags.putIfAbsent(tag.getName(), tag);
        }
    }

    private void mergeSecuritySchemes(OpenAPI target, ObjectNode root) throws Exception {
        JsonNode schemesNode = root.path("components").path("securitySchemes");
        if (!schemesNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = schemesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (target.getComponents().getSecuritySchemes() != null
                    && target.getComponents().getSecuritySchemes().containsKey(entry.getKey())) {
                continue;
            }
            SecurityScheme scheme = Json31.mapper().treeToValue(entry.getValue(), SecurityScheme.class);
            target.getComponents().addSecuritySchemes(entry.getKey(), scheme);
        }
    }
}
