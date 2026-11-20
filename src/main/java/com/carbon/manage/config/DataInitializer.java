package com.carbon.manage.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.carbon.manage.entity.EmissionFactor;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.entity.EnergyTypeDict;
import com.carbon.manage.entity.Enterprise;
import com.carbon.manage.entity.User;
import com.carbon.manage.enums.UserRole;
import com.carbon.manage.mapper.EmissionFactorMapper;
import com.carbon.manage.mapper.EnergyDataMapper;
import com.carbon.manage.mapper.EnergyTypeDictMapper;
import com.carbon.manage.mapper.EnterpriseEnergyTypeMapper;
import com.carbon.manage.mapper.EnterpriseMapper;
import com.carbon.manage.mapper.UserMapper;

/**
 * 种子数据初始化：演示账号、碳排因子、企业、历史能耗（供仪表盘展示）。
 * <p>历史数据刻意不触发 AI 诊断，避免首次启动就刷出大量工单。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ELECTRICITY = "ELECTRICITY";
    private static final String GAS = "GAS";

    private final UserMapper userMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseEnergyTypeMapper energyTypeMapper;
    private final EnergyTypeDictMapper typeDictMapper;
    private final EmissionFactorMapper factorMapper;
    private final EnergyDataMapper energyDataMapper;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        initAdmin();
        initOperators();
        initEnergyTypes();
        initFactors();
        initEnterprises();
        ensureEnterpriseEnergyTypes();
        initHistoryEnergyData();
    }

    private void initAdmin() {
        if (userMapper.countByUsername("admin") > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setNickname("系统管理员");
        admin.setRole(UserRole.ADMIN.name());
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("已初始化演示账号 admin / 123456");
    }

    private void initOperators() {
        seedOperator("operator1", "王工");
        seedOperator("operator2", "李工");
    }

    private void seedOperator(String username, String nickname) {
        if (userMapper.countByUsername(username) > 0) {
            return;
        }
        User operator = new User();
        operator.setUsername(username);
        operator.setPassword(passwordEncoder.encode("123456"));
        operator.setNickname(nickname);
        operator.setRole(UserRole.OPERATOR.name());
        operator.setStatus(1);
        userMapper.insert(operator);
        log.info("已初始化整改负责人账号 {} / 123456", username);
    }

    /**
     * 能源类型字典为空时种入默认类型；管理员后续可自由增删改，不会被启动逻辑覆盖。
     */
    private void initEnergyTypes() {
        if (typeDictMapper.countAll() > 0) {
            return;
        }
        EnergyTypeDict electricity = new EnergyTypeDict();
        electricity.setTypeCode(ELECTRICITY);
        electricity.setTypeName("电力");
        electricity.setUnit("kWh");
        electricity.setRatioMin(new BigDecimal("0.65"));
        electricity.setRatioMax(new BigDecimal("0.85"));
        electricity.setStatus(1);
        electricity.setSortOrder(1);
        electricity.setRemark("默认能源类型");
        typeDictMapper.insert(electricity);

        EnergyTypeDict gas = new EnergyTypeDict();
        gas.setTypeCode(GAS);
        gas.setTypeName("天然气");
        gas.setUnit("m³");
        gas.setRatioMin(new BigDecimal("0.45"));
        gas.setRatioMax(new BigDecimal("0.65"));
        gas.setStatus(1);
        gas.setSortOrder(2);
        gas.setRemark("默认能源类型");
        typeDictMapper.insert(gas);
        log.info("已初始化能源类型字典：电力 / 天然气");
    }

    private void initFactors() {
        if (factorMapper.selectByEnergyType(ELECTRICITY) == null) {
            EmissionFactor electricity = new EmissionFactor();
            electricity.setEnergyType(ELECTRICITY);
            electricity.setFactorName("华东电网电力排放因子");
            electricity.setFactorValue(new BigDecimal("0.5703"));
            electricity.setUnit("kWh");
            electricity.setEffectiveDate(LocalDate.of(2025, 1, 1));
            electricity.setStatus(1);
            electricity.setRemark("kgCO2e per kWh，参考区域电网平均排放因子");
            factorMapper.insert(electricity);
        }
        if (factorMapper.selectByEnergyType(GAS) == null) {
            EmissionFactor gas = new EmissionFactor();
            gas.setEnergyType(GAS);
            gas.setFactorName("天然气排放因子");
            gas.setFactorValue(new BigDecimal("2.1620"));
            gas.setUnit("m³");
            gas.setEffectiveDate(LocalDate.of(2025, 1, 1));
            gas.setStatus(1);
            gas.setRemark("kgCO2e per m³，天然气燃烧排放因子");
            factorMapper.insert(gas);
        }
    }

    private void initEnterprises() {
        if (!enterpriseMapper.selectList(null).isEmpty()) {
            return;
        }
        Enterprise first = new Enterprise();
        first.setEnterpriseName("华东精密制造有限公司");
        first.setWorkshopName("一号机加工车间");
        first.setIndustry("装备制造");
        first.setThresholdValue(new BigDecimal("500.0000"));
        first.setContactPerson("张工");
        first.setContactPhone("13800000001");
        first.setStatus(1);
        enterpriseMapper.insert(first);

        Enterprise second = new Enterprise();
        second.setEnterpriseName("绿源食品加工有限公司");
        second.setWorkshopName("烘焙车间");
        second.setIndustry("食品加工");
        second.setThresholdValue(new BigDecimal("320.0000"));
        second.setContactPerson("李工");
        second.setContactPhone("13800000002");
        second.setStatus(1);
        enterpriseMapper.insert(second);
        log.info("已初始化 2 个演示企业");
    }

    /**
     * 兼容旧库：没有能源类型配置的企业默认补上“已启用且有启用因子”的全部类型。
     */
    private void ensureEnterpriseEnergyTypes() {
        List<String> defaultTypes = factorMapper.selectEnabled().stream()
                .map(EmissionFactor::getEnergyType)
                .distinct()
                .toList();
        if (defaultTypes.isEmpty()) {
            return;
        }
        int added = 0;
        for (Enterprise enterprise : enterpriseMapper.selectList(null)) {
            if (energyTypeMapper.selectEnergyTypesByEnterpriseId(enterprise.getId()).isEmpty()) {
                energyTypeMapper.insertBatch(enterprise.getId(), defaultTypes);
                added++;
            }
        }
        if (added > 0) {
            log.info("已为 {} 个企业补齐默认能源类型配置", added);
        }
    }

    private void initHistoryEnergyData() {
        if (energyDataMapper.countPage(null, null, null) > 0) {
            return;
        }
        List<Enterprise> enterprises = enterpriseMapper.selectEnabled();
        Map<String, EmissionFactor> factorMap = factorMapper.selectEnabled().stream()
                .collect(Collectors.toMap(EmissionFactor::getEnergyType, f -> f, (a, b) -> a));
        Map<String, EnergyTypeDict> typeMap = typeDictMapper.selectEnabled().stream()
                .collect(Collectors.toMap(EnergyTypeDict::getTypeCode, t -> t, (a, b) -> a));
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
                for (int hoursAgo = 24; hoursAgo >= 1; hoursAgo--) {
                    double ratio = randomRatio(type);
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
                    data.setIsExceeded(0);
                    data.setCollectTime(LocalDateTime.now().minusHours(hoursAgo));
                    energyDataMapper.insert(data);
                    count++;
                }
            }
        }
        log.info("已回填 {} 条历史能耗数据用于仪表盘演示", count);
    }

    private double randomRatio(EnergyTypeDict type) {
        double min = type.getRatioMin().doubleValue();
        double max = type.getRatioMax().doubleValue();
        return min + random.nextDouble() * (max - min);
    }
}
