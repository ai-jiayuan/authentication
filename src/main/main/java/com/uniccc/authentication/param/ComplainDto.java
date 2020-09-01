package com.uniccc.authentication.param;

import lombok.Data;

/**
 * 封号解封
 */
@Data
public class ComplainDto  {
    /**
     * 用户名/手机号
     */
    private String  username;

    private Boolean isDelete;

}
