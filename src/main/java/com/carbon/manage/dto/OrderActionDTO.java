package com.carbon.manage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 工单操作参数：派单填 assigneeId/deadline，提交整改填 rectifyResult，审核填 auditRemark。
 */
@Data
public class OrderActionDTO {

    private Long assigneeId;
    private String rectifyResult;
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
}
