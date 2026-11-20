package com.carbon.manage.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.carbon.manage.entity.EmissionFactor;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.entity.EnergyTypeDict;
import com.carbon.manage.entity.Enterprise;
import com.carbon.manage.mapper.EmissionFactorMapper;
import com.carbon.manage.mapper.EnergyTypeDictMapper;
import com.carbon.manage.mapper.EnterpriseEnergyTypeMapper;
import com.carbon.manage.mapper.EnterpriseMapper;
import com.carbon.manage.service.AiDiagnosisService;
import com.carbon.manage.service.EnergyDataService;

/**
 * IoT 数据采集模拟器：周期性为每个启用企业按字典中已配置的能源类型生成能耗数据，
 * 按碳排因子核算碳排放，超过阈值时触发 AI 诊断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnergyCollectTask {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseEnergyTypeMapper energyTypeMapper;
    private final EnergyTypeDictMapper typeDictMapper;
    private final EmissionFactorMapper factorMapper;
    private final EnergyDataService energyDataService;
    private final AiDiagnosisService aiDiagnosisService;
    private final Random random = new Random();

    @Value("${carbon.collect.enabled:true}")
    private boolean collectEnabled;

    @Value("${carbon.ai.enabled:true}")
    private boolean aiEnabled;

    @Scheduled(fixedRateString = "${carbon.collect.interval-ms:30000}",
            initialDelayString = "${carbon.collect.initial-delay-ms:15000}")
    public void scheduledCollect() {
        if (!collectEnabled) {
            return;
        }
        try {
            int count = collectOnce();
            if (count > 0) {
                log.info("IoT 模拟采集完成，新增 {} 条能耗数据", count);
            }
        } catch (Exception e) {
            log.error("定时采集任务执行失败", e);
        }
    }

    /**
     * 执行一轮采集。每家企业提交事务后再触发 AI，保证异步线程能读到已提交的数据。
     */
    public int collectOnce() {
        Map<String, EmissionFactor> factorMap = factorMapper.selectEnabled().stream()
                .collect(Collectors.toMap(EmissionFactor::getEnergyType, f -> f, (a, b) -> a));
        Map<String, EnergyTypeDict> typeMap = typeDictMapper.selectEnabled().stream()
                .collect(Collectors.toMap(EnergyTypeDict::getTypeCode, t -> t, (a, b) -> a));
        List<Enterprise> enterprises = enterpriseMapper.selectEnabled();
        int count = 0;
        for (Enterprise enterprise : enterprises) {
            List<String> configuredTypes = energyTypeMapper.selectEnergyTypesByEnterpriseId(enterprise.getId());
            for (String typeCode : configuredTypes) {
                EnergyTypeDict type = typeMap.get(typeCode);
                if (type == null) {
                    continue;
                }
                EmissionFactor factor = factorMap.get(typeCode);
                if (factor == null) {
                    continue;
                }
                EnergyData data = generate(enterprise, factor, type);
                energyDataService.create(data);
                count++;
                if (data.getIsExceeded() == 1 && aiEnabled) {
                    Long logId = aiDiagnosisService.claimAndTrigger(data.getId());
                    if (logId != null) {
                        log.info("能耗数据 {}（{}）超标，已触发 AI 诊断，logId={}",
                                data.getId(), type.getTypeName(), logId);
                    }
                }
            }
        }
        return count;
    }

    private EnergyData generate(Enterprise enterprise, EmissionFactor factor, EnergyTypeDict type) {
        double min = type.getRatioMin().doubleValue();
        double max = type.getRatioMax().doubleValue();
        double ratio = min + random.nextDouble() * (max - min);
        // 10% 概率出现尖峰工况，用于演示超标告警链路
        if (random.nextDouble() < 0.10) {
            ratio *= 1.70 + random.nextDouble() * 0.15;
        }
        BigDecimal targetEmission = enterprise.getThresholdValue()
                .multiply(BigDecimal.valueOf(ratio))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal energyValue = targetEmission.divide(factor.getFactorValue(), 2, RoundingMode.HALF_UP);
        BigDecimal emission = energyValue.multiply(factor.getFactorValue())
                .setScale(4, RoundingMode.HALF_UP);

        EnergyData data = new EnergyData();
        data.setEnterpriseId(enterprise.getId());
        data.setFactorId(factor.getId());
        data.setEnergyType(type.getTypeCode());
        data.setEnergyValue(energyValue);
        data.setEmissionValue(emission);
        data.setThresholdValue(enterprise.getThresholdValue());
        data.setIsExceeded(emission.compareTo(enterprise.getThresholdValue()) > 0 ? 1 : 0);
        data.setCollectTime(LocalDateTime.now());
        return data;
    }
}
