package com.github.k0kubun.gitstar_ranking.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.User
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import java.io.StringReader
import java.lang.RuntimeException
import java.net.SocketTimeoutException
import java.util.ArrayList
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonString
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.LoggerFactory

private const val MAX_RETRY = 3
private const val PAGE_SIZE = 100
private const val API_ENDPOINT = "https://api.github.com"
private const val GRAPHQL_ENDPOINT = "$API_ENDPOINT/graphql"

class NodeNotFoundException(message: String) : RuntimeException(message)
class UserNotFoundException(message: String) : RuntimeException(message)
class GraphQLUnhandledException(message: String) : RuntimeException(message)

private data class RateLimitResponse(val resources: RateLimitResources)
private data class RateLimitResources(val core: RateLimit, val graphql: RateLimit)
private data class RateLimit(val remaining: Int)

class GitHubClient(private val token: String) {
    private val logger = LoggerFactory.getLogger(GitHubClient::class.simpleName)
    private val client = ClientBuilder.newBuilder()
        .property(ClientProperties.CONNECT_TIMEOUT, 5000)
        .property(ClientProperties.READ_TIMEOUT, 30000)
        .register(
            JacksonJsonProvider(
                ObjectMapper().registerModule(KotlinModule())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            )
        )
        .build().target(API_ENDPOINT)
    private val requestFactory: HttpRequestFactory = NetHttpTransport().createRequestFactory()

    val rateLimitRemaining: Int
        get() {
            return requestGet<RateLimitResponse>("/rate_limit").resources.graphql.remaining
        }

    fun getLogin(userId: Long): String {
        val responseObject = graphql("""
            query {
                node(id:"${encodeUserId(userId)}") {
                    ... on User {
                        login
                    }
                }
            }
        """.trimIndent())
        handleUserNodeErrors(responseObject)
        val userNode = responseObject.getJsonObject("data").getJsonObject("node")
        return if (userNode.containsKey("login")) {
            userNode.getString("login")
        } else { // bot user like id=30333062 returns `node {}`
            throw UserNotFoundException("user_id = $userId did not have login")
        }
    }

    fun getPublicRepos(userId: Long, isOrganization: Boolean): List<Repository> {
        val repos: MutableList<Repository> = ArrayList()
        getPublicRepoNodes(userId, isOrganization).forEach { node ->
            val id = decodeRepositoryId(node.getString("id"))
            val encodedOwnerId = node.getJsonObject("owner").getString("id")
            val ownerId = if (isOrganization) decodeOrganizationId(encodedOwnerId) else decodeUserId(encodedOwnerId)
            val name = node.getString("name")
            val fullName = node.getString("nameWithOwner")
            val description = if (node.isNull("description")) null else node.getString("description")
            val fork = node.getBoolean("isFork")
            val homepage = if (node.isNull("homepageUrl")) null else node.getString("homepageUrl")
            val stargazersCount = node.getJsonObject("stargazers").getInt("totalCount")
            val language = if (node.isNull("primaryLanguage")) null else node.getJsonObject("primaryLanguage").getString("name")
            if (userId == java.lang.Long.valueOf(ownerId.toLong())) { // eliminate writable but not-owning repositories for some implicit consistency, like star count
                repos.add(Repository(id, ownerId, name, fullName, description, fork, homepage, stargazersCount, language))
            }
        }
        return repos
    }

    fun getUserWithLogin(login: String): User {
        val request = requestFactory.buildGetRequest(GenericUrl("$API_ENDPOINT/users/$login"))
        val headers = com.google.api.client.http.HttpHeaders()
        headers.authorization = "bearer $token"
        request.headers = headers
        val response = executeWithRetry(request)
        val userObject = Json.createReader(StringReader(response.parseAsString())).readObject()
        val user = User(java.lang.Long.valueOf(userObject.getInt("id").toLong()), userObject.getString("type"))
        user.login = userObject.getString("login")
        user.avatarUrl = userObject.getString("avatar_url")
        return user
    }

    fun getUsersSince(since: Long): List<User> {
        val request = requestFactory.buildGetRequest(GenericUrl("$API_ENDPOINT/users?per_page=100&since=$since"))
        val headers = com.google.api.client.http.HttpHeaders()
        headers.authorization = "bearer $token"
        request.headers = headers
        val response = executeWithRetry(request)
        val userObjects = Json.createReader(StringReader(response.parseAsString())).readArray().getValuesAs(JsonObject::class.java)
        val users: MutableList<User> = ArrayList()
        for (userObject in userObjects) {
            val user = User(userObject.getJsonNumber("id").longValue(), userObject.getString("type"))
            user.login = userObject.getString("login")
            user.avatarUrl = userObject.getString("avatar_url")
            users.add(user)
        }
        return users
    }

    private fun graphql(query: String): JsonObject {
        val payload = Json.createObjectBuilder()
            .add("query", query)
            .add("variables", "{}")
            .build().toString()
        val request = requestFactory.buildPostRequest(
            GenericUrl(GRAPHQL_ENDPOINT),
            ByteArrayContent.fromString("application/json", payload))
        val headers = com.google.api.client.http.HttpHeaders()
        headers.authorization = "bearer $token"
        request.headers = headers
        val response = executeWithRetry(request)
        val responseObject = Json.createReader(StringReader(response.parseAsString())).readObject()
        if (responseObject.containsKey("errors")) {
            logger.debug("errors with query:\n$query")
            logger.debug("response:\n$responseObject")
        }
        return responseObject
    }

    private fun getPublicRepoNodes(userId: Long, isOrganization: Boolean): List<JsonObject> {
        var cursor: String? = null
        val nodes: MutableList<JsonObject> = ArrayList()
        while (true) {
            var after = ""
            if (cursor != null) {
                after = " after:\"$cursor\""
            }
            val query = """
                query {
                    node(id:"${encodeUserId(userId)}") {
                        ... on ${if (isOrganization) "Organization" else "User"} {
                            repositories(first:$PAGE_SIZE$after privacy:PUBLIC affiliations:OWNER) {
                                edges {
                                    cursor
                                    node {
                                        ... on Repository {
                                            id
                                            owner {
                                                id
                                            }
                                            name
                                            nameWithOwner
                                            description
                                            isFork
                                            homepageUrl
                                            stargazers {
                                                totalCount
                                            }
                                            primaryLanguage {
                                                name
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            val responseObject = graphql(query)
            handleUserNodeErrors(responseObject)
            val node = responseObject.getJsonObject("data").getJsonObject("node")
            if (!node.containsKey("repositories")) {
                throw NodeNotFoundException("$query\n$responseObject")
            }
            val edges = node.getJsonObject("repositories").getJsonArray("edges").getValuesAs(JsonObject::class.java)
            edges.forEach { edge ->
                nodes.add(edge.getJsonObject("node"))
            }
            if (edges.size == PAGE_SIZE) {
                cursor = edges[PAGE_SIZE - 1].getString("cursor")
                logger.debug("Paginate user_id: $userId with cursor: $cursor size: ${nodes.size}")
            } else {
                break
            }
        }
        return nodes
    }

    private fun encodeUserId(id: Long): String {
        val unencoded = "04:User$id"
        return Base64.getEncoder().encodeToString(unencoded.toByteArray())
    }

    private fun decodeUserId(encoded: String): Int { // TODO: Use Long
        val decoded = String(Base64.getDecoder().decode(encoded))
        return when {
            decoded.startsWith("04:User") -> {
                decoded.replaceFirst("04:User".toRegex(), "").toInt()
            }
            decoded.startsWith("012:Organization") -> {
                decoded.replaceFirst("012:Organization".toRegex(), "").toInt()
            }
            else -> {
                throw RuntimeException("'$decoded' does not have expected prefix for userId")
            }
        }
    }

    private fun decodeRepositoryId(encoded: String): Long {
        val decoded = String(Base64.getDecoder().decode(encoded))
        return if (decoded.startsWith("010:Repository")) {
            decoded.replaceFirst("010:Repository".toRegex(), "").toLong()
        } else {
            throw RuntimeException("'$decoded' does not have expected prefix for repositoryId")
        }
    }

    private fun decodeOrganizationId(encoded: String): Int { // TODO: Use Long
        val decoded = String(Base64.getDecoder().decode(encoded))
        return if (decoded.startsWith("012:Organization")) {
            decoded.replaceFirst("012:Organization".toRegex(), "").toInt()
        } else {
            throw RuntimeException("'$decoded' does not have expected prefix for organizationId")
        }
    }

    private fun handleUserNodeErrors(responseObject: JsonObject) {
        if (responseObject.containsKey("errors")) {
            val errors = responseObject.getJsonArray("errors").getValuesAs(JsonObject::class.java)
            errors.forEach { error -> // TODO: Log suppressed errors
                if (error.containsKey("type") && error.getString("type") == "NOT_FOUND" && error.containsKey("path")) {
                    for (path in error.getJsonArray("path").getValuesAs(JsonString::class.java)) {
                        if (path.string == "node") {
                            throw UserNotFoundException(error.getString("message"))
                        }
                    }
                }
            }
            throw GraphQLUnhandledException(responseObject.toString())
        }
    }

    private fun executeWithRetry(request: HttpRequest): HttpResponse {
        repeat(MAX_RETRY) { i ->
            try {
                return request.execute()
            } catch (e: HttpResponseException) {
                if (e.statusCode == 502) {
                    logger.debug("retrying 502: ${e.message}, i=$i")
                    TimeUnit.SECONDS.sleep((i * i).toLong()) // exponential retry
                } else {
                    throw e
                }
            } catch (e: SocketTimeoutException) {
                logger.debug("retrying socket timeout: ${e.message}, i=$i")
                TimeUnit.SECONDS.sleep((i * i).toLong()) // exponential retry
            }
        }
        return request.execute()
    }

    private inline fun <reified T> requestGet(path: String): T {
        return client.path(path)
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "bearer $token")
            .get(T::class.java)
    }
}
