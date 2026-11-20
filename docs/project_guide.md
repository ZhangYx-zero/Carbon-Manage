# Carbon-Manage 企业碳资产智能监测系统 · 项目指南

> 毕业设计项目文档：项目背景、技术方案、核心代码阅读指南、启动演示步骤与答辩问答。

---

## 一、项目背景与定位

在“双碳”（碳达峰、碳中和）战略背景下，中小制造企业普遍缺乏低成本的碳排放监测手段：人工抄表滞后、核算口径不统一、超标后缺少整改闭环。本系统面向该场景，构建一条完整业务闭环：

```
IoT 模拟采集能耗 ──► 按碳排因子核算排放量 ──► 超阈值判定
        ▲                                          │
        │                                          ▼
    定时任务循环                          异步触发本地大模型诊断
                                                   │
        已完成 ◄── 审核通过 ── 待审核 ◄── 提交 ── 整改中 ◄── 派发 ── 待派发 ◄── 自动生成整改工单
                                  │                ▲
                                  └──── 驳回 ──────┘（保留负责人，期限重置 3 天）
```

**与普通 CRUD 项目的本质区别**：

1. **有状态机**——工单只能按 `待派发 → 整改中 → 待审核 → 已完成` 流转，且审核不通过可退回整改中；非法跳转在 Service 层被拒绝，并发篡改用 CAS 拦住。
2. **有异步 AI 集成**——不是调公网 API，而是接入本地 Ollama 大模型，数据不出企业内网，零调用成本，且带规则兜底。
3. **有业务闭环与角色分工**——采集、核算、诊断、建单、整改、审核全部打通；管理员负责派发与审核，整改负责人只能看到并提交派发给自己的工单，越权访问在 Service 层返回 403；任一环节可在页面上演示。

---

## 二、技术栈

| 层次 | 技术 | 版本 | 选型理由 |
| --- | --- | --- | --- |
| 语言/运行时 | Java | 21 | LTS，支持 record/switch 模式匹配 |
| 后端框架 | Spring Boot | 3.5.16 | 最新稳定版，自动配置 |
| 数据访问 | MyBatis + Mapper XML | starter 3.0.5 | SQL 显式可控，便于展示索引与聚合能力 |
| 数据库 | MySQL | 8.0 | 事务、外键、窗口聚合 |
| AI 接入 | Spring AI + Ollama | BOM 1.0.9 | 官方抽象，`ChatClient` 统一调用本地模型 |
| 本地模型 | qwen2.5:1.5b（可切 8b/9b） | — | 1.5b 响应快（约 3~5s），答辩演示流畅 |
| 鉴权 | JWT (jjwt) + BCrypt | 0.12.6 | 无状态鉴权，密码不明文落库 |
| 定时任务 | Spring `@Scheduled` | — | 模拟 IoT 周期采集 |
| 异步线程池 | Spring `@Async` + 自定义 `aiExecutor` | — | AI 调用不阻塞采集主流程 |
| 前端 | Vue 3 + TypeScript + Vite | 3.4 / 5.5 / 5.4 | 组合式 API，构建快 |
| UI | Element Plus | 2.7 | 表格/表单/弹窗开箱即用 |
| 图表 | ECharts | 5.5 | 仪表盘趋势图/饼图/柱图 |
| 状态管理 | Pinia | 2.2 | 登录态持久化 |

---

## 三、项目结构

```
Carbon-Manage/
├─ pom.xml
├─ src/main/java/com/carbon/manage/
│  ├─ CarbonManageApplication.java     启动类（@EnableScheduling）
│  ├─ controller/                      9 个 REST 控制器（含 UserController / EnergyTypeController）
│  ├─ service/                         业务层（含 AI 诊断、PromptBuilder、UserService、EnergyTypeService）
│  ├─ mapper/                          9 个 MyBatis Mapper 接口
│  ├─ entity/                          8 张表对应的实体类
│  ├─ dto/                             LoginDTO / LoginVO / OrderActionDTO / DashboardVO
│  ├─ enums/                           OrderStatus / DiagStatus / UserRole（能源类型已改为字典表）
│  ├─ common/                          Result / PageResult / BusinessException / 全局异常 / JWT 鉴权 / UserContext 角色校验
│  ├─ config/                          拦截器注册 / CORS / 线程池 / 种子数据（含演示账号）
│  └─ task/                            EnergyCollectTask 定时采集
├─ src/main/resources/
│  ├─ application.yml                  数据源 / Ollama / MyBatis / 业务开关
│  ├─ db/schema.sql                    8 张表 DDL（启动时幂等执行）
│  └─ mapper/*.xml                     9 个 Mapper XML
├─ frontend/                           Vue3 前端（9 个页面，含登录页、个人中心与能源类型管理）
└─ docs/project_guide.md               本文档
```

---

## 四、数据库设计（8 张表）

| 表名 | 作用 | 关键设计 |
| --- | --- | --- |
| `t_user` | 系统用户 | 密码 BCrypt，`uk_username` 唯一；`nickname/phone/email` 供个人中心维护 |
| `t_energy_type` | 能源类型字典 | `uk_type_code` 唯一，编码创建后不可改；`ratio_min/ratio_max` 控制模拟采集比例；被引用时禁止删除，只可停用 |
| `t_enterprise` | 企业/车间档案 | `threshold_value` 为单次采集碳排阈值 |
| `t_enterprise_energy_type` | 企业能源类型配置 | `uk_enterprise_type` 唯一；企业只采集已配置的能源类型 |
| `t_emission_factor` | 碳排因子 | `energy_type` 唯一，走“最新有效因子” |
| `t_energy_data` | 能耗时序数据 | 排放量、阈值**双快照**；`idx_ent_time` 支撑按企业时间范围查询 |
| `t_rectification_order` | 整改工单 | `status` 受状态机约束；`assignee_id` 绑定负责人账号并做数据隔离；`uk_order_no` 防重号 |
| `t_ai_diag_log` | AI 诊断日志 | `uk_energy_data` 唯一索引实现“一条数据只诊断一次”的幂等 |

设计要点：

- **阈值与因子快照**：能耗表保存采集时刻的阈值与核算结果，企业后续改阈值不会污染历史数据。
- **能源类型字典化**：能源类型不再写死在枚举里，管理员可在「能源类型」页面新增/编辑/停用（如蒸汽 STEAM），采集任务、企业配置、碳排因子、仪表盘与前端下拉全部从 `t_energy_type` 读取，新增类型无需改代码。编码在业务表（`t_energy_data` 等）中按字符串冗余保存，历史数据不随字典调整而漂移；被企业配置/因子/能耗数据引用的类型禁止删除，只允许停用。
- **按企业配置能源类型**：`t_enterprise_energy_type` 与 `t_enterprise` 一对多（`uk_enterprise_type` 防重），采集任务只遍历企业已配置的类型，支持“只采集电力 / 只采集天然气 / 只采集蒸汽 / 任意组合”的场景。
- **幂等索引**：`t_ai_diag_log.uk_energy_data` 让“插入 RUNNING 占位”天然防重复诊断，捕获 `DuplicateKeyException` 即可跳过。
- **外键 + 状态索引**：工单按状态分页、按企业统计都有索引支撑。
- **责任到人与数据隔离**：工单派发时写入 `assignee_id`（负责人账号 ID）与 `assignee`（显示名快照）；权限判断只看 `assignee_id`，负责人的分页查询在 SQL 层追加 `assignee_id = 当前用户` 条件，待派发单因 ID 为空天然不可见。

完整 DDL 见 `src/main/resources/db/schema.sql`（启动自动执行），也可直接复制到 DBeaver 手工建库。

> 已有库升级：若 `t_user` 还是旧版本，需补两列个人资料字段：
>
> ```sql
> ALTER TABLE t_user
>     ADD COLUMN phone VARCHAR(20)  DEFAULT NULL COMMENT '手机号' AFTER nickname,
>     ADD COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER phone;
> ```
>
> 企业能源类型配置表由 `schema.sql` 幂等创建；旧企业若没有任何类型配置，启动时 `DataInitializer.ensureEnterpriseEnergyTypes()` 会自动补上“已启用且有启用因子的能源类型”，保证升级后采集行为不变。
>
> 能源类型字典表 `t_energy_type` 同样由 `schema.sql` 幂等创建；表为空时 `DataInitializer.initEnergyTypes()` 会种入“电力 / 天然气”两条基础数据（电力 kWh、比例 0.65~0.85；天然气 m³、比例 0.45~0.65），已有字典则不再覆盖，避免把管理员删除的类型重新种回来。

---

## 五、核心模块实现

### 5.1 数据采集与碳排核算 —— `task/EnergyCollectTask.java`

- `@Scheduled(fixedRateString = "${carbon.collect.interval-ms:30000}")` 默认每 30 秒采集一轮，`carbon.collect.enabled=false` 可关闭（压测或讲解时用）。
- 每轮遍历启用企业，按 `t_enterprise_energy_type` 中配置的类型逐条生成数据；类型必须存在字典且启用、且已配置启用因子，否则跳过。能耗值按“目标排放 = 阈值 × 比例”反推，比例取自 `t_energy_type.ratio_min/ratio_max` 的随机区间，并带 10% 概率尖峰，保证演示时能稳定触发超标；新增类型只需在字典配置比例并录入因子。
- 核算公式：`emission = energyValue × factorValue`，`BigDecimal` 保留 4 位小数（`HALF_UP`），避免浮点误差。
- **事务边界**：`energyDataService.create(data)` 是一个独立事务，提交成功后才调用 AI 触发逻辑，异步线程必然能读到数据。
- 同时提供 `POST /api/energy/collect` 手动采集接口，答辩现场不用等定时任务。

### 5.2 AI 异步诊断 —— `service/` 包（AI 相关）

调用链：

```
EnergyCollectTask.collectOnce()
  └─ AiDiagnosisService.claimAndTrigger(energyDataId)   // 插入 RUNNING 占位（幂等）
       └─ AiDiagnosisAsyncService.diagnose(logId)       // @Async("aiExecutor") 真异步
            ├─ PromptBuilder 构建三段式 Prompt
            ├─ ChatClient → 本地 Ollama（qwen2.5:1.5b）
            └─ AiDiagPersistService.saveSuccess()       // 事务内：更新日志 + 自动建单
```

实现要点（答辩重点）：

1. **幂等占位**：先插 `t_ai_diag_log(status=RUNNING)`，靠唯一索引兜底，重复触发直接返回 `null`。
2. **@Async 自调用陷阱**：异步方法放在独立的 `AiDiagnosisAsyncService` Bean 中，避免同类调用绕过 Spring 代理。
3. **线程池隔离**：`config/AsyncConfig.java` 定义 `aiExecutor`（core 2 / max 4 / queue 100 / CallerRunsPolicy），AI 慢请求不会拖垮采集线程。
4. **规则兜底**：Ollama 不可用、超时、返回空时，`PromptBuilder.fallbackSuggestion()` 按能源类型输出规则建议（电力：空压机/空调/设备空转/错峰；天然气：管网泄漏/燃烧不充分/计量异常；字典中的其他类型走含类型名称的通用模板），状态记 `FALLBACK`，**工单照常生成**——保证业务闭环不因 AI 失败而中断。
5. **只诊断超标数据**：`claimAndTrigger` 与 `retry` 都会校验 `is_exceeded=1`，正常数据直接返回 400；前端正常行显示“无需诊断”，不暴露按钮，避免把正常波动误判成异常并生成无意义的整改工单。
6. **重试**：`FAILED` 状态可通过 `POST /api/diagnosis/{id}/retry` 重置为 `RUNNING` 后重新异步执行。

Prompt 设计（`service/PromptBuilder.java`）：System 角色定位“企业碳排诊断专家”，固定输出【异常原因】【可疑排放源】【整改建议】三段，限 200 字；User 注入企业、车间、时间、能源类型、用量、因子、排放量、阈值、超标比例，让模型有充分上下文。

模型切换只需改 `application.yml`：

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen2.5:1.5b   # 可换 llama3.1:8b / qwen3.5:9b
```

### 5.3 工单状态机与角色闭环 —— `enums/OrderStatus.java` + `service/RectificationOrderService.java`

允许转移关系集中定义在枚举内部（四条合法流转，另有一条终态）：

```java
PENDING_DISPATCH → RECTIFYING          // 派发（管理员，指定 assignee_id）
RECTIFYING       → PENDING_AUDIT       // 提交整改（该工单负责人）
PENDING_AUDIT    → COMPLETED           // 审核通过（管理员，必填审核意见）
PENDING_AUDIT    → RECTIFYING          // 审核驳回（管理员，必填驳回原因，期限重置 3 天）
COMPLETED        → （无出边）
```

四个公开入口 `dispatch / submit / auditPass / auditReject` 都先做角色校验（`UserContext.requireRole`），再统一委托给私有的 `transition(id, expectedFrom, target, ...)`。`transition` 做三层校验：

1. **起始状态比对**：`from != expectedFrom` → `400 当前状态为X，无法执行该操作`，用于区分“派发”与“驳回”两条进入整改中的边；
2. **合法性**：`from.canGoTo(target)` 不通过 → `BusinessException(400, "非法状态流转：...")`；
3. **并发安全**：`UPDATE ... WHERE id=? AND status=?`（CAS），影响行数为 0 说明状态已被他人修改 → `409 请刷新后重试`。

权限与字段约束：

- 派发/审核通过/审核驳回：仅 `ADMIN`；提交整改：仅 `OPERATOR` 且必须是该工单的 `assignee_id`；
- 派单必须选择存在、角色为 `OPERATOR` 且启用的账号，显示名由后端读取，不接受前端传入；
- 提交必须有整改结果，审核通过必须有审核意见，驳回必须有驳回原因；
- `changeStatusCas` 只更新非空字段——提交、审核、驳回都不传 `assigneeId/assignee`，因此驳回后负责人保持不变。

四个公开入口均标注 `@Transactional`，任一步失败全部回滚。

### 5.4 JWT 登录鉴权 —— `common/`（JwtInterceptor/JwtUtil/UserContext）+ `service/AuthService`

- `JwtInterceptor` 拦截 `/api/**`，排除 `/api/auth/login`，放行 OPTIONS 预检；
- 解析 `Authorization: Bearer <token>`，写入 `UserContext`（ThreadLocal），请求结束清理，防止线程池串号；
- 未登录统一返回 `Result{code:401}`，前端拦截器捕获后清空本地登录态并跳转登录页；
- JWT 中携带 `role`，`UserContext.requireRole(UserRole.ADMIN/OPERATOR)` 在 Service 层做角色校验，越权返回 `Result{code:403}`；
- 前端路由守卫按 `meta.roles` 隐藏并拦截“企业管理 / 碳排因子 / 能源类型”页面，但真正的权限边界仍在后端；
- 密码用 BCrypt 存储，`User.password` 标注 `@JsonIgnore` 防止序列化泄露。

### 5.5 个人中心与账号管理 —— `service/UserService.java` + `views/ProfileView.vue`

- **页面入口**：侧边栏与右上角用户菜单均可进入“个人中心”，管理员和整改负责人都可见。
- **个人信息**：只允许修改昵称、手机号、邮箱，登录账号和角色由系统维护；保存后同步 Pinia 与 localStorage，顶部问候语即时刷新。
- **修改密码**：必须校验原密码，新密码 6~32 位且不能与原密码相同；后端用 BCrypt 重新哈希后更新 `t_user.password`，成功后前端清除登录态并跳转登录页。
- **账号管理（仅管理员）**：可新增整改负责人，也可再注册一名管理员；账号唯一性、角色合法性、手机号/邮箱格式均在 Service 层校验，初始密码同样 BCrypt 入库。
- **权限边界**：账号列表与新增接口调用 `UserContext.requireRole(UserRole.ADMIN)`；资料与密码接口只使用 JWT 中的当前用户 ID，不接收前端传入的 userId，防止越权修改他人账号。

### 5.6 仪表盘聚合 —— `dashboard/DashboardMapper.xml`

四条统计 SQL + 三条图表 SQL（24 小时分时排放、能源类型占比、工单状态分布），全部在 SQL 层聚合，避免把明细拉回 Java 再计算。

---

## 六、核心代码阅读顺序（答辩前建议 2 小时过一遍）

| 顺序 | 文件 | 看什么 | 可以怎么改（防问“如果让你改…”） |
| --- | --- | --- | --- |
| 1 | `task/EnergyCollectTask.java` | 定时任务、能耗生成策略、事务后触发 | 改采样频率、在能源类型页配置新类型、接入真实 MQTT |
| 2 | `service/AiDiagnosisService.java` | 幂等占位、触发异步 | 换触发条件（连续 N 次超标） |
| 3 | `service/AiDiagnosisAsyncService.java` | @Async 独立 Bean、异常兜底 | 换模型、增加多轮对话 |
| 4 | `service/PromptBuilder.java` | Prompt 工程、规则兜底文案 | 调整输出结构、增加行业模板 |
| 5 | `service/AiDiagPersistService.java` | 事务内落库 + 自动建单 | 增加通知（钉钉/邮件） |
| 6 | `enums/OrderStatus.java` | 四条流转的状态转移表 | 增加“挂起/撤销”状态 |
| 7 | `service/RectificationOrderService.java` | 角色校验 + 归属校验 + 状态机 + CAS | 增加操作日志表、多级审批 |
| 8 | `common/JwtInterceptor.java`、`common/UserContext.java` | ThreadLocal 上下文、`requireRole` | 增加组织架构与更细粒度 RBAC |
| 9 | `resources/db/schema.sql` | 索引与唯一约束设计 | 加分表/归档策略 |

---

## 七、接口清单

统一响应：`{ "code": 200, "message": "success", "data": ... }`；除登录外均需 `Authorization: Bearer <token>`。

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 认证 | POST | `/api/auth/login` | 登录，返回 token |
| 认证 | GET | `/api/auth/me` | 当前用户 |
| 能源类型 | GET | `/api/energy-types` | 全量字典（含停用），供前端名称/单位映射，登录即可 |
| 能源类型 | GET | `/api/energy-types/list` | 仅启用类型，供新增下拉 |
| 能源类型 | POST/PUT/DELETE | `/api/energy-types`、`/api/energy-types/{id}` | 字典维护，管理员；编码创建后不可改，被引用时禁止删除 |
| 企业 | GET/POST/PUT/DELETE | `/api/enterprises`、`/api/enterprises/{id}` | 企业 CRUD，请求体 `energyTypes: ["ELECTRICITY","GAS"]` 配置采集类型（须为字典中的启用类型） |
| 企业 | GET | `/api/enterprises/list` | 启用企业下拉 |
| 因子 | GET/POST/PUT/DELETE | `/api/factors`、`/api/factors/{id}` | 因子 CRUD，单位须与能源类型字典一致 |
| 能耗 | GET | `/api/energy/page` | 分页（企业/能源/是否超标） |
| 能耗 | POST | `/api/energy/collect` | 手动触发采集 |
| 诊断 | GET | `/api/diagnosis/page`、`/{id}` | 诊断日志 |
| 诊断 | POST | `/api/diagnosis/trigger/{energyDataId}` | 手动触发诊断 |
| 诊断 | POST | `/api/diagnosis/{id}/retry` | 失败重试 |
| 工单 | GET | `/api/orders/page`、`/{id}` | 工单分页/详情 |
| 工单 | POST | `/api/orders/{id}/dispatch` | 派单，管理员（→整改中，指定 `assigneeId`） |
| 工单 | POST | `/api/orders/{id}/submit` | 提交整改，负责人（→待审核） |
| 工单 | POST | `/api/orders/{id}/audit/pass` | 审核通过，管理员（→已完成） |
| 工单 | POST | `/api/orders/{id}/audit/reject` | 审核驳回，管理员（→整改中，重置期限） |
| 用户 | GET | `/api/users` | 账号列表，管理员（个人中心账号管理） |
| 用户 | POST | `/api/users` | 新增整改负责人或管理员账号，管理员 |
| 用户 | PUT | `/api/users/profile` | 修改本人昵称/手机号/邮箱 |
| 用户 | PUT | `/api/users/password` | 校验原密码后修改本人密码（BCrypt 入库） |
| 用户 | GET | `/api/users/operators` | 启用的整改负责人列表，管理员（派单下拉） |
| 仪表盘 | GET | `/api/dashboard/stats` | 统计卡 + 三张图数据 |

---

## 八、启动与演示

### 8.1 环境要求

- JDK 21、Maven 3.9+、Node 18+、MySQL 8.0（root/123456，或改 `application.yml`）
- Ollama 运行在 `http://localhost:11434`，且已 `ollama pull qwen2.5:1.5b`

### 8.2 启动后端

```bash
cd Carbon-Manage
mvn spring-boot:run
```

首次启动会自动：建库 `carbon_manage` → 建 8 张表 → 写入种子数据（管理员 admin/123456、整改负责人 operator1/123456 王工与 operator2/123456 李工、能源类型字典（电力 / 天然气）、2 家企业（默认配置启用类型）、2 个碳排因子、近 24 小时历史能耗）。

### 8.3 启动前端

```bash
cd Carbon-Manage/frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`，使用 **admin / 123456** 登录（管理员）；需要演示负责人视角时用 **operator1 / 123456** 登录。

### 8.4 答辩演示动线（约 6 分钟）

1. **管理员登录** → 展示 JWT 鉴权与仪表盘实时数据；
2. **碳排因子 / 企业管理** → 说明核算口径与阈值来源（并说明写操作仅管理员可执行）；
3. **实时监测** → 点“手动采集”，出现超标红色 Tag，点该行“AI 诊断”；正常行显示“无需诊断”，说明诊断只面向超标数据；
4. **AI 诊断** → 展示 Prompt 快照与本地大模型输出（约 3~5 秒），强调数据不出内网；
5. **管理员派单** → 在整改工单页点“派单”，从下拉中选择“王工（operator1）”，设置截止时间；
6. **负责人视角** → 退出并用 operator1 登录：菜单无企业管理/碳排因子，工单页只显示自己的单，点“提交整改”；
7. **管理员审核** → 重新用 admin 登录，在“待审核”工单上演示“不通过（退回整改）”，说明期限重置与驳回原因可见；再次提交后演示“通过”，工单变为已完成，双方都可见；
8. **个人中心** → 管理员在“账号管理”新增一名负责人或管理员；任意角色在“个人信息”修改昵称，在“修改密码”改密后重新登录，说明密码以 BCrypt 哈希同步写入数据库；
9. **加分演示**：停掉 Ollama 再采集，展示 `规则兜底` 状态与工单照常生成；用 curl 直接调“待派发 → 已完成”展示 400 拒绝；用 operator1 调派单接口展示 403。

---

## 九、答辩常见问题与参考回答

**Q1：为什么用原生 MyBatis 而不是 MyBatis-Plus？**
A：本项目 SQL 是展示重点——动态条件分页、聚合统计、CAS 更新都需要显式控制。原生 MyBatis + XML 让索引使用和 SQL 语义一目了然，也避免 ORM 隐藏细节；MyBatis-Plus 更适合以增删改查为主的管理系统。

**Q2：状态机怎么防止乱序/篡改？**
A：三层防护。第一层角色与归属校验——派发/审核仅管理员，提交仅该工单负责人；第二层是起始状态比对 + 枚举转移表——`transition` 先锁 `expectedFrom` 区分“派发”与“驳回”两条进入整改中的边，再用 `canGoTo` 查白名单，非法跳转直接 400；第三层是 CAS：`UPDATE ... WHERE id=? AND status=?`，若两人同时操作同一工单（例如一个通过、一个驳回），后到者影响行数为 0，返回 409 提示刷新，绝不出现越级或回退的脏写。

**Q3：@Async 有什么坑？**
A：三个。①同类自调用不走代理，异步失效——所以单独拆了 `AiDiagnosisAsyncService`；②异步线程读不到未提交事务的数据——所以采集事务提交后才触发；③默认 `SimpleAsyncTaskExecutor` 无上限——所以自定义 `aiExecutor` 限流，并配 `CallerRunsPolicy` 防止任务丢失。

**Q4：怎么保证同一条超标数据不被重复诊断？**
A：`t_ai_diag_log.energy_data_id` 上建唯一索引，触发时先插入 `RUNNING` 占位，并发重复插入会抛 `DuplicateKeyException`，捕获后直接跳过。这是数据库层的强幂等，比先查后插更可靠。

**Q5：Ollama 挂了系统还能用吗？**
A：能。诊断异常时走规则兜底，按能源类型输出模板化整改建议，日志状态记 `FALLBACK`，工单照常生成，整条业务闭环不断。失败记录可人工重试。这是本系统面向企业场景的可用性设计。

**Q6：为什么用 Spring AI 而不是直接调 HTTP？**
A：Spring AI 提供 `ChatClient` 统一抽象，切换模型厂商（Ollama/OpenAI/通义）只改配置不改代码；还统一处理超时、重试、流式输出等细节。

**Q7：BigDecimal 精度怎么处理的？**
A：能耗 × 因子全程 `BigDecimal`，中间结果不转 double，最终统一 `setScale(4, HALF_UP)` 落库，避免 0.1+0.2 类浮点误差在碳排放核算中被放大。

**Q8：索引是怎么设计的？**
A：能耗表 `(enterprise_id, collect_time)` 联合索引支撑“按企业 + 时间范围”查询与仪表盘 24 小时聚合；单列 `collect_time`、`is_exceeded` 分别支撑全量时间排序和超标筛选；工单表按 `status`、`enterprise_id` 建索引；诊断日志 `energy_data_id` 唯一索引兼顾幂等与关联查询。

**Q9：JWT 和 Session 怎么选？**
A：前后端分离场景下 JWT 无状态、易水平扩展；种子密码 BCrypt 加密；登出由前端清除 token，后续可加 Redis 黑名单做服务端失效。

**Q10：项目还能扩展什么？**
A：接入真实 IoT 协议（MQTT/Modbus）、引入时序库（TDengine/InfluxDB）、增加操作审计日志、工单消息通知、更细粒度的 RBAC（组织架构、多级审批、按钮级授权）、碳配额与碳交易模拟等。

---

## 十、配置项速查（`application.yml`）

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `carbon.collect.enabled` | true | 定时采集开关 |
| `carbon.collect.interval-ms` | 30000 | 采集周期 |
| `carbon.ai.enabled` | true | 超标后是否自动触发 AI |
| `spring.ai.ollama.chat.options.model` | qwen2.5:1.5b | 本地模型名 |
| `spring.ai.ollama.chat.options.temperature` | 0.3 | 降低发散，输出更稳定 |
| `app.jwt.secret` | （32 字节以上） | 生产环境务必改为环境变量 |
| `app.jwt.expire-hours` | 12 | token 有效期 |
