package com.tingjian.server.config;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.common.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Object userId = webRequest.getAttribute(
                AccessTokenInterceptor.USER_ID_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (!(userId instanceof String value) || value.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return value;
    }
}
