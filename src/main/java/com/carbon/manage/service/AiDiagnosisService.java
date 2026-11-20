package com.carbon.manage.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.PageResult;
import com.carbon.manage.entity.AiDiagLog;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.enums.DiagStatus;
import com.carbon.manage.mapper.AiDiagLogMapper;
import com.carbon.manage.mapper.EnergyDataMapper;

/**
 * 诊断入口：先用唯一索引占位实现幂等，再把真正的耗时的模型调用交给异步线程池。
 */
@Service
@RequiredArgsConstructor
public class AiDiagnosisService {

    private final AiDiagLogMapper diagLogMapper;
    private final EnergyDataMapper energyDataMapper;
    private final PromptBuilder promptBuilder;
    private final AiDiagnosisAsyncService asyncService;

    @Value("${spring.ai.ollama.chat.options.model:qwen2.5:1.5b}")
    private String modelName;

    /**
     * 认领一次诊断：插入 RUNNING 占位日志（uk_energy_data 防重复），成功后再异步执行。
     *
     * @return 新建的诊断日志 ID；若该数据已诊断过则返回 null
     */
    public Long claimAndTrigger(Long energyDataId) {
        EnergyData data = energyDataMapper.selectDetailById(energyDataId);
        if (data == null) {
            throw new BusinessException(404, "能耗数据不存在");
        }
        requireExceeded(data);
        AiDiagLog diagLog = new AiDiagLog();
        diagLog.setEnergyDataId(energyDataId);
        diagLog.setModelName(modelName);
        diagLog.setPromptSnapshot(promptBuilder.buildUserPrompt(data));
        diagLog.setStatus(DiagStatus.RUNNING.name());
        try {
            diagLogMapper.insert(diagLog);
        } catch (DuplicateKeyException e) {
            return null;
        }
        asyncService.diagnose(diagLog.getId());
        return diagLog.getId();
    }

    public PageResult<AiDiagLog> page(int pageNum, int pageSize, String status) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long total = diagLogMapper.countPage(status);
        if (total == 0) {
            return PageResult.of(0, safePageNum, safePageSize, List.of());
        }
        int offset = (safePageNum - 1) * safePageSize;
        return PageResult.of(total, safePageNum, safePageSize,
                diagLogMapper.selectPage(status, offset, safePageSize));
    }

    public AiDiagLog getById(Long id) {
        AiDiagLog log = diagLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(404, "诊断记录不存在");
        }
        return log;
    }

    public void retry(Long logId) {
        AiDiagLog log = getById(logId);
        if (!DiagStatus.FAILED.name().equals(log.getStatus())) {
            throw new BusinessException(400, "仅 FAILED 状态的诊断记录可以重试");
        }
        EnergyData data = energyDataMapper.selectDetailById(log.getEnergyDataId());
        if (data == null) {
            throw new BusinessException(404, "能耗数据不存在");
        }
        requireExceeded(data);
        int updated = diagLogMapper.resetForRetry(logId);
        if (updated == 0) {
            throw new BusinessException(409, "诊断状态已变化，请刷新后重试");
        }
        asyncService.diagnose(logId);
    }

    /**
     * AI 诊断只面向超标数据；正常数据不需要整改建议，也不应生成整改工单。
     */
    private void requireExceeded(EnergyData data) {
        if (data.getIsExceeded() == null || data.getIsExceeded() != 1) {
            throw new BusinessException(400, "该条能耗数据未超标，无需 AI 诊断");
        }
    }
}
