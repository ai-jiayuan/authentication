package com.uniccc.authentication.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class Oauth2Interceptor extends HandlerInterceptorAdapter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String AUTHORIZATION_TOKEN = "access_token";

    private final static String JWT_TOKEN_PREFIX = "bearer ";

    private  final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler)  {
        String token = resolveToken(request);
        if (StringUtils.isEmpty(token)) {
            return false;
        }
        OAuth2Authentication oAuth2Authentication = tokenStore.readAuthentication(token);
        if (oAuth2Authentication == null) {
            return false;
        }
        SecurityContextHolder.getContext().setAuthentication(oAuth2Authentication);
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        //从HTTP头部获取TOKEN
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        //返回Token字符串，去除Bearer
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JWT_TOKEN_PREFIX)) {
            return bearerToken.substring(JWT_TOKEN_PREFIX.length());
        }
        //从请求参数中获取TOKEN
        String jwt = request.getParameter(AUTHORIZATION_TOKEN);
        if (StringUtils.hasText(jwt)) {
            return jwt;
        }
        return null;
    }
}
