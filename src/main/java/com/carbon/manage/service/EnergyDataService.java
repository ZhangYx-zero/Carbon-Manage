package com.carbon.manage.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.PageResult;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.mapper.EnergyDataMapper;

@Service
@RequiredArgsConstructor
public class EnergyDataService {

    private final EnergyDataMapper energyDataMapper;

    public PageResult<EnergyData> page(int pageNum, int pageSize, Long enterpriseId,
                                       String energyType, Boolean exceeded) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long total = energyDataMapper.countPage(enterpriseId, energyType, exceeded);
        if (total == 0) {
            return PageResult.of(0, safePageNum, safePageSize, List.of());
        }
        int offset = (safePageNum - 1) * safePageSize;
        List<EnergyData> list = energyDataMapper.selectPage(enterpriseId, energyType, exceeded, offset, safePageSize);
        return PageResult.of(total, safePageNum, safePageSize, list);
    }

    public EnergyData getById(Long id) {
        EnergyData data = energyDataMapper.selectById(id);
        if (data == null) {
            throw new BusinessException(404, "能耗数据不存在");
        }
        return data;
    }

    /**
     * 写入一条采集数据。事务在此方法结束时提交，调用方随后触发 AI 诊断时数据已可见。
     */
    @Transactional
    public EnergyData create(EnergyData data) {
        energyDataMapper.insert(data);
        return data;
    }
}
