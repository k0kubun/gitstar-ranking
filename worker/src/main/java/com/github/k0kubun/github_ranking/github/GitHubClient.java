package com.github.k0kubun.github_ranking.github;

import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.User;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubClient
{
    private static final Integer PAGE_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(GitHubClient.class);
    private static final String GRAPHQL_ENDPOINT = "https://api.github.com/graphql";

    private final String accessToken;
    private final HttpRequestFactory requestFactory;

    public GitHubClient(String accessToken)
    {
        this.accessToken = accessToken;
        requestFactory = new NetHttpTransport().createRequestFactory();
    }

    public String getLogin(Integer userId) throws IOException
    {
        JsonObject responseObject = graphql(
                "query {" +
                "\nnode(id:\"" + encodeUserId(userId) + "\") {" +
                "\n  ... on User {" +
                "\n    login" +
                "\n  }" +
                "\n}" +
                "}"
                );
        handleUserNodeErrors(responseObject);
        return responseObject.getJsonObject("data").getJsonObject("node").getString("login");
    }

    public List<Repository> getPublicRepos(Integer userId, boolean isOrganization) throws IOException
    {
        List<Repository> repos = new ArrayList<>();
        for (JsonObject node : getPublicRepoNodes(userId, isOrganization)) {
            try {
                Long id = decodeRepositoryId(node.getString("id"));
                Integer ownerId = decodeUserId(node.getJsonObject("owner").getString("id"));
                String name = node.getString("name");
                String fullName = node.getString("nameWithOwner");
                String description = node.isNull("description") ? null : node.getString("description");
                Boolean fork = node.getBoolean("isFork");
                String homepage = node.isNull("homepageUrl") ? null : node.getString("homepageUrl");
                int stargazersCount = node.getJsonObject("stargazers").getInt("totalCount");
                String language = node.isNull("primaryLanguage") ? null : node.getJsonObject("primaryLanguage").getString("name");

                repos.add(new Repository(id, ownerId, name, fullName, description, fork, homepage, stargazersCount, language));
            } catch (ClassCastException e) {
                LOG.debug("node: " + node.toString());
                throw e;
            }
        }
        return repos;
    }

    private JsonObject graphql(String query) throws IOException
    {
        String payload = Json.createObjectBuilder()
            .add("query", query)
            .add("variables", "{}")
            .build().toString();

        HttpRequest request = requestFactory.buildPostRequest(
                new GenericUrl(GRAPHQL_ENDPOINT),
                ByteArrayContent.fromString("application/json", payload));

        HttpHeaders headers = new HttpHeaders();
        headers.setAuthorization("bearer " + accessToken);
        request.setHeaders(headers);

        HttpResponse response = request.execute();
        // TODO: Handle error status code
        JsonObject responseObject = Json.createReader(new StringReader(response.parseAsString())).readObject();
        if (responseObject.containsKey("errors")) {
            LOG.debug("errors with query: " + query);
        }
        return responseObject;
    }

    private List<JsonObject> getPublicRepoNodes(Integer userId, boolean isOrganization) throws IOException
    {
        String cursor = null;
        List<JsonObject> nodes = new ArrayList<>();

        while (true) {
            String after = "";
            if (cursor != null) {
                after = " after:\"" + cursor + "\"";
            }
            String query =
                    "query {" +
                    "\nnode(id:\"" + encodeUserId(userId) + "\") {" +
                    "\n  ... on " + (isOrganization ? "Organization" : "User") + " {" +
                    "\n    repositories(first:" + PAGE_SIZE.toString() + after + " privacy:PUBLIC affiliations:OWNER) {" +
                    "\n      edges {" +
                    "\n        cursor" +
                    "\n        node {" +
                    "\n          ... on Repository {" +
                    "\n            id" +
                    "\n            owner {" +
                    "\n              id" +
                    "\n            }" +
                    "\n            name" +
                    "\n            nameWithOwner" +
                    "\n            description" +
                    "\n            isFork" +
                    "\n            homepageUrl" +
                    "\n            stargazers {" +
                    "\n              totalCount" +
                    "\n            }" +
                    "\n            primaryLanguage {" +
                    "\n              name" +
                    "\n            }" +
                    "\n          }" +
                    "\n        }" +
                    "\n      }" +
                    "\n    }" +
                    "\n  }" +
                    "\n}" +
                    "}";
            JsonObject responseObject = graphql(query);
            handleUserNodeErrors(responseObject);

            JsonObject node = responseObject.getJsonObject("data").getJsonObject("node");
            if (!node.containsKey("repositories")) {
                throw new NodeNotFoundException(query + "\n" + responseObject.toString());
            }
            List<JsonObject> edges = node.getJsonObject("repositories").getJsonArray("edges").getValuesAs(JsonObject.class);
            for (JsonObject edge : edges) {
                nodes.add(edge.getJsonObject("node"));
            }

            if (edges.size() == PAGE_SIZE) {
                cursor = edges.get(PAGE_SIZE - 1).getString("cursor");
            } else {
                break;
            }
        }
        return nodes;
    }

    private String encodeUserId(Integer id)
    {
        String unencoded = "04:User" + id.toString();
        return Base64.getEncoder().encodeToString(unencoded.getBytes());
    }

    private Integer decodeUserId(String encoded)
    {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        // TODO: Raise error if prefix is wrong
        return Integer.valueOf(decoded.replaceFirst("04:User", ""));
    }

    private Long decodeRepositoryId(String encoded)
    {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        // TODO: Raise error if prefix is wrong
        return Long.valueOf(decoded.replaceFirst("010:Repository", ""));
    }

    private void handleUserNodeErrors(JsonObject responseObject)
    {
        if (responseObject.containsKey("errors")) {
            List<JsonObject> errors = responseObject.getJsonArray("errors").getValuesAs(JsonObject.class);
            for (JsonObject error : errors) { // TODO: Log suppressed errors
                if (error.containsKey("type") && error.getString("type") == "NOT_FOUND" && error.containsKey("path")) {
                    for (JsonValue path : error.getJsonArray("path").getValuesAs(JsonValue.class)) {
                        if (path.toString() == "node") {
                            throw new UserNotFoundException(error.getString("message"));
                        }
                    }
                }
            }

            StringBuilder builder = new StringBuilder();
            for (JsonObject error : errors) {
                builder.append(error.getString("message"));
                builder.append("\n");
            }
            throw new GraphQLUnhandledException(builder.toString());
        }
    }

    public class NodeNotFoundException extends RuntimeException
    {
        public NodeNotFoundException(String message)
        {
            super(message);
        }
    }

    public class UserNotFoundException extends RuntimeException
    {
        public UserNotFoundException(String message)
        {
            super(message);
        }
    }

    public class GraphQLUnhandledException extends RuntimeException
    {
        public GraphQLUnhandledException(String message)
        {
            super(message);
        }
    }
}
