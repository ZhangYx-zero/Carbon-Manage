package com.carbon.manage.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.UserContext;
import com.carbon.manage.entity.EnergyTypeDict;
import com.carbon.manage.enums.UserRole;
import com.carbon.manage.mapper.EnergyTypeDictMapper;

/**
 * 能源类型字典：管理员可自主维护类型编码、名称、单位与模拟采集比例区间。
 */
@Service
@RequiredArgsConstructor
public class EnergyTypeService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,19}$");
    private static final BigDecimal MAX_RATIO = new BigDecimal("3");

    private final EnergyTypeDictMapper typeMapper;

    public List<EnergyTypeDict> list() {
        return typeMapper.selectList();
    }

    public List<EnergyTypeDict> listEnabled() {
        return typeMapper.selectEnabled();
    }

    public EnergyTypeDict getById(Long id) {
        EnergyTypeDict type = typeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "能源类型不存在");
        }
        return type;
    }

    public EnergyTypeDict getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return typeMapper.selectByCode(code.trim().toUpperCase(Locale.ROOT));
    }

    /** 编码对应的中文名称，字典缺失时回退原始编码。 */
    public String labelOf(String code) {
        EnergyTypeDict type = getByCode(code);
        return type != null ? type.getTypeName() : code;
    }

    /** 编码对应的计量单位，字典缺失时返回空串。 */
    public String unitOf(String code) {
        EnergyTypeDict type = getByCode(code);
        return type != null ? type.getUnit() : "";
    }

    public Long create(EnergyTypeDict type) {
        UserContext.requireRole(UserRole.ADMIN);
        String code = normalizeCode(type.getTypeCode());
        validateCode(code);
        if (typeMapper.selectByCode(code) != null) {
            throw new BusinessException(400, "能源类型编码已存在：" + code);
        }
        type.setTypeCode(code);
        type.setTypeName(requireName(type.getTypeName()));
        type.setUnit(requireUnit(type.getUnit()));
        validateRatio(type.getRatioMin(), type.getRatioMax());
        type.setStatus(type.getStatus() == null ? 1 : type.getStatus());
        type.setSortOrder(type.getSortOrder() == null ? 0 : type.getSortOrder());
        try {
            typeMapper.insert(type);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(400, "能源类型编码已存在：" + code);
        }
        return type.getId();
    }

    public void update(Long id, EnergyTypeDict type) {
        UserContext.requireRole(UserRole.ADMIN);
        EnergyTypeDict exist = getById(id);
        if (StringUtils.hasText(type.getTypeCode())
                && !exist.getTypeCode().equalsIgnoreCase(type.getTypeCode().trim())) {
            throw new BusinessException(400, "能源类型编码创建后不可修改");
        }
        String code = exist.getTypeCode();
        String name = type.getTypeName() == null ? exist.getTypeName() : requireName(type.getTypeName());
        String unit = type.getUnit() == null ? exist.getUnit() : requireUnit(type.getUnit());
        BigDecimal ratioMin = type.getRatioMin() == null ? exist.getRatioMin() : type.getRatioMin();
        BigDecimal ratioMax = type.getRatioMax() == null ? exist.getRatioMax() : type.getRatioMax();
        validateRatio(ratioMin, ratioMax);
        if (!unit.equals(exist.getUnit()) && typeMapper.countEnergyData(code) > 0) {
            throw new BusinessException(400, "该能源类型已有历史能耗数据，计量单位不可修改");
        }
        type.setId(id);
        type.setTypeCode(null);
        type.setTypeName(name);
        type.setUnit(unit);
        type.setRatioMin(ratioMin);
        type.setRatioMax(ratioMax);
        typeMapper.update(type);
    }

    public void delete(Long id) {
        UserContext.requireRole(UserRole.ADMIN);
        EnergyTypeDict exist = getById(id);
        String code = exist.getTypeCode();
        if (typeMapper.countEnterpriseConfigs(code) > 0
                || typeMapper.countFactors(code) > 0
                || typeMapper.countEnergyData(code) > 0) {
            throw new BusinessException(400, "该能源类型已被企业配置 / 碳排因子 / 能耗数据引用，无法删除，可改为停用");
        }
        typeMapper.deleteById(id);
    }

    private String normalizeCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(400, "类型编码不能为空");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private void validateCode(String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException(400, "类型编码需为 2-20 位大写字母、数字或下划线，且以字母开头");
        }
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(400, "类型名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            throw new BusinessException(400, "类型名称不能超过 50 个字符");
        }
        return trimmed;
    }

    private String requireUnit(String unit) {
        if (!StringUtils.hasText(unit)) {
            throw new BusinessException(400, "计量单位不能为空");
        }
        String trimmed = unit.trim();
        if (trimmed.length() > 20) {
            throw new BusinessException(400, "计量单位不能超过 20 个字符");
        }
        return trimmed;
    }

    private void validateRatio(BigDecimal ratioMin, BigDecimal ratioMax) {
        if (ratioMin == null || ratioMax == null) {
            throw new BusinessException(400, "采集模拟比例区间不能为空");
        }
        if (ratioMin.compareTo(BigDecimal.ZERO) <= 0 || ratioMax.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "采集模拟比例必须大于 0");
        }
        if (ratioMin.compareTo(ratioMax) > 0) {
            throw new BusinessException(400, "采集模拟比例下限不能大于上限");
        }
        if (ratioMax.compareTo(MAX_RATIO) > 0) {
            throw new BusinessException(400, "采集模拟比例上限不能超过 3");
        }
    }
}
