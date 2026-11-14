package ge.cynthia.openatlas;

import static ge.cynthia.openatlas.Core.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.*;

@RestController
public class ApiController {
    private final Core core;
    private static final Map<String, String> STATUSES =
            new LinkedHashMap<>(Map.of("11", "To Do", "21", "In Progress", "31", "Done"));

    public ApiController(Core core) {
        this.core = core;
    }

    @RequestMapping("/**")
    public ResponseEntity<?> dispatch(
            HttpServletRequest request, @RequestBody(required = false) byte[] bytes)
            throws Exception {
        return core.execute(
                request,
                bytes,
                this::route,
                e -> map("errorMessages", List.of(e.getMessage()), "errors", Map.of()));
    }

    private Result route() throws Exception {
        String path = core.request().getRequestURI().replaceAll("/+$", "");
        String method = core.request().getMethod();
        if (path.equals("/api/openatlas/projects")) {
            core.parameters();
            if (method.equals("GET")) return ok(map("values", core.rows("project")));
            requireMethod(method, "POST");
            if (!core.actor().get("role").equals("admin"))
                throw new Failure(403, "Administrator required");
            var body = object(core.body(), "key", "name");
            String key = text(body.get("key"), 10, false);
            if (!key.matches("[A-Z][A-Z0-9]{1,9}"))
                throw new Failure(400, "Project keys contain 2–10 uppercase letters or digits");
            text(body.get("name"), 255, false);
            if (core.rows("project").stream().anyMatch(p -> p.get("key").equals(key)))
                throw new Failure(409, "Project key already exists");
            return created(core.insert("project", key, body));
        }
        if (path.equals("/rest/api/3/issue")) {
            requireMethod(method, "POST");
            core.parameters();
            core.writable();
            var data = fields(object(core.body(), "fields").get("fields"), true);
            String identifier = String.valueOf(core.sequence("issue"));
            String project = String.valueOf(cast(data.get("project")).get("key"));
            String key = project + "-" + core.sequence("project:" + project);
            String self = core.request().getRequestURL().toString() + "/" + identifier;
            data.putAll(
                    map(
                            "status",
                            map("name", "To Do"),
                            "created",
                            timestamp(),
                            "updated",
                            timestamp()));
            var item = map("id", identifier, "key", key, "self", self, "fields", data);
            core.insert("issue", identifier, item);
            core.insert("issue_key", key, map("id", identifier));
            return created(map("id", identifier, "key", key, "self", self));
        }
        if (path.equals("/rest/api/3/search/jql")) {
            requireMethod(method, "GET");
            return search();
        }
        Matcher route =
                Pattern.compile("/rest/api/3/issue/([^/]+)(?:/(transitions|comment)(?:/([^/]+))?)?")
                        .matcher(path);
        if (!route.matches()) throw new Failure(404, "Resource not found");
        core.parameters();
        var item = issue(route.group(1));
        String id = String.valueOf(item.get("id"));
        if (route.group(2) == null) {
            if (method.equals("GET")) return ok(item);
            requireMethod(method, "PUT", "DELETE");
            core.writable();
            if (method.equals("DELETE")) {
                core.delete("issue", id);
                core.delete("issue_key", item.get("key"));
                for (var comment : core.rows("comment:" + id))
                    core.delete("comment:" + id, comment.get("id"));
                return empty();
            }
            var values = fields(object(core.body(), "fields").get("fields"), false);
            cast(item.get("fields")).putAll(values);
            cast(item.get("fields")).put("updated", timestamp());
            core.save("issue", id, item);
            return empty();
        }
        if (route.group(2).equals("transitions")) {
            if (route.group(3) != null) throw new Failure(404, "Resource not found");
            String current =
                    String.valueOf(cast(cast(item.get("fields")).get("status")).get("name"));
            List<Map<String, Object>> available =
                    STATUSES.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .filter(e -> !e.getValue().equals(current))
                            .map(e ->
                                    map(
                                            "id",
                                            e.getKey(),
                                            "name",
                                            e.getValue(),
                                            "to",
                                            map("name", e.getValue())))
                            .toList();
            if (method.equals("GET")) return ok(map("transitions", available));
            requireMethod(method, "POST");
            core.writable();
            Object transition =
                    object(object(core.body(), "transition").get("transition"), "id").get("id");
            if (available.stream().noneMatch(t -> t.get("id").equals(transition)))
                throw new Failure(400, "Transition is not available");
            cast(item.get("fields"))
                    .putAll(
                            map(
                                    "status",
                                    map("name", STATUSES.get(transition)),
                                    "updated",
                                    timestamp()));
            core.save("issue", id, item);
            return empty();
        }
        String kind = "comment:" + id;
        if (route.group(3) == null) {
            if (method.equals("GET")) {
                var comments = core.rows(kind);
                return ok(
                        map(
                                "comments",
                                comments,
                                "startAt",
                                0,
                                "maxResults",
                                comments.size(),
                                "total",
                                comments.size()));
            }
            requireMethod(method, "POST");
            core.writable();
            Object body = adf(object(core.body(), "body").get("body"));
            String commentId = String.valueOf(core.sequence("comment"));
            return created(
                    core.insert(
                            kind,
                            commentId,
                            map(
                                    "id",
                                    commentId,
                                    "body",
                                    body,
                                    "author",
                                    map("accountId", core.actor().get("user")),
                                    "created",
                                    timestamp())));
        }
        var comment = core.get(kind, route.group(3));
        if (method.equals("GET")) return ok(comment);
        requireMethod(method, "PUT", "DELETE");
        core.writable();
        if (!core.actor().get("role").equals("admin")
                && !cast(comment.get("author")).get("accountId").equals(core.actor().get("user")))
            throw new Failure(403, "Only the author or an administrator may change this comment");
        if (method.equals("DELETE")) {
            core.delete(kind, route.group(3));
            return empty();
        }
        comment.putAll(
                map(
                        "body",
                        adf(object(core.body(), "body").get("body")),
                        "updated",
                        timestamp(),
                        "updateAuthor",
                        map("accountId", core.actor().get("user"))));
        return ok(core.save(kind, route.group(3), comment));
    }

    private Map<String, Object> issue(String identifier) throws Exception {
        return core.get(
                "issue",
                identifier.matches("[0-9]+")
                        ? identifier
                        : core.get("issue_key", identifier).get("id"));
    }

    private Map<String, Object> fields(Object value, boolean creating) throws Exception {
        var fields =
                object(
                        value,
                        Set.of("summary", "description", "project", "issuetype"),
                        creating ? Set.of("summary", "project", "issuetype") : Set.of());
        if (fields.containsKey("summary")) text(fields.get("summary"), 255, false);
        if (fields.containsKey("description")) adf(fields.get("description"));
        if (creating) {
            core.get("project", text(object(fields.get("project"), "key").get("key")));
            if (!Arrays.asList(map("name", "Task"), map("name", "Bug"), map("name", "Story"))
                    .contains(fields.get("issuetype")))
                throw new Failure(400, "Supported issue types: Task, Bug, Story");
        } else if (fields.containsKey("project") || fields.containsKey("issuetype")) {
            throw new Failure(400, "Moving issues or changing types is not supported");
        }
        return fields;
    }

    private Object adf(Object value) {
        var document = object(value, "type", "version", "content");
        if (!Objects.equals(document.get("type"), "doc")
                || !Objects.equals(document.get("version"), 1)
                || !(document.get("content") instanceof List<?> paragraphs))
            throw new Failure(400, "Expected an ADF version 1 document");
        for (Object paragraph : paragraphs) {
            var entry = object(paragraph, "type", "content");
            if (!Objects.equals(entry.get("type"), "paragraph")
                    || !(entry.get("content") instanceof List<?> nodes))
                throw new Failure(400, "Only paragraph ADF nodes are supported");
            for (Object node : nodes) {
                var textNode = object(node, "type", "text");
                if (!Objects.equals(textNode.get("type"), "text"))
                    throw new Failure(400, "Only plain text ADF nodes are supported");
                text(textNode.get("text"));
            }
        }
        return value;
    }

    private Result search() throws Exception {
        core.parameters("jql", "maxResults", "nextPageToken");
        Matcher query =
                Pattern.compile(
                                "project\\s*=\\s*\"?([A-Z][A-Z0-9]+)\"?(?:\\s+AND\\s+status\\s*=\\s*\"([^\"]+)\")?",
                                Pattern.CASE_INSENSITIVE)
                        .matcher(core.argument("jql", ""));
        if (!query.matches())
            throw new Failure(400, "Supported JQL: project = KEY [AND status = \"name\"]");
        core.get("project", query.group(1));
        int limit = integer(core.argument("maxResults", "50"), 1, 100);
        int offset = integer(core.argument("nextPageToken", "0"), 0, Integer.MAX_VALUE);
        var values =
                core.rows("issue").stream()
                        .filter(i ->
                                cast(cast(i.get("fields")).get("project"))
                                                .get("key")
                                                .equals(query.group(1))
                                        && (query.group(2) == null
                                                || cast(cast(i.get("fields")).get("status"))
                                                        .get("name")
                                                        .equals(query.group(2))))
                        .toList();
        int start = Math.min(offset, values.size());
        int end = (int) Math.min((long) offset + limit, values.size());
        var result = map("issues", values.subList(start, end), "isLast", end >= values.size());
        if (end < values.size()) result.put("nextPageToken", String.valueOf(end));
        return ok(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    private static void requireMethod(String actual, String... supported) {
        if (!List.of(supported).contains(actual)) throw new Failure(405, "Method not allowed");
    }
}
