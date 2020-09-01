package com.uniccc.authentication.service;


import com.uniccc.authentication.param.IdentityDto;

/**
 * Created by wangshuai
 */
public interface AuthenticationService {

    /**
     * 获取当前用户名称
     */
    String getCurrentUserName();
    /**
     * 获取当前请求的服务Id
     */
    String getClientId();
    /**
     * 获取当前身份
     */
    IdentityDto getIdentityDto();


}
