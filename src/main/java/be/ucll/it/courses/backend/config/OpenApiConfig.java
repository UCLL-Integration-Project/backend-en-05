package be.ucll.it.courses.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fireDetectionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fire Detection System API")
                        .description("REST API for the Fire Detection and Robot Control System")
                        .version("v1.0.0")
                        .license(new Info().getLicense()));
    }
}
