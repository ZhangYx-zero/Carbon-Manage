package com.carbon.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业碳资产智能监测系统启动类。
 * <p>核心闭环：定时采集能耗 -> 核算碳排放 -> 超阈值触发 AI 诊断 -> 自动生成整改工单 -> 状态机流转归档。</p>
 */
@EnableScheduling
@SpringBootApplication
public class CarbonManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarbonManageApplication.class, args);
    }
}
