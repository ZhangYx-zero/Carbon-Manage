package com.carbon.manage.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DashboardVO {

    private BigDecimal todayEmission;
    private long todayExceeded;
    private long activeOrders;
    private long todayDiagnosis;

    /** 近 24 小时碳排放趋势：[{timeLabel, value}] */
    private List<Map<String, Object>> emissionTrend;

    /** 能源类型碳排占比：[{name, value}] */
    private List<Map<String, Object>> energyPie;

    /** 工单状态分布：[{name, value}] */
    private List<Map<String, Object>> orderStatus;
}
