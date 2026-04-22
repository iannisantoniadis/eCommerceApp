package com.example.ecomerce;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(title = "Customer Service API", version = "1.0"),
		servers = @Server(url = "${app.gateway-url}", description = "API Gateway") )
@SecurityScheme(
		name = "Keycloak-JWT",
		type = SecuritySchemeType.OAUTH2,
//		type = SecuritySchemeType.HTTP,
//		bearerFormat = "JWT",
//		scheme = "bearer"
		flows = @OAuthFlows(
				authorizationCode = @OAuthFlow(
						authorizationUrl = "${app.keycloak.authorization_url}",
						tokenUrl = "${app.keycloak.token_url}",
						scopes = {
								@OAuthScope(name = "openid", description = "openid scope"),
								@OAuthScope(name = "profile", description = "profile scope")
						}
				)
		)
)
public class CustomerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerApplication.class, args);
	}

}
