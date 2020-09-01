package com.yzj.authentication.param;

import lombok.Data;

@Data
public class CompanyDto {
    /**
     * 企业id
     */
    private Long companyId;
    /**
     * 企业名
     */
    private String enterpriseName;
    /**
     * 社会统一信用代码
     */
    private String uniformSocialCreditCode;
    /**
     * 父公司信用代码
     */
    private String parentCreditCode;
    /**
     * 公司类型
     */
    private Integer companyType;
    /**
     * 逻辑删除
     */
    private Boolean isDelete;
}
