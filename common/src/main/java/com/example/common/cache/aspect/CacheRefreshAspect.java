package com.example.common.cache.aspect;

import com.example.common.cache.annotation.CacheRefreshable;
import com.example.common.cache.security.CacheRefreshAuthorizer;
import com.example.common.cache.service.CacheRefreshService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Intercepts only methods explicitly annotated with @CacheRefreshable.
 * Normal requests retain their existing cache behavior.
 */
@Aspect
@Component
public class CacheRefreshAspect {

    private static final String REFRESH_PARAMETER = "cache_refresh";
    private static final String REFRESH_TOKEN_HEADER = "X-Cache-Refresh-Token";

    private final CacheRefreshService cacheRefreshService;
    private final CacheRefreshAuthorizer cacheRefreshAuthorizer;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public CacheRefreshAspect(
            CacheRefreshService cacheRefreshService,
            CacheRefreshAuthorizer cacheRefreshAuthorizer) {
        this.cacheRefreshService = Objects.requireNonNull(cacheRefreshService);
        this.cacheRefreshAuthorizer = Objects.requireNonNull(cacheRefreshAuthorizer);
    }

    @Around("@annotation(cacheRefreshable)")
    public Object refreshCacheBeforeInvocation(
            ProceedingJoinPoint joinPoint,
            CacheRefreshable cacheRefreshable) throws Throwable {

        HttpServletRequest request = getCurrentRequest();

        if (!isRefreshRequested(request)) {
            return joinPoint.proceed();
        }

        cacheRefreshAuthorizer.authorize(request.getHeader(REFRESH_TOKEN_HEADER));

        Object key = resolveCacheKey(joinPoint, cacheRefreshable);

        cacheRefreshService.evict(Arrays.asList(cacheRefreshable.cacheNames()), key);

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cache refresh requires an active HTTP request"
            );
        }

        return servletAttributes.getRequest();
    }

    private boolean isRefreshRequested(HttpServletRequest request) {
        String value = request.getParameter(REFRESH_PARAMETER);
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private Object resolveCacheKey(
            ProceedingJoinPoint joinPoint,
            CacheRefreshable cacheRefreshable) {

        String keyExpression = cacheRefreshable.keyExpression();

        if (keyExpression == null || keyExpression.isBlank()) {
            return SimpleKey.EMPTY;
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] arguments = joinPoint.getArgs();
        EvaluationContext context = new StandardEvaluationContext();

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        for (int index = 0; index < arguments.length; index++) {
            context.setVariable("arg" + index, arguments[index]);

            if (parameterNames != null && index < parameterNames.length) {
                context.setVariable(parameterNames[index], arguments[index]);
            }
        }

        Expression expression = expressionParser.parseExpression(keyExpression);
        Object key = expression.getValue(context);

        if (key == null) {
            throw new IllegalArgumentException(
                    "Cache refresh key expression evaluated to null: " + keyExpression
            );
        }

        return key;
    }
}