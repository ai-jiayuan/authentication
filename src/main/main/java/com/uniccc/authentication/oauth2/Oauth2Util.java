package com.uniccc.authentication.oauth2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.redis.JdkSerializationStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

@Component
public class Oauth2Util {
    @Autowired
    private RedisConnectionFactory connectionFactory;

    public static Oauth2Util autowiredStatic;


    private static final String CLIENT_ID = "client_id";
    private static final String SCOPE = "scope";
    private static final String USERNAME = "username";

    private static final String ACCESS = "access:";
    private static final String AUTH_TO_ACCESS = "auth_to_access:";
    private static final String AUTH = "auth:";
    private static final String REFRESH_AUTH = "refresh_auth:";
    private static final String ACCESS_TO_REFRESH = "access_to_refresh:";
    private static final String REFRESH = "refresh:";
    private static final String UNAME_TO_REFRESH = "uname_to_refresh:";
    private static final String REFRESH_TO_ACCESS = "refresh_to_access:";
    private static final String CLIENT_ID_TO_ACCESS = "client_id_to_access:";
    private static final String UNAME_TO_ACCESS = "uname_to_access:";

    @PostConstruct
    public void init() {
        autowiredStatic = this;
    }

    private static JdkSerializationStrategy serializationStrategy = new JdkSerializationStrategy();


    private static String extractKey(OAuth2Authentication authentication) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        OAuth2Request authorizationRequest = authentication.getOAuth2Request();
        if (!authentication.isClientOnly()) {
            values.put(USERNAME, authentication.getName());
        }
        values.put(CLIENT_ID, authorizationRequest.getClientId());
        if (authorizationRequest.getScope() != null) {
            values.put(SCOPE, OAuth2Utils.formatParameterList(new TreeSet<String>(authorizationRequest.getScope())));
        }
        return generateKey(values);
    }

    private static String generateKey(Map<String, String> values) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(values.toString().getBytes("UTF-8"));
            return String.format("%032x", new BigInteger(1, bytes));
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).", nsae);
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).", uee);
        }
    }

    /**
     * 清理指定账号的token，让其token强行删除，无法访问。
     *
     * @param name
     * @param clientId
     */
    public static void removeTokenAccess(String name, String clientId) {
        byte[] refresUnameKey = serializeKey(UNAME_TO_REFRESH + getApprovalKey(clientId,name)+"*");
        RedisConnection conn = autowiredStatic.connectionFactory.getConnection();
        try {
            Set<byte[]> keys = conn.keys(refresUnameKey);
            if(!CollectionUtils.isEmpty(keys)) {
                for (byte[] key : keys) {
                    byte[] refreshBytes = conn.get(key);
                    OAuth2RefreshToken refreshToken = serializationStrategy.deserialize(refreshBytes, OAuth2RefreshToken.class);
                    if(Objects.isNull(refreshToken)){
                        break;
                    }
                    byte[] refreshToAccessKey = serializeKey(REFRESH_TO_ACCESS + refreshToken.getValue());
                    byte[] accessBytes = conn.get(refreshToAccessKey);
                    if(Objects.nonNull(accessBytes)){
                        String tokenValue = deserializeString(accessBytes);
                        removeAccessToken(tokenValue,conn);
                    }
                    removeRefreshToken(refreshToken.getValue(),conn,key);
                }
            }
        } finally {
            conn.close();
        }
    }

    public static Set<String> getOnlineUser(String username){
        RedisConnection conn = autowiredStatic.connectionFactory.getConnection();
        byte[] unameKey = serializeKey(UNAME_TO_ACCESS +"*"+username);
        Set<byte[]> keys = conn.keys(unameKey);
        if(!CollectionUtils.isEmpty(keys)) {
            return keys.stream().map(String::new).map(string -> {
                Pattern p = Pattern.compile("uname_to_access:(?<clientId>.*?):"+username);
                Matcher m = p.matcher(string);
                if(m.find()) {
                    return m.group("clientId");
                }else {
                    return null;
                }
            }).filter(StringUtils::isNotBlank).collect(toSet());
        }else {
            return new HashSet<>();
        }
    }

    public static Set<String> getCacheUser(String username){
        RedisConnection conn = autowiredStatic.connectionFactory.getConnection();
        byte[] refresUnameKey = serializeKey(UNAME_TO_REFRESH +"*"+username+"*");
        Set<byte[]> keys = conn.keys(refresUnameKey);
        if(!CollectionUtils.isEmpty(keys)) {
            return keys.stream().map(String::new).map(string -> {
                Pattern p = Pattern.compile("uname_to_refresh:(?<clientId>.*?):"+username+"(?<value>.*?)");
                Matcher m = p.matcher(string);
                if(m.find()) {
                    return m.group("clientId");
                }else {
                    return null;
                }
            }).filter(StringUtils::isNotBlank).collect(toSet());
        }else {
            return new HashSet<>();
        }
    }

    public static void removeAccessToken(String tokenValue,RedisConnection conn) {
        byte[] accessKey = serializeKey(ACCESS + tokenValue);
        byte[] authKey = serializeKey(AUTH + tokenValue);
        byte[] accessToRefreshKey = serializeKey(ACCESS_TO_REFRESH + tokenValue);
        try {
            conn.openPipeline();
            conn.get(accessKey);
            conn.get(authKey);
            conn.del(accessKey);
            conn.del(accessToRefreshKey);
            // Don't remove the refresh token - it's up to the caller to do that
            conn.del(authKey);
            List<Object> results = conn.closePipeline();
            byte[] access = (byte[]) results.get(0);
            byte[] auth = (byte[]) results.get(1);

            OAuth2Authentication authentication = serializationStrategy.deserialize(auth, OAuth2Authentication.class);
            if (authentication != null) {
                String key = extractKey(authentication);
                byte[] authToAccessKey = serializeKey(AUTH_TO_ACCESS + key);
                byte[] unameKey = serializeKey(UNAME_TO_ACCESS + getApprovalKey(authentication));
                byte[] clientId = serializeKey(CLIENT_ID_TO_ACCESS + authentication.getOAuth2Request().getClientId());
                conn.openPipeline();
                conn.del(authToAccessKey);
                conn.sRem(unameKey, access);
                conn.sRem(clientId, access);
                conn.del(serialize(ACCESS + key));
                conn.closePipeline();
            }
        } finally {
            conn.close();
        }
    }

    public static void removeRefreshToken(String tokenValue,RedisConnection conn,byte[] key) {
        byte[] refreshKey = serializeKey(REFRESH + tokenValue);
        byte[] refreshAuthKey = serializeKey(REFRESH_AUTH + tokenValue);
        byte[] refresh2AccessKey = serializeKey(REFRESH_TO_ACCESS + tokenValue);
        byte[] access2RefreshKey = serializeKey(ACCESS_TO_REFRESH + tokenValue);
        try {
            conn.openPipeline();
            conn.del(key);
            conn.del(refreshKey);
            conn.del(refreshAuthKey);
            conn.del(refresh2AccessKey);
            conn.del(access2RefreshKey);
            conn.closePipeline();
        } finally {
            conn.close();
        }
    }

    private static String getApprovalKey(String clientId, String userName) {
        return clientId + (userName == null ? "" : ":" + userName);
    }

    private static String getApprovalKey(OAuth2Authentication authentication) {
        String userName = authentication.getUserAuthentication() == null ? ""
                : authentication.getUserAuthentication().getName();
        return getApprovalKey(authentication.getOAuth2Request().getClientId(), userName);
    }

    private static byte[] serializeKey(String string) {
        return serializationStrategy.serialize(string);
    }

    private static byte[] serialize(String string) {
        return serializationStrategy.serialize(string);
    }

    private static String deserializeString(byte[] bytes) {
        return serializationStrategy.deserializeString(bytes);
    }
}