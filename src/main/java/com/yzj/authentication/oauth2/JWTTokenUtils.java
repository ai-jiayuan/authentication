package com.yzj.authentication.oauth2;


import com.yzj.authentication.param.CompanyDto;
import com.yzj.authentication.param.IdentityDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.yzj.authentication.utils.FormatUtil.*;

@Slf4j
@Component
public class JWTTokenUtils {
    //签名密钥
    @Value(value = "${system.security.authentication.jwt.base64Secret}")
    private String base64Secret;

    private Key key;
    //失效日期
    private long tokenValidityInMilliseconds;

    @Value(value = "${system.security.authentication.jwt.tokenValidity}")
    private int  tokenValidit;


    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityInMilliseconds =  TimeUnit.HOURS.toMillis(tokenValidit);
    }


    //创建Token
    public String createToken(String username,Long identityId,String identityName,Long companyId,Integer identityType,String clientId,
                              CompanyDto companyDto) {
        //存放过期时间
        Date validity = new Date(System.currentTimeMillis() + this.tokenValidityInMilliseconds);
        Integer companyType =null;
        String enterpriseName = null;
        String uniformSocialCreditCode = null;
        String parentCreditCode = null;
        if(Objects.nonNull(companyDto)){
            companyType =companyDto.getCompanyType();
            uniformSocialCreditCode = companyDto.getUniformSocialCreditCode();
            parentCreditCode = companyDto.getParentCreditCode();
            enterpriseName = companyDto.getEnterpriseName();
        }
        //创建Token令牌
        return Jwts.builder()
                .setSubject(username)
                .claim("identityId", identityId)
                .claim("identityName", identityName)
                .claim("clientId", clientId)
                .claim("companyId", companyId)
                .claim("identityType", identityType)
                .claim("enterpriseName", enterpriseName)
                .claim("companyType", companyType)
                .claim("uniformSocialCreditCode", uniformSocialCreditCode)
                .claim("parentCreditCode", parentCreditCode)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
    }

    //获取用户权限
    public IdentityDto getIdentityDto(String token) {
        log.debug("token", token);
        //解析Token的payload
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
        IdentityDto identityDto = new IdentityDto();
        identityDto.setId(getLong(claims.get("identityId")));
        identityDto.setIdentityName(getString(claims.get("identityName")));
        identityDto.setType(getInteger(claims.get("identityType")));
        identityDto.setCompanyId(getLong(claims.get("companyId")));
        return identityDto;
    }

}
