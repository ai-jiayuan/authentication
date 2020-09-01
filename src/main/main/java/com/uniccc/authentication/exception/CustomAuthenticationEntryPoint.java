package com.uniccc.authentication.exception;

import com.alibaba.fastjson.JSON;
import com.uniccc.common.core.entity.vo.ResultDto;
import com.uniccc.common.core.enums.ResultCode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

import static com.uniccc.common.core.enums.ResultCode.*;


@Component("customEntryPoint")
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final String BEARER = "bearer ";

    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String accessToken = request.getHeader("Authorization");
        response.getWriter().write(JSON.toJSONString(ResultDto.failure(isTokenValid(accessToken))));
    }

    public ResultCode isTokenValid(String accessToken) {
        RedisConnection conn = this.redisConnectionFactory.getConnection();
        int length = BEARER.length();
        if(StringUtils.isBlank(accessToken)||accessToken.length()<=length){
            return MALICIOUS_VISIT;
        }
        String token = accessToken.substring(length);
        byte[] dAccess = null;
        try {
            dAccess = conn.get(STRING_SERIALIZER.serialize("discard_to_access:" + token));
        } finally {
            conn.close();
        }

        if (Objects.nonNull(dAccess) && StringUtils.isNotBlank(new String(dAccess))) {
            return DUPLICATE_LOGON;
        } else {
            return ACCESS_TOKEN_INVALID;
        }
    }
}
