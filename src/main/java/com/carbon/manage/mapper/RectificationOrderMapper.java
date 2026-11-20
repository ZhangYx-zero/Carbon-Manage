package com.carbon.manage.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.RectificationOrder;

@Mapper
public interface RectificationOrderMapper {

    int insert(RectificationOrder order);

    RectificationOrder selectById(@Param("id") Long id);

    RectificationOrder selectByEnergyDataId(@Param("energyDataId") Long energyDataId);

    List<RectificationOrder> selectPage(@Param("status") String status,
                                        @Param("enterpriseId") Long enterpriseId,
                                        @Param("assigneeId") Long assigneeId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    long countPage(@Param("status") String status,
                   @Param("enterpriseId") Long enterpriseId,
                   @Param("assigneeId") Long assigneeId);

    /**
     * 带旧状态条件的 CAS 更新：并发下若状态已被他人改变，影响行数为 0。
     * <p>只有非空参数才会被写入，因此提交/审核/驳回传 null 不会覆盖原有负责人和整改结果。</p>
     */
    int changeStatusCas(@Param("id") Long id,
                        @Param("from") String from,
                        @Param("to") String to,
                        @Param("assigneeId") Long assigneeId,
                        @Param("assignee") String assignee,
                        @Param("rectifyResult") String rectifyResult,
                        @Param("auditRemark") String auditRemark,
                        @Param("deadline") LocalDateTime deadline);
}
