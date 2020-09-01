package com.uniccc.authentication.service.impl;

import com.google.common.collect.Lists;
import com.uniccc.authentication.param.IdentityDto;
import com.uniccc.authentication.provider.OrganizationProvider;
import com.uniccc.authentication.service.AuthenticationService;
import com.uniccc.common.core.enums.ResultCode;
import com.uniccc.common.core.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class AuthenticationImpl implements AuthenticationService {

    @Autowired
    private OrganizationProvider organizationProvider;

    @Override
    public String getCurrentUserName() {
        return Optional.ofNullable(getJwtUser())
                .map(User::getUsername)
                .orElse(null);
    }

    @Override
    public String getClientId() {
        return Optional.ofNullable(getOAuth2Authentication())
                .map(OAuth2Authentication::getOAuth2Request)
                .map(OAuth2Request::getClientId)
                .orElse(null);
    }

    @Override
    public IdentityDto getIdentityDto() {
        return Optional.ofNullable(getCurrentUserName())
                .map(user->organizationProvider.getIdentityDto(user,getClientId()))
                .filter(HttpEntity::hasBody)
                .map(HttpEntity::getBody)
                .orElseThrow(()->new BaseException(ResultCode.ORGAN_GATEWAY_ERROR));
    }

    private Authentication getAuthentication(){
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .orElse(null);
    }

    private User getJwtUser(){
        return Optional.ofNullable(getAuthentication())
                .map(Authentication::getPrincipal)
                .map(object->  {
                    if(object instanceof User){
                        return (User)object;
                    }else if(object instanceof String){
                        return new User(String.valueOf(object),"", Lists.newArrayList());
                    }else {
                        log.error("用户读取失败,token有问题");
                        return null;
                    }
                })
                .orElse(null);
    }

    private OAuth2Authentication getOAuth2Authentication(){
        return Optional.ofNullable(getAuthentication())
                .map(authentication -> {
                    try{
                        return (OAuth2Authentication)authentication;
                    }catch (Exception e){
                        log.error("用户读取失败,token有问题");
                        return null;
                    }})
                .orElse(null);
    }
}
