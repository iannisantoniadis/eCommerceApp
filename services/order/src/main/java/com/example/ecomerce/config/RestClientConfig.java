package com.example.ecomerce.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {


    //Ramane comentat deocamdata
//    @Bean
//    public RestClient restClient(RestClient.Builder builder, ObservationRegistry observationRegistry){
//        return builder
//                .observationRegistry(observationRegistry)
//                .build();
//    }

    @Bean
    public RestClient restClient(RestClient.Builder builder, ObservationRegistry observationRegistry){
        return builder
                .observationRegistry(observationRegistry)
                .requestInterceptor((request, body, execution) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication instanceof JwtAuthenticationToken jwt){
                        request.getHeaders().setBearerAuth(jwt.getToken().getTokenValue());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
