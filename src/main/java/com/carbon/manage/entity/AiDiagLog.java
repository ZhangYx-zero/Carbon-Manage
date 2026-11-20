package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiDiagLog {

    private Long id;
    private Long energyDataId;
    private Long orderId;
    private String modelName;
    private String promptSnapshot;
    private String diagnosis;
    private String suggestion;
    private String status;
    private String errorMsg;
    private Long durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 关联查询展示字段 */
    private Long enterpriseId;
    private String enterpriseName;
    private String workshopName;
    private String energyType;
}
