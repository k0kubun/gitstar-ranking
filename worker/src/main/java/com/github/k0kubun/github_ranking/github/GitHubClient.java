package com.github.k0kubun.github_ranking.github;

import com.github.k0kubun.github_ranking.github.AccessTokenFactory;
import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.User;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.sentry.Sentry;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubClient
{
    private static final int MAX_RETRY = 3;
    private static final Integer PAGE_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(GitHubClient.class);
    private static final String API_ENDPOINT = "https://api.github.com";
    private static final String GRAPHQL_ENDPOINT = API_ENDPOINT + "/graphql";

    private final AccessTokenFactory tokenFactory;
    private final HttpRequestFactory requestFactory;

    public GitHubClient(AccessTokenFactory tokenFactory)
    {
        this.tokenFactory = tokenFactory;
        requestFactory = new NetHttpTransport().createRequestFactory();
    }

    public GitHubClient(String token)
    {
        this(new StaticTokenFactory(token));
    }

    public String getLogin(Integer userId)
            throws IOException
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
        JsonObject userNode = responseObject.getJsonObject("data").getJsonObject("node");
        if (userNode.containsKey("login")) {
            return userNode.getString("login");
        }
        else { // bot user like id=30333062 returns `node {}`
            throw new UserNotFoundException("user_id = " + userId + " did not have login");
        }
    }

    public List<Repository> getPublicRepos(Integer userId, boolean isOrganization)
            throws IOException
    {
        List<Repository> repos = new ArrayList<>();
        for (JsonObject node : getPublicRepoNodes(userId, isOrganization)) {
            try {
                Long id = decodeRepositoryId(node.getString("id"));
                String encodedOwnerId = node.getJsonObject("owner").getString("id");
                Integer ownerId = isOrganization ? decodeOrganizationId(encodedOwnerId) : decodeUserId(encodedOwnerId);
                String name = node.getString("name");
                String fullName = node.getString("nameWithOwner");
                String description = node.isNull("description") ? null : node.getString("description");
                Boolean fork = node.getBoolean("isFork");
                String homepage = node.isNull("homepageUrl") ? null : node.getString("homepageUrl");
                int stargazersCount = node.getJsonObject("stargazers").getInt("totalCount");
                String language = node.isNull("primaryLanguage") ? null : node.getJsonObject("primaryLanguage").getString("name");

                repos.add(new Repository(id, ownerId, name, fullName, description, fork, homepage, stargazersCount, language));
            }
            catch (ClassCastException e) {
                Sentry.capture(e);
                LOG.debug("node: " + node.toString());
                throw e;
            }
        }
        return repos;
    }

    public int getRateLimitRemaining()
    {
        try {
            JsonObject responseObject = graphql("query { rateLimit { remaining } }");
            // TODO: handle error object
            return responseObject.getJsonObject("data").getJsonObject("rateLimit").getInt("remaining");
        }
        catch (IOException e) {
            Sentry.capture(e);
            return 0;
        }
    }

    public List<User> getUsersSince(int since)
            throws IOException
    {
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(API_ENDPOINT + "/users?since=" + since));

        HttpHeaders headers = new HttpHeaders();
        headers.setAuthorization("bearer " + tokenFactory.getToken());
        request.setHeaders(headers);

        HttpResponse response = executeWithRetry(request);
        // TODO: Handle error status code
        List<JsonObject> userObjects = Json.createReader(new StringReader(response.parseAsString())).readArray().getValuesAs(JsonObject.class);
        List<User> users = new ArrayList<>();
        for (JsonObject userObject : userObjects) {
            User user = new User(userObject.getInt("id"), userObject.getString("type"));
            user.setLogin(userObject.getString("login"));
            user.setAvatarUrl(userObject.getString("avatar_url"));
            users.add(user);
        }
        return users;
    }

    private JsonObject graphql(String query)
            throws IOException
    {
        String payload = Json.createObjectBuilder()
                .add("query", query)
                .add("variables", "{}")
                .build().toString();

        HttpRequest request = requestFactory.buildPostRequest(
                new GenericUrl(GRAPHQL_ENDPOINT),
                ByteArrayContent.fromString("application/json", payload));

        HttpHeaders headers = new HttpHeaders();
        headers.setAuthorization("bearer " + tokenFactory.getToken());
        request.setHeaders(headers);

        HttpResponse response = executeWithRetry(request);
        // TODO: Handle error status code
        JsonObject responseObject = Json.createReader(new StringReader(response.parseAsString())).readObject();
        if (responseObject.containsKey("errors")) {
            LOG.debug("errors with query:\n" + query);
            LOG.debug("response:\n" + responseObject.toString());
        }
        return responseObject;
    }

    private List<JsonObject> getPublicRepoNodes(Integer userId, boolean isOrganization)
            throws IOException
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
                LOG.debug("Paginate user_id: " + userId.toString() + " with cursor: " + cursor + " size: " + nodes.size());
            }
            else {
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

    private Integer decodeOrganizationId(String encoded)
    {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        // TODO: Raise error if prefix is wrong
        return Integer.valueOf(decoded.replaceFirst("12:Organization", ""));
    }

    private void handleUserNodeErrors(JsonObject responseObject)
    {
        if (responseObject.containsKey("errors")) {
            List<JsonObject> errors = responseObject.getJsonArray("errors").getValuesAs(JsonObject.class);
            for (JsonObject error : errors) { // TODO: Log suppressed errors
                if (error.containsKey("type") && error.getString("type").equals("NOT_FOUND") && error.containsKey("path")) {
                    for (JsonString path : error.getJsonArray("path").getValuesAs(JsonString.class)) {
                        if (path.getString().equals("node")) {
                            throw new UserNotFoundException(error.getString("message"));
                        }
                    }
                }
            }

            throw new GraphQLUnhandledException(responseObject.toString());
        }
    }

    private HttpResponse executeWithRetry(HttpRequest request)
            throws IOException
    {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return request.execute();
            }
            catch (HttpResponseException e) {
                if (e.getStatusCode() == 502) {
                    LOG.debug("retrying 502: " + e.getMessage() + ", i=" + i);
                    try {
                        TimeUnit.SECONDS.sleep(i * i); // exponential retry
                    }
                    catch (InterruptedException ie) {
                        Sentry.capture(ie);
                    }
                }
                else {
                    throw e;
                }
            }
            catch (SocketTimeoutException e) {
                LOG.debug("retrying socket timeout: " + e.getMessage() + ", i=" + i);
                try {
                    TimeUnit.SECONDS.sleep(i * i); // exponential retry
                }
                catch (InterruptedException ie) {
                    Sentry.capture(ie);
                }
            }
        }
        return request.execute();
    }

    public class NodeNotFoundException
            extends RuntimeException
    {
        public NodeNotFoundException(String message)
        {
            super(message);
        }
    }

    public class UserNotFoundException
            extends RuntimeException
    {
        public UserNotFoundException(String message)
        {
            super(message);
        }
    }

    public class GraphQLUnhandledException
            extends RuntimeException
    {
        public GraphQLUnhandledException(String message)
        {
            super(message);
        }
    }
}
