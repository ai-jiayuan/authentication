package com.yzj.authentication.service.impl;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheGetResult;
import com.alicp.jetcache.anno.CreateCache;
import com.google.common.collect.Lists;
import com.yzj.authentication.config.StreamClient;
import com.yzj.authentication.oauth2.JWTTokenUtils;
import com.yzj.authentication.oauth2.Oauth2Util;
import com.yzj.authentication.param.ComplainDto;
import com.yzj.authentication.param.IdentityDto;
import com.yzj.authentication.param.TokenMsgDto;
import com.yzj.authentication.provider.OrganizationProvider;
import com.yzj.authentication.service.AuthenticationService;
import com.yzj.authentication.service.JwtService;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@EnableBinding(StreamClient.class)
public class JwtServiceImpl implements JwtService {

    private static final String JWTTOKEN = "jwt_token_";

    private static final List<String> SPECIAL_TOKEN = Lists.newArrayList("banUser","refresh");

    @CreateCache(name = JWTTOKEN, expire = 8,timeUnit = TimeUnit.HOURS)
    private Cache<String, String> jwtTokenCache;

    @CreateCache(name = "refresh_", expire = 1,timeUnit = TimeUnit.DAYS)
    private Cache<String, String> refresh;

    @CreateCache(name = "banUser_", expire = 1,timeUnit = TimeUnit.DAYS)
    private Cache<String, String> banUser;

    @Autowired
    private JWTTokenUtils jwtTokenUtils;

    @Autowired
    private OrganizationProvider organizationProvider;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private StatefulRedisConnection<String, String> redisConnection;


    @Override
    public String getJwt(String username,String clientId) {
        String key = getKey(username,clientId);
        if (banUser.GET(key).isSuccess()) {
            banUser.remove(key);
            if (refresh.GET(key).isSuccess()) {
                refresh.remove(key);
            }
            CompletableFuture.runAsync(() ->
                    Oauth2Util.removeTokenAccess(username,clientId));
            return "banUser";
        }
        if (refresh.GET(key).isSuccess()) {
            refresh.remove(key);
            return "refresh";
        }
        CacheGetResult<String> jwtTokenState = jwtTokenCache.GET(key);
        if(jwtTokenState.isSuccess()){
            return jwtTokenState.getValue();
        }else {
            IdentityDto identityDto = authenticationService.getIdentityDto();
            String jwtTokenStr = jwtTokenUtils.createToken(username,identityDto.getId(),identityDto.getIdentityName(),identityDto.getCompanyId(),identityDto.getType(),clientId, Optional.ofNullable(identityDto.getCompanyId())
                    .map(organizationProvider::getCompanyDto)
                    .filter(HttpEntity::hasBody)
                    .map(HttpEntity::getBody)
                    .orElse(null));
            jwtTokenCache.PUT(key,jwtTokenStr).isSuccess();
            return jwtTokenStr;
        }
    }

    @Override
    public void refreshJwt(String username,String clientId, IdentityDto identityDto) {
        jwtTokenCache.PUT(getKey(username,clientId),jwtTokenUtils.createToken(username,identityDto.getId(),identityDto.getIdentityName(),identityDto.getCompanyId(),identityDto.getType(),
                clientId, Optional.ofNullable(identityDto.getCompanyId())
                        .map(organizationProvider::getCompanyDto)
                        .filter(HttpEntity::hasBody)
                        .map(HttpEntity::getBody)
                        .orElse(null))).isSuccess();
    }

    @Override
    public IdentityDto getIdentityDto(String username, String clientId) {
        CacheGetResult<String> jwtTokenState = jwtTokenCache.GET(getKey(username,clientId));
        if(jwtTokenState.isSuccess()){
            return jwtTokenUtils.getIdentityDto(jwtTokenState.getValue());
        }else {
            return authenticationService.getIdentityDto();
        }
    }

    @Override
    public TokenMsgDto checkIdentity(String jwt, String identity,String username, String clientId) {
        if(SPECIAL_TOKEN.contains(jwt)){
            return TokenMsgDto.builder()
                    .token(jwt)
                    .username(username)
                    .clientId(clientId)
                    .pageIdentity(identity)
                    .errorMsg("系统操作")
                    .build();
        }
        if(StringUtils.isEmpty(identity)){
            return TokenMsgDto.builder()
                    .token(jwt)
                    .username(username)
                    .clientId(clientId)
                    .build();
        }
        IdentityDto identityDto = jwtTokenUtils.getIdentityDto(jwt);
        if(identity.equals(identityDto.getIdentityName())){
            return TokenMsgDto.builder()
                    .token(jwt)
                    .username(username)
                    .clientId(clientId)
                    .pageIdentity(identity)
                    .systemIdentity(identityDto.getIdentityName())
                    .build();
        }else {
            return TokenMsgDto.builder()
                    .token("refresh")
                    .username(username)
                    .clientId(clientId)
                    .pageIdentity(identity)
                    .systemIdentity(identityDto.getIdentityName())
                    .errorMsg("前端身份与系统身份不相同,进行刷新页面操作")
                    .build();
        }
    }

    @StreamListener(StreamClient.JWT)
    public void clearJwt(String username) {
        if(StringUtils.isEmpty(username)){
            return;
        }
        log.info("给用户" + username + "刷新身份,清楚jwt缓存");
        RedisAsyncCommands<String, String> redisCommands = redisConnection.async();
        redisCommands.keys(JWTTOKEN + username + "|*")
                .thenAcceptAsync(keys -> {
                    if (!CollectionUtils.isEmpty(keys)) {
                        redisCommands.del(keys.toArray(new String[0]));
                    }
                });
        Oauth2Util.getCacheUser(username)
                .forEach(clientId-> refresh.PUT(getKey(username,clientId),"1").isSuccess());
    }

    @StreamListener(StreamClient.BAN)
    public void banUser(ComplainDto complainDto) {
        if(Objects.isNull(complainDto)){
            return;
        }
        if(complainDto.getIsDelete()) {
            log.info("将用户" + complainDto.getUsername() + "下线");
            Oauth2Util.getCacheUser(complainDto.getUsername())
                    .forEach(clientId -> banUser.PUT(getKey(complainDto.getUsername(), clientId), "1").isSuccess());
        }else {
            log.info("恢复用户" + complainDto.getUsername());
            RedisAsyncCommands<String, String> redisCommands = redisConnection.async();
            redisCommands.keys("banUser_" + complainDto.getUsername() + "|*")
                    .thenAcceptAsync(keys -> {
                        if (!CollectionUtils.isEmpty(keys)) {
                            redisCommands.del(keys.toArray(new String[0]));
                        }
                    });
        }
    }


    private String getKey(String username, String clientId){
        return username+"|"+clientId;
    }
}
