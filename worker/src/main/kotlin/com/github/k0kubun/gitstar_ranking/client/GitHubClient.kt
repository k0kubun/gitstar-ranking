package com.github.k0kubun.gitstar_ranking.client

import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.User
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import io.sentry.Sentry
import java.io.IOException
import java.io.StringReader
import java.lang.ClassCastException
import java.lang.InterruptedException
import java.lang.RuntimeException
import java.net.SocketTimeoutException
import java.util.ArrayList
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonString
import kotlin.Throws
import org.slf4j.LoggerFactory

class GitHubClient(private val token: String) {
    private val requestFactory: HttpRequestFactory = NetHttpTransport().createRequestFactory()

    fun getLogin(userId: Long): String {
        val responseObject = graphql(
            """query {
node(id:"${encodeUserId(userId)}") {
  ... on User {
    login
  }
}}"""
        )
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
        for (node in getPublicRepoNodes(userId, isOrganization)) {
            try {
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
            } catch (e: ClassCastException) {
                Sentry.capture(e)
                LOG.debug("node: $node")
                throw e
            }
        }
        return repos
    }

    // TODO: handle error object
    val rateLimitRemaining: Int
        get() = try {
            val responseObject = graphql("query { rateLimit { remaining } }")
            // TODO: handle error object
            responseObject.getJsonObject("data").getJsonObject("rateLimit").getInt("remaining")
        } catch (e: IOException) {
            Sentry.capture(e)
            0
        }

    fun getUserWithLogin(login: String): User {
        val request = requestFactory.buildGetRequest(GenericUrl(API_ENDPOINT + "/users/" + login))
        val headers = HttpHeaders()
        headers.authorization = "bearer $token"
        request.headers = headers
        val response = executeWithRetry(request)
        // TODO: Handle error status code
        val userObject = Json.createReader(StringReader(response.parseAsString())).readObject()
        val user = User(java.lang.Long.valueOf(userObject.getInt("id").toLong()), userObject.getString("type"))
        user.login = userObject.getString("login")
        user.avatarUrl = userObject.getString("avatar_url")
        return user
    }

    fun getUsersSince(since: Long): List<User> {
        val request = requestFactory.buildGetRequest(GenericUrl(API_ENDPOINT + "/users?per_page=100&since=" + since))
        val headers = HttpHeaders()
        headers.authorization = "bearer $token"
        request.headers = headers
        val response = executeWithRetry(request)
        // TODO: Handle error status code
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

    @Throws(IOException::class)
    private fun graphql(query: String): JsonObject {
        val payload = Json.createObjectBuilder()
            .add("query", query)
            .add("variables", "{}")
            .build().toString()
        val request = requestFactory.buildPostRequest(
            GenericUrl(GRAPHQL_ENDPOINT),
            ByteArrayContent.fromString("application/json", payload))
        val headers = HttpHeaders()
        headers.authorization = "bearer $token"
        request.headers = headers
        val response = executeWithRetry(request)
        // TODO: Handle error status code
        val responseObject = Json.createReader(StringReader(response.parseAsString())).readObject()
        if (responseObject.containsKey("errors")) {
            LOG.debug("errors with query:\n$query")
            LOG.debug("response:\n$responseObject")
        }
        return responseObject
    }

    @Throws(IOException::class)
    private fun getPublicRepoNodes(userId: Long, isOrganization: Boolean): List<JsonObject> {
        var cursor: String? = null
        val nodes: MutableList<JsonObject> = ArrayList()
        while (true) {
            var after = ""
            if (cursor != null) {
                after = " after:\"$cursor\""
            }
            val query = """query {
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
}}"""
            val responseObject = graphql(query)
            handleUserNodeErrors(responseObject)
            val node = responseObject.getJsonObject("data").getJsonObject("node")
            if (!node.containsKey("repositories")) {
                throw NodeNotFoundException("""
    $query
    $responseObject
    """.trimIndent())
            }
            val edges = node.getJsonObject("repositories").getJsonArray("edges").getValuesAs(JsonObject::class.java)
            for (edge in edges) {
                nodes.add(edge.getJsonObject("node"))
            }
            if (edges.size == PAGE_SIZE) {
                cursor = edges[PAGE_SIZE - 1].getString("cursor")
                LOG.debug("Paginate user_id: " + userId.toString() + " with cursor: " + cursor + " size: " + nodes.size)
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

    // TODO: Use Long
    private fun decodeUserId(encoded: String): Int {
        val decoded = String(Base64.getDecoder().decode(encoded))
        return if (decoded.startsWith("04:User")) {
            Integer.valueOf(decoded.replaceFirst("04:User".toRegex(), ""))
        } else if (decoded.startsWith("012:Organization")) {
            Integer.valueOf(decoded.replaceFirst("012:Organization".toRegex(), ""))
        } else {
            throw RuntimeException(String.format("'%s' does not have expected prefix for userId", decoded))
        }
    }

    private fun decodeRepositoryId(encoded: String): Long {
        val decoded = String(Base64.getDecoder().decode(encoded))
        return if (decoded.startsWith("010:Repository")) {
            java.lang.Long.valueOf(decoded.replaceFirst("010:Repository".toRegex(), ""))
        } else {
            throw RuntimeException(String.format("'%s' does not have expected prefix for repositoryId", decoded))
        }
    }

    // TODO: Use Long
    private fun decodeOrganizationId(encoded: String): Int {
        val decoded = String(Base64.getDecoder().decode(encoded))
        return if (decoded.startsWith("012:Organization")) {
            Integer.valueOf(decoded.replaceFirst("012:Organization".toRegex(), ""))
        } else {
            throw RuntimeException(String.format("'%s' does not have expected prefix for organizationId", decoded))
        }
    }

    private fun handleUserNodeErrors(responseObject: JsonObject) {
        if (responseObject.containsKey("errors")) {
            val errors = responseObject.getJsonArray("errors").getValuesAs(JsonObject::class.java)
            for (error in errors) { // TODO: Log suppressed errors
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
        for (i in 0 until MAX_RETRY) {
            try {
                return request.execute()
            } catch (e: HttpResponseException) {
                if (e.statusCode == 502) {
                    LOG.debug("retrying 502: " + e.message + ", i=" + i)
                    try {
                        TimeUnit.SECONDS.sleep((i * i).toLong()) // exponential retry
                    } catch (ie: InterruptedException) {
                        Sentry.capture(ie)
                    }
                } else {
                    throw e
                }
            } catch (e: SocketTimeoutException) {
                LOG.debug("retrying socket timeout: " + e.message + ", i=" + i)
                try {
                    TimeUnit.SECONDS.sleep((i * i).toLong()) // exponential retry
                } catch (ie: InterruptedException) {
                    Sentry.capture(ie)
                }
            }
        }
        return request.execute()
    }

    inner class NodeNotFoundException(message: String?) : RuntimeException(message)
    inner class UserNotFoundException(message: String?) : RuntimeException(message)
    inner class GraphQLUnhandledException(message: String?) : RuntimeException(message)
    companion object {
        private const val MAX_RETRY = 3
        private const val PAGE_SIZE = 100
        private val LOG = LoggerFactory.getLogger(GitHubClient::class.java)
        private const val API_ENDPOINT = "https://api.github.com"
        private const val GRAPHQL_ENDPOINT = API_ENDPOINT + "/graphql"
    }
}
