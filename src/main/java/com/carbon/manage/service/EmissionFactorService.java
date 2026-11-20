package com.carbon.manage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.UserContext;
import com.carbon.manage.entity.EmissionFactor;
import com.carbon.manage.entity.EnergyTypeDict;
import com.carbon.manage.enums.UserRole;
import com.carbon.manage.mapper.EmissionFactorMapper;

@Service
@RequiredArgsConstructor
public class EmissionFactorService {

    private final EmissionFactorMapper factorMapper;
    private final EnergyTypeService energyTypeService;

    public List<EmissionFactor> list() {
        return factorMapper.selectList();
    }

    public List<EmissionFactor> listEnabled() {
        return factorMapper.selectEnabled();
    }

    public EmissionFactor getById(Long id) {
        EmissionFactor factor = factorMapper.selectById(id);
        if (factor == null) {
            throw new BusinessException(404, "碳排因子不存在");
        }
        return factor;
    }

    public Long create(EmissionFactor factor) {
        UserContext.requireRole(UserRole.ADMIN);
        validate(factor);
        factor.setStatus(factor.getStatus() == null ? 1 : factor.getStatus());
        if (factor.getEffectiveDate() == null) {
            factor.setEffectiveDate(LocalDate.now());
        }
        factorMapper.insert(factor);
        return factor.getId();
    }

    public void update(Long id, EmissionFactor factor) {
        UserContext.requireRole(UserRole.ADMIN);
        EmissionFactor exist = getById(id);
        if (factor.getFactorValue() != null
                && factor.getFactorValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "碳排因子必须大于 0");
        }
        String energyType = exist.getEnergyType();
        if (factor.getEnergyType() != null) {
            EnergyTypeDict dict = requireType(factor.getEnergyType(), exist.getEnergyType());
            energyType = dict.getTypeCode();
            if (!energyType.equals(exist.getEnergyType()) && factorMapper.countEnergyData(id) > 0) {
                throw new BusinessException(400, "该因子已参与碳排核算，能源类型不可修改");
            }
            factor.setEnergyType(energyType);
        }
        if (factor.getUnit() != null) {
            if (!StringUtils.hasText(factor.getUnit())) {
                throw new BusinessException(400, "计量单位不能为空");
            }
            EnergyTypeDict dict = energyTypeService.getByCode(energyType);
            String unit = factor.getUnit().trim();
            if (dict != null && !unit.equals(dict.getUnit())) {
                throw new BusinessException(400, "计量单位须与能源类型一致：" + dict.getUnit());
            }
            factor.setUnit(unit);
        }
        factor.setId(id);
        factorMapper.update(factor);
    }

    public void delete(Long id) {
        UserContext.requireRole(UserRole.ADMIN);
        getById(id);
        if (factorMapper.countEnergyData(id) > 0) {
            throw new BusinessException(400, "该因子已参与碳排核算，无法删除，可改为停用");
        }
        factorMapper.deleteById(id);
    }

    private void validate(EmissionFactor factor) {
        if (!StringUtils.hasText(factor.getFactorName())) {
            throw new BusinessException(400, "因子名称不能为空");
        }
        EnergyTypeDict dict = requireType(factor.getEnergyType(), null);
        factor.setEnergyType(dict.getTypeCode());
        if (factor.getFactorValue() == null || factor.getFactorValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "碳排因子必须大于 0");
        }
        if (!StringUtils.hasText(factor.getUnit())) {
            factor.setUnit(dict.getUnit());
        } else {
            String unit = factor.getUnit().trim();
            if (!unit.equals(dict.getUnit())) {
                throw new BusinessException(400, "计量单位须与能源类型一致：" + dict.getUnit());
            }
            factor.setUnit(unit);
        }
    }

    /**
     * 因子只能挂在字典中的能源类型上；停用类型仅允许用于编辑已有因子，不允许新挂载。
     */
    private EnergyTypeDict requireType(String code, String allowedDisabledCode) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "请选择能源类型");
        }
        EnergyTypeDict dict = energyTypeService.getByCode(code);
        if (dict == null) {
            throw new BusinessException(400, "不支持的能源类型：" + code);
        }
        if (dict.getStatus() != 1
                && (allowedDisabledCode == null || !allowedDisabledCode.equals(dict.getTypeCode()))) {
            throw new BusinessException(400, "能源类型已停用：" + dict.getTypeCode());
        }
        return dict;
    }
}
