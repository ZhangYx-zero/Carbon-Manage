package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 能源类型字典：管理员可维护的类型集合（编码、名称、单位、模拟采集比例区间）。
 */
@Data
public class EnergyTypeDict {

    private Long id;

    /** 类型编码（大写，创建后不可修改），被能耗/因子/企业配置表引用 */
    private String typeCode;

    /** 类型名称，如：电力、天然气、蒸汽 */
    private String typeName;

    /** 计量单位，如：kWh、m³、t */
    private String unit;

    /** 模拟采集目标排放占企业阈值的比例下限 */
    private BigDecimal ratioMin;

    /** 模拟采集目标排放占企业阈值的比例上限 */
    private BigDecimal ratioMax;

    private Integer status;

    private Integer sortOrder;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
