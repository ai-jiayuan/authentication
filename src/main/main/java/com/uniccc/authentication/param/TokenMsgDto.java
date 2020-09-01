package com.uniccc.authentication.param;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenMsgDto {
    /**
     * token
     */
    private String token;
    /**
     * 页面身份
     */
    private String pageIdentity;
    /**
     * 系统身份
     */
    private String systemIdentity;
    /**
     * 用户名
     */
    private String username;
    /**
     * 用户设备类型
     */
    private String clientId;
    /**
     * 错误原因
     */
    private String errorMsg;
}
