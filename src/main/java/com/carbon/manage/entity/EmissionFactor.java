package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EmissionFactor {

    private Long id;
    private String energyType;
    private String factorName;
    private BigDecimal factorValue;
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    private Integer status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
