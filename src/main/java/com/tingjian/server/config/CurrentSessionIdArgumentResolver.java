package com.tingjian.server.config;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.CurrentSessionId;
import com.tingjian.server.common.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentSessionIdArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentSessionId.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Object sessionId = webRequest.getAttribute(
                AccessTokenInterceptor.SESSION_ID_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (!(sessionId instanceof String value) || value.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return value;
    }
}
