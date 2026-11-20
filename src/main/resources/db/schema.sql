-- =====================================================================
-- 企业碳资产智能监测系统 建表脚本
-- 数据库：carbon_manage（JDBC URL 已配置 createDatabaseIfNotExist=true）
-- 说明：全部使用 CREATE TABLE IF NOT EXISTS，应用启动时幂等执行
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 系统用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '用户昵称',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role        VARCHAR(20)  NOT NULL DEFAULT 'OPERATOR' COMMENT '角色：ADMIN-管理员 OPERATOR-整改负责人',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='系统用户表';

-- ---------------------------------------------------------------------
-- 2. 能源类型字典表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_energy_type
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    type_code   VARCHAR(20)  NOT NULL COMMENT '类型编码（大写，创建后不可修改）',
    type_name   VARCHAR(50)  NOT NULL COMMENT '类型名称，如：电力、天然气、蒸汽',
    unit        VARCHAR(20)  NOT NULL COMMENT '计量单位，如：kWh、m³、t',
    ratio_min   DECIMAL(4, 2) NOT NULL COMMENT '模拟采集目标排放占企业阈值的比例下限',
    ratio_max   DECIMAL(4, 2) NOT NULL COMMENT '模拟采集目标排放占企业阈值的比例上限',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_code (type_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='能源类型字典表';

-- ---------------------------------------------------------------------
-- 3. 企业信息表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_enterprise
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    enterprise_name VARCHAR(100)   NOT NULL COMMENT '企业名称',
    workshop_name   VARCHAR(100)   NOT NULL COMMENT '车间名称',
    industry        VARCHAR(50)    DEFAULT NULL COMMENT '所属行业',
    threshold_value DECIMAL(14, 4) NOT NULL COMMENT '单次采集碳排放阈值（kgCO2e），超过即判定超标',
    contact_person  VARCHAR(50)    DEFAULT NULL COMMENT '联系人',
    contact_phone   VARCHAR(20)    DEFAULT NULL COMMENT '联系电话',
    status          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='企业信息表';

-- ---------------------------------------------------------------------
-- 4. 企业能源类型配置表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_enterprise_energy_type
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    enterprise_id BIGINT      NOT NULL COMMENT '企业ID',
    energy_type   VARCHAR(20) NOT NULL COMMENT '能源类型编码，字典见 t_energy_type',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_enterprise_type (enterprise_id, energy_type),
    KEY idx_energy_type (energy_type),
    CONSTRAINT fk_eet_enterprise FOREIGN KEY (enterprise_id) REFERENCES t_enterprise (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='企业能源类型配置表';

-- ---------------------------------------------------------------------
-- 5. 碳排因子表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_emission_factor
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    energy_type    VARCHAR(20)    NOT NULL COMMENT '能源类型编码，字典见 t_energy_type',
    factor_name    VARCHAR(100)   NOT NULL COMMENT '因子名称',
    factor_value   DECIMAL(12, 6) NOT NULL COMMENT '碳排放因子（kgCO2e / 计量单位）',
    unit           VARCHAR(20)    NOT NULL COMMENT '计量单位：kWh、m³',
    effective_date DATE           NOT NULL COMMENT '生效日期',
    status         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    remark         VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    create_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_energy_type (energy_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='碳排放因子表';

-- ---------------------------------------------------------------------
-- 6. 能耗与碳排时序数据表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_energy_data
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    enterprise_id   BIGINT         NOT NULL COMMENT '企业ID（外键）',
    factor_id       BIGINT         NOT NULL COMMENT '核算所用碳排因子ID（外键）',
    energy_type     VARCHAR(20)    NOT NULL COMMENT '能源类型编码，字典见 t_energy_type',
    energy_value    DECIMAL(14, 2) NOT NULL COMMENT '能耗用量（单位取自 t_energy_type.unit）',
    emission_value  DECIMAL(14, 4) NOT NULL COMMENT '核算碳排放量（kgCO2e）',
    threshold_value DECIMAL(14, 4) NOT NULL COMMENT '采集时刻的企业阈值快照',
    is_exceeded     TINYINT        NOT NULL DEFAULT 0 COMMENT '是否超标：0-正常 1-超标',
    collect_time    DATETIME       NOT NULL COMMENT '采集时间',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
    KEY idx_ent_time (enterprise_id, collect_time),
    KEY idx_collect_time (collect_time),
    KEY idx_exceeded (is_exceeded),
    CONSTRAINT fk_energy_enterprise FOREIGN KEY (enterprise_id) REFERENCES t_enterprise (id),
    CONSTRAINT fk_energy_factor FOREIGN KEY (factor_id) REFERENCES t_emission_factor (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='能耗与碳排时序数据表';

-- ---------------------------------------------------------------------
-- 7. 减排整改工单表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_rectification_order
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no       VARCHAR(32)  NOT NULL COMMENT '工单号',
    enterprise_id  BIGINT       NOT NULL COMMENT '企业ID（外键）',
    energy_data_id BIGINT       NOT NULL COMMENT '触发的能耗数据ID（外键）',
    title          VARCHAR(200) NOT NULL COMMENT '工单标题',
    suggestion     TEXT COMMENT 'AI 减排建议快照',
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING_DISPATCH' COMMENT '状态：PENDING_DISPATCH/RECTIFYING/PENDING_AUDIT/COMPLETED',
    assignee       VARCHAR(50)  DEFAULT NULL COMMENT '整改负责人显示名（派发时快照）',
    assignee_id    BIGINT       DEFAULT NULL COMMENT '整改负责人用户ID（t_user.id）',
    rectify_result TEXT COMMENT '整改结果说明',
    audit_remark   VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    deadline       DATETIME     DEFAULT NULL COMMENT '整改截止时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_status (status),
    KEY idx_enterprise (enterprise_id),
    KEY idx_energy_data (energy_data_id),
    KEY idx_assignee_id (assignee_id),
    CONSTRAINT fk_order_enterprise FOREIGN KEY (enterprise_id) REFERENCES t_enterprise (id),
    CONSTRAINT fk_order_energy FOREIGN KEY (energy_data_id) REFERENCES t_energy_data (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='减排整改工单表';

-- ---------------------------------------------------------------------
-- 8. AI 诊断日志表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_ai_diag_log
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    energy_data_id  BIGINT       NOT NULL COMMENT '能耗数据ID（外键，唯一：同一条数据只诊断一次）',
    order_id        BIGINT       DEFAULT NULL COMMENT '自动生成的整改工单ID（外键）',
    model_name      VARCHAR(50)  DEFAULT NULL COMMENT '调用的本地大模型名称',
    prompt_snapshot TEXT COMMENT 'Prompt 快照',
    diagnosis       TEXT COMMENT '大模型诊断文本',
    suggestion      TEXT COMMENT '减排整改建议',
    status          VARCHAR(20)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/SUCCESS/FALLBACK/FAILED',
    error_msg       VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    duration_ms     BIGINT       DEFAULT NULL COMMENT '诊断耗时（毫秒）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_energy_data (energy_data_id),
    KEY idx_order (order_id),
    KEY idx_status (status),
    CONSTRAINT fk_diag_energy FOREIGN KEY (energy_data_id) REFERENCES t_energy_data (id),
    CONSTRAINT fk_diag_order FOREIGN KEY (order_id) REFERENCES t_rectification_order (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='AI 诊断日志表';
