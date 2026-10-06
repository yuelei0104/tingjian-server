package com.tingjian.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AccessTokenInterceptor accessTokenInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
    private final CurrentSessionIdArgumentResolver currentSessionIdArgumentResolver;

    public WebMvcConfig(
            AccessTokenInterceptor accessTokenInterceptor,
            CurrentUserIdArgumentResolver currentUserIdArgumentResolver,
            CurrentSessionIdArgumentResolver currentSessionIdArgumentResolver) {
        this.accessTokenInterceptor = accessTokenInterceptor;
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
        this.currentSessionIdArgumentResolver = currentSessionIdArgumentResolver;
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
}
