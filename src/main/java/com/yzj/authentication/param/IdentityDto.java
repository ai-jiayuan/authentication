package com.yzj.authentication.param;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 *  身份
 *
 * @author wangshuai
 * @since 2019-09-25
 */
@Data
public class IdentityDto {

    @NotNull(message = "身份Id不能为空")
    private Long id;
    /**
     * 身份名称
     */
    private String identityName;
    /**
     * 身份类型
     */
    @NotNull(message = "身份类型不能为空")
    private Integer type;
    /**
     * 公司id
     */
    private Long companyId;
}
