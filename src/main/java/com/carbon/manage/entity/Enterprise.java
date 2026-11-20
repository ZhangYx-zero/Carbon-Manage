package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class Enterprise {

    private Long id;
    private String enterpriseName;
    private String workshopName;
    private String industry;

    /** 单次采集碳排放阈值（kgCO2e），超过即判定超标 */
    private BigDecimal thresholdValue;

    private String contactPerson;
    private String contactPhone;
    private Integer status;

    /** 该企业配置采集的能源类型：ELECTRICITY / GAS，不落 t_enterprise 表 */
    private List<String> energyTypes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
