package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DirectoryInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.LookupRequest
import com.reposilite.web.ReposiliteRoute
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.ReposiliteWebDsl
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

class MavenApiEndpoints(private val mavenFacade: MavenFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/details",
        methods = [HttpMethod.GET],
        summary = "Get list of available repositories",
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns list of available repositories",
                content = [OpenApiContent(from = DirectoryInfo::class)]
            )
        ]
    )
    private val findRepositories = ReposiliteRoute("/api/maven/details", GET) {
        accessed {
            response = mavenFacade.findFile(LookupRequest(null, "", this?.accessToken))
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/details/{repository}/*",
        methods = [HttpMethod.GET],
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns document (different for directory and file) that describes requested resource",
                content = [OpenApiContent(from = FileDetails::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 in case of unauthorized attempt of access to private repository",
                content = [OpenApiContent(from = ErrorResponse::class)]
            ),
            OpenApiResponse(
                status = "404",
                description = "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
            )
        ]
    )
    private val findFileDetails: suspend ReposiliteWebDsl.() -> Unit = {
        accessed {
            response = mavenFacade.findFile(LookupRequest(parameter("repository"), wildcard("gav"), this?.accessToken))
        }
    }

    private val findFileDetailsWithoutGav = ReposiliteRoute("/api/maven/details/{repository}", GET, handler = findFileDetails)
    private val findFileDetailsWithGav = ReposiliteRoute("/api/maven/details/{repository}/<gav>", GET, handler = findFileDetails)

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/versions/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
    )
    private val findVersions = ReposiliteRoute("/api/maven/versions/{repository}/<gav>", GET) {
        accessed {
            response = mavenFacade.findVersions(LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken))
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
    )
    private val findLatest = ReposiliteRoute("/api/maven/latest/{repository}/<gav>", GET) {
        accessed {
            response = mavenFacade.findLatest(LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken))
        }
    }

    override val routes = setOf(findRepositories, findFileDetailsWithoutGav, findFileDetailsWithGav, findVersions, findLatest)

}