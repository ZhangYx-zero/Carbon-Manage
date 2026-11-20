package com.carbon.manage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carbon.manage.entity.AiDiagLog;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.entity.RectificationOrder;
import com.carbon.manage.mapper.AiDiagLogMapper;
import com.carbon.manage.service.RectificationOrderService;

/**
 * 诊断结果落库（独立 Bean，保证 @Transactional 生效且事务范围短：
 * 大模型调用不在事务内，避免长时间占用数据库连接）。
 */
@Service
@RequiredArgsConstructor
public class AiDiagPersistService {

    private final AiDiagLogMapper diagLogMapper;
    private final RectificationOrderService orderService;

    @Transactional
    public void saveSuccess(Long logId, String diagnosis, String suggestion,
                            AiDiagLog log, long durationMs) {
        RectificationOrder order = orderService.createFromDiagnosis(toEnergyData(log), suggestion);
        diagLogMapper.markSuccess(logId, diagnosis, suggestion, order.getId(), durationMs);
    }

    @Transactional
    public void saveFallback(Long logId, String fallbackSuggestion, String errorMsg,
                             AiDiagLog log, long durationMs) {
        RectificationOrder order = orderService.createFromDiagnosis(toEnergyData(log), fallbackSuggestion);
        diagLogMapper.markFallback(logId, fallbackSuggestion, fallbackSuggestion,
                order.getId(), errorMsg, durationMs);
    }

    @Transactional
    public void markFailed(Long logId, String errorMsg, long durationMs) {
        diagLogMapper.markFailed(logId, errorMsg, durationMs);
    }

    private EnergyData toEnergyData(AiDiagLog log) {
        EnergyData data = new EnergyData();
        data.setId(log.getEnergyDataId());
        data.setEnterpriseId(log.getEnterpriseId());
        data.setEnterpriseName(log.getEnterpriseName());
        data.setWorkshopName(log.getWorkshopName());
        data.setEnergyType(log.getEnergyType());
        return data;
    }
}
