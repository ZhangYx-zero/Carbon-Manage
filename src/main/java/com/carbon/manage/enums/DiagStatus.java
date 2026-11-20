package com.carbon.manage.enums;

/**
 * 诊断状态：RUNNING-诊断中 SUCCESS-大模型成功 FALLBACK-大模型不可用走规则兜底 FAILED-彻底失败可重试。
 */
public enum DiagStatus {
    RUNNING,
    SUCCESS,
    FALLBACK,
    FAILED
}
