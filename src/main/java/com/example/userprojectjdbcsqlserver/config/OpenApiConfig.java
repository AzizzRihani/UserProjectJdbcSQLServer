package com.example.userprojectjdbcsqlserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Configuration
public class OpenApiConfig {

    private static final String COOKIE_SCHEME = "sessionCookie";
    private static final String LOGOUT_PATH = "/auth/logout";

    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestion d'utilisateurs API")
                        .version("1.0")
                        .description("API REST pour la gestion des utilisateurs, de leurs roles et permissions, authentification par cookie de session"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_SCHEME, new SecurityScheme()
                                .name("SESSION")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)));
    }

    /**
     * /auth/logout est gere par le LogoutFilter natif de Spring Security (SecurityConfig), pas par
     * une methode de controleur : springdoc ne peut donc pas le decouvrir automatiquement. On l'ajoute
     * ici manuellement, purement pour la documentation/Swagger UI - aucun impact sur le comportement
     * reel de l'endpoint (toujours traite par LogoutFilter).
     *
     * Toute requete mutante (POST/PUT/DELETE/PATCH), logout inclus, exige le header X-XSRF-TOKEN
     * (protection CSRF, cf. SecurityConfig). Swagger UI ne rejoue pas automatiquement ce header : sans
     * ce parametre declare explicitement, "Try it out" echoue systematiquement en 403 sur ces endpoints.
     * Pour l'obtenir : appeler n'importe quel GET (ex. /auth/me) et lire le header X-CSRF-TOKEN de sa
     * reponse dans le panneau "Response headers" de Swagger UI, puis le coller ici.
     */
    @Bean
    public GlobalOpenApiCustomizer authDocsCustomizer() {
        return openApi -> {
            addLogoutPath(openApi);
            addCsrfHeaderToMutatingOperations(openApi);
        };
    }

    private static void addLogoutPath(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        if (openApi.getPaths().containsKey(LOGOUT_PATH)) {
            return;
        }
        Operation logoutOperation = new Operation()
                .tags(List.of("Authentification"))
                .summary("Deconnexion : invalide la session courante et efface les cookies")
                .description("Gere par le LogoutFilter natif de Spring Security (SecurityConfig), pas par un controleur Java - documente ici manuellement pour apparaitre dans Swagger UI.")
                .responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse().description("Deconnexion reussie")));
        openApi.getPaths().addPathItem(LOGOUT_PATH, new PathItem().post(logoutOperation));
    }

    private static void addCsrfHeaderToMutatingOperations(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().forEach(pathItem -> Stream
                .of(pathItem.getPost(), pathItem.getPut(), pathItem.getDelete(), pathItem.getPatch())
                .filter(Objects::nonNull)
                .forEach(OpenApiConfig::addCsrfHeader));
    }

    private static void addCsrfHeader(Operation operation) {
        operation.addParametersItem(new Parameter()
                .in("header")
                .name("X-XSRF-TOKEN")
                .description("Token CSRF, recupere depuis le header de reponse X-CSRF-TOKEN d'un appel GET prealable (ex. /auth/me)")
                .required(true)
                .schema(new StringSchema()));
    }
}
