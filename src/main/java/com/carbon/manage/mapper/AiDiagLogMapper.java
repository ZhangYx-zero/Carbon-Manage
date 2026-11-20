package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.AiDiagLog;

@Mapper
public interface AiDiagLogMapper {

    int insert(AiDiagLog log);

    AiDiagLog selectById(@Param("id") Long id);

    List<AiDiagLog> selectPage(@Param("status") String status,
                               @Param("offset") int offset,
                               @Param("size") int size);

    long countPage(@Param("status") String status);

    int markSuccess(@Param("id") Long id,
                    @Param("diagnosis") String diagnosis,
                    @Param("suggestion") String suggestion,
                    @Param("orderId") Long orderId,
                    @Param("durationMs") Long durationMs);

    int markFallback(@Param("id") Long id,
                     @Param("diagnosis") String diagnosis,
                     @Param("suggestion") String suggestion,
                     @Param("orderId") Long orderId,
                     @Param("errorMsg") String errorMsg,
                     @Param("durationMs") Long durationMs);

    int markFailed(@Param("id") Long id,
                   @Param("errorMsg") String errorMsg,
                   @Param("durationMs") Long durationMs);

    int resetForRetry(@Param("id") Long id);
}
