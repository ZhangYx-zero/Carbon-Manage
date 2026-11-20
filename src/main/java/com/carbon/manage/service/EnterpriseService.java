package com.carbon.manage.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.UserContext;
import com.carbon.manage.entity.EmissionFactor;
import com.carbon.manage.entity.EnergyTypeDict;
import com.carbon.manage.entity.Enterprise;
import com.carbon.manage.entity.EnterpriseEnergyType;
import com.carbon.manage.enums.UserRole;
import com.carbon.manage.mapper.EmissionFactorMapper;
import com.carbon.manage.mapper.EnergyTypeDictMapper;
import com.carbon.manage.mapper.EnterpriseEnergyTypeMapper;
import com.carbon.manage.mapper.EnterpriseMapper;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseEnergyTypeMapper energyTypeMapper;
    private final EnergyTypeDictMapper typeDictMapper;
    private final EmissionFactorMapper factorMapper;

    public List<Enterprise> list(String keyword) {
        List<Enterprise> enterprises = enterpriseMapper.selectList(keyword);
        fillEnergyTypes(enterprises);
        return enterprises;
    }

    public List<Enterprise> listEnabled() {
        List<Enterprise> enterprises = enterpriseMapper.selectEnabled();
        fillEnergyTypes(enterprises);
        return enterprises;
    }

    public Enterprise getById(Long id) {
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException(404, "企业不存在");
        }
        enterprise.setEnergyTypes(energyTypeMapper.selectEnergyTypesByEnterpriseId(id));
        return enterprise;
    }

    @Transactional
    public Long create(Enterprise enterprise) {
        UserContext.requireRole(UserRole.ADMIN);
        validate(enterprise);
        List<String> energyTypes = normalizeEnergyTypes(enterprise.getEnergyTypes(), true, null);
        enterprise.setStatus(enterprise.getStatus() == null ? 1 : enterprise.getStatus());
        enterpriseMapper.insert(enterprise);
        energyTypeMapper.insertBatch(enterprise.getId(), energyTypes);
        enterprise.setEnergyTypes(energyTypes);
        return enterprise.getId();
    }

    @Transactional
    public void update(Long id, Enterprise enterprise) {
        UserContext.requireRole(UserRole.ADMIN);
        getById(id);
        if (enterprise.getThresholdValue() != null
                && enterprise.getThresholdValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "碳排放阈值必须大于 0");
        }
        List<String> energyTypes = null;
        if (enterprise.getEnergyTypes() != null) {
            energyTypes = normalizeEnergyTypes(enterprise.getEnergyTypes(), false, id);
        }
        enterprise.setId(id);
        enterpriseMapper.update(enterprise);
        if (energyTypes != null) {
            energyTypeMapper.deleteByEnterpriseId(id);
            energyTypeMapper.insertBatch(id, energyTypes);
        }
    }

    @Transactional
    public void delete(Long id) {
        UserContext.requireRole(UserRole.ADMIN);
        getById(id);
        if (enterpriseMapper.countEnergyData(id) > 0) {
            throw new BusinessException(400, "该企业已有能耗数据，无法删除，可改为停用");
        }
        energyTypeMapper.deleteByEnterpriseId(id);
        enterpriseMapper.deleteById(id);
    }

    /**
     * 企业列表批量补齐能源类型，避免逐行查询。
     */
    private void fillEnergyTypes(List<Enterprise> enterprises) {
        if (enterprises.isEmpty()) {
            return;
        }
        List<Long> ids = enterprises.stream().map(Enterprise::getId).toList();
        Map<Long, List<String>> typeMap = energyTypeMapper.selectByEnterpriseIds(ids).stream()
                .collect(Collectors.groupingBy(
                        EnterpriseEnergyType::getEnterpriseId,
                        Collectors.mapping(EnterpriseEnergyType::getEnergyType, Collectors.toList())));
        enterprises.forEach(e -> e.setEnergyTypes(typeMap.getOrDefault(e.getId(), List.of())));
    }

    /**
     * 校验并规范化能源类型编码：编码必须存在于字典；新增选择必须是启用类型，
     * 企业已配置的停用类型允许保留，避免编辑其他字段时被迫改动历史配置。
     */
    private List<String> normalizeEnergyTypes(List<String> rawTypes, boolean isCreate, Long enterpriseId) {
        Map<String, EnergyTypeDict> dictMap = typeDictMapper.selectList().stream()
                .collect(Collectors.toMap(EnergyTypeDict::getTypeCode, t -> t, (a, b) -> a));
        Set<String> existing = enterpriseId == null
                ? Set.of()
                : new HashSet<>(energyTypeMapper.selectEnergyTypesByEnterpriseId(enterpriseId));
        if (rawTypes == null || rawTypes.isEmpty()) {
            if (!isCreate) {
                throw new BusinessException(400, "请至少选择一种能源类型");
            }
            Set<String> factorTypes = factorMapper.selectEnabled().stream()
                    .map(EmissionFactor::getEnergyType)
                    .collect(Collectors.toSet());
            List<String> defaults = dictMap.values().stream()
                    .filter(t -> t.getStatus() == 1 && factorTypes.contains(t.getTypeCode()))
                    .sorted(Comparator.comparing(EnergyTypeDict::getSortOrder)
                            .thenComparing(EnergyTypeDict::getId))
                    .map(EnergyTypeDict::getTypeCode)
                    .toList();
            if (!defaults.isEmpty()) {
                return new ArrayList<>(defaults);
            }
            throw new BusinessException(400, "请至少选择一种能源类型");
        }
        LinkedHashSet<String> types = new LinkedHashSet<>();
        for (String raw : rawTypes) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            EnergyTypeDict type = dictMap.get(code);
            if (type == null) {
                throw new BusinessException(400, "不支持的能源类型：" + code);
            }
            if (type.getStatus() != 1 && !existing.contains(code)) {
                throw new BusinessException(400, "能源类型已停用：" + code);
            }
            types.add(code);
        }
        if (types.isEmpty()) {
            throw new BusinessException(400, "请至少选择一种能源类型");
        }
        return new ArrayList<>(types);
    }

    private void validate(Enterprise enterprise) {
        if (!StringUtils.hasText(enterprise.getEnterpriseName())) {
            throw new BusinessException(400, "企业名称不能为空");
        }
        if (!StringUtils.hasText(enterprise.getWorkshopName())) {
            throw new BusinessException(400, "车间名称不能为空");
        }
        if (enterprise.getThresholdValue() == null
                || enterprise.getThresholdValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "碳排放阈值必须大于 0");
        }
    }
}
