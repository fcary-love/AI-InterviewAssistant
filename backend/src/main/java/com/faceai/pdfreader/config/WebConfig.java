package com.faceai.pdfreader.config;

import com.faceai.pdfreader.auth.AuthInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;
    private final AuthInterceptor authInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public WebConfig(StorageProperties storageProperties, AuthInterceptor authInterceptor) {
        this.storageProperties = storageProperties;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rootPath = Paths.get(storageProperties.rootDir()).toAbsolutePath().normalize();
        String rootLocation = rootPath.toUri().toString();
        if (!rootLocation.endsWith("/")) {
            rootLocation = rootLocation + "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations(rootLocation);
    }
}
