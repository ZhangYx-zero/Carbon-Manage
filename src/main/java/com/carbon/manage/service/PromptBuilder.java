package com.carbon.manage.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.carbon.manage.entity.EnergyData;

/**
 * Prompt 组装器：System 固定专家角色与输出结构，User 注入本次异常数据。
 * <p>本地大模型不可用时，用 {@link #fallbackSuggestion(String)} 的规则化建议兜底。</p>
 */
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private static final String ELECTRICITY = "ELECTRICITY";
    private static final String GAS = "GAS";

    public static final String SYSTEM_PROMPT = """
            你是一名企业碳排放诊断专家。请只依据用户提供的车间能耗监测数据分析，
            不要编造数据中不存在的设备、数值或事件。
            严格按以下三段结构，使用中文，总字数不超过 200 字：
            【异常原因】结合数据说明最可能的异常原因。
            【可疑排放源】指出可疑的设备或环节。
            【整改建议】给出 2-4 条可落地的减排整改措施。
            """;

    private final EnergyTypeService energyTypeService;

    public String buildUserPrompt(EnergyData data) {
        BigDecimal threshold = data.getThresholdValue();
        BigDecimal exceedPercent = data.getEmissionValue().subtract(threshold)
                .multiply(BigDecimal.valueOf(100))
                .divide(threshold, 1, RoundingMode.HALF_UP);
        return """
                企业名称：%s
                车间名称：%s
                采集时间：%s
                能源类型：%s
                能耗用量：%s %s
                碳排放量：%s kgCO2e
                报警阈值：%s kgCO2e
                判定结果：%s
                超标比例：%s%%
                请分析本次能耗超标的原因，并给出减排整改建议。
                """.formatted(
                data.getEnterpriseName(),
                data.getWorkshopName(),
                data.getCollectTime(),
                energyTypeService.labelOf(data.getEnergyType()),
                data.getEnergyValue(),
                energyTypeService.unitOf(data.getEnergyType()),
                data.getEmissionValue(),
                threshold,
                Integer.valueOf(1).equals(data.getIsExceeded()) ? "已超标" : "未超标",
                exceedPercent);
    }

    /**
     * 本地大模型不可用（未启动/超时/模型缺失）时的规则兜底建议。
     */
    public String fallbackSuggestion(String energyType) {
        if (GAS.equals(energyType)) {
            return "【异常原因】天然气用量短时异常升高，疑似管网泄漏、燃烧不充分或计量偏差。\n"
                    + "【可疑排放源】燃气锅炉、热处理炉、燃气管道及法兰接口。\n"
                    + "【整改建议】1. 立即巡检燃气管网与法兰接口排查泄漏；2. 检查燃烧器空燃比，避免燃烧不充分；"
                    + "3. 复核燃气表计量精度；4. 优化炉窑保温与生产排程，减少空烧时间。";
        }
        if (ELECTRICITY.equals(energyType)) {
            return "【异常原因】用电负荷短时异常升高，疑似设备空转、空压机/空调负荷上升或计量异常。\n"
                    + "【可疑排放源】空压机、中央空调、照明与产线待机设备。\n"
                    + "【整改建议】1. 排查空压机与空调运行状态，避免设备空转；2. 关闭非必要待机设备；"
                    + "3. 将高耗能工序调整至谷电时段；4. 复核电表计量与产线负荷分配。";
        }
        String label = energyTypeService.labelOf(energyType);
        return "【异常原因】" + label + "用量短时异常升高，疑似设备故障、工艺工况波动或计量偏差。\n"
                + "【可疑排放源】" + label + "相关生产设备、输送管网与计量装置。\n"
                + "【整改建议】1. 安排现场巡检，核查设备运行状态；2. 复核计量装置与用量台账；"
                + "3. 优化工艺参数与生产排程，减少异常工况；4. 建立该能源类型的用量预警线。";
    }
}
