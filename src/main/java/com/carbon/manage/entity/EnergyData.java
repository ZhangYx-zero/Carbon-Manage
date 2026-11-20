package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EnergyData {

    private Long id;
    private Long enterpriseId;
    private Long factorId;
    private String energyType;
    private BigDecimal energyValue;
    private BigDecimal emissionValue;
    private BigDecimal thresholdValue;
    private Integer isExceeded;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 关联查询展示字段 */
    private String enterpriseName;
    private String workshopName;
}
