package com.yzj.authentication.service;


import com.yzj.authentication.param.IdentityDto;
import com.yzj.authentication.param.TokenMsgDto;

/**
 * Created by wangshuai
 */
public interface JwtService {
    /**
     * 获取jwt
     * @param username
     * @param clientId
     * @return
     */
    String getJwt(String username, String clientId);
    /**
     * 根据传值刷新jwt
     * @param username
     * @param clientId
     * @param identityDto
     */
    void refreshJwt(String username, String clientId, IdentityDto identityDto);
    /**
     * 获取当前身份
     * @param username
     * @param clientId
     * @return
     */
    IdentityDto getIdentityDto(String username, String clientId);

    /**
     * 校验传入身份与当前身份是否匹配
     * @param jwt
     * @param identity
     * @return
     */
    TokenMsgDto checkIdentity(String jwt, String identity, String username, String clientId);
}
