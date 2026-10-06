package com.tingjian.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AccessTokenInterceptor accessTokenInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
    private final CurrentSessionIdArgumentResolver currentSessionIdArgumentResolver;
    private final CorsProperties corsProperties;

    public WebMvcConfig(
            AccessTokenInterceptor accessTokenInterceptor,
            CurrentUserIdArgumentResolver currentUserIdArgumentResolver,
            CurrentSessionIdArgumentResolver currentSessionIdArgumentResolver,
            CorsProperties corsProperties) {
        this.accessTokenInterceptor = accessTokenInterceptor;
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
        this.currentSessionIdArgumentResolver = currentSessionIdArgumentResolver;
        this.corsProperties = corsProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessTokenInterceptor).addPathPatterns("/api/v1/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
        resolvers.add(currentSessionIdArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(corsProperties.allowedOriginPatterns())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Request-Id",
                        "X-Device-Id", "X-Device-Name", "X-Platform", "X-App-Version")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
