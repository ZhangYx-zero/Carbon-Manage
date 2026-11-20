package com.carbon.manage.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 企业能源类型配置：一个企业可以配置电力、天然气中的一种或多种。
 */
@Data
public class EnterpriseEnergyType {

    private Long id;
    private Long enterpriseId;
    private String energyType;
    private LocalDateTime createTime;
}
