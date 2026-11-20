package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RectificationOrder {

    private Long id;
    private String orderNo;
    private Long enterpriseId;
    private Long energyDataId;
    private String title;
    private String suggestion;
    private String status;
    private String assignee;
    private Long assigneeId;
    private String rectifyResult;
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 关联查询展示字段 */
    private String enterpriseName;
    private String workshopName;
}
