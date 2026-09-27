# 城市无主井盖认领巡检维修与赔付协同平台

市民上报井盖问题（缺失/松动/异响/塌陷）后，平台沿两条线协同处置：

- **应急处置线**（只看现场风险与到场时效）：事件推送街道值班员 → 受理派发 → 设置警示锥/临时盖板/夜间反光标识 → 记录费用并标记**责任待定**。
- **权属追责线**（看历史移交、施工单位、管线单位与日常巡检证据）：按井盖标识、地下管线、历史施工、周边商户、城管台账排查权属 → 权属单位认领/被确认 → 派发维修工单 → 维修队登记到场时间、井室状况、井盖规格、封路与燃气/电力/通信专业协同 → 城管巡查员按照片、定位、路面平整度、车辆碾压测试验收 → 结案。

街道可以先把危险点处理掉，后续再根据复核结果分摊费用，避免因归属不清让风险长期留在路面。若已造成人车损伤，赔付专员依据上报时间、临时围护时效、权属确认耗时、维修时长与事故证明判定责任。平台沉淀高发路段、权属争议单位、维修超时与赔付金额，用于调整日常巡检路线和施工移交规则。

## 技术栈

- 前端：Vue 3 + Vite + Element Plus + ECharts + Axios + Vue Router
- 后端：Spring Boot 3.2（Web / Data JPA / Validation），Token 鉴权
- 数据库：PostgreSQL 15（本地实例，端口 5433）

## 目录结构

```
├── backend/            # Spring Boot 后端（端口 8080）
│   └── src/main/java/com/city/manhole/
│       ├── entity/         # 用户、权属单位、事件、应急、排查、工单、验收、赔付、费用、日志
│       ├── controller/     # 事件/应急/权属/工单/验收/赔付/费用/统计/元数据/文件接口
│       ├── repository/     # Spring Data JPA
│       └── config/         # 鉴权拦截器、CORS、数据种子（测试账号+示例事件）
├── frontend/           # Vue 3 前端（端口 5173，代理 /api 与 /uploads 到 8080）
├── scripts/start-db.sh # 本地 PostgreSQL 初始化与启动（端口 5433）
├── start.sh            # 一键启动（数据库+后端+前端）
├── stop.sh             # 停止后端与前端
└── e2e-test.sh         # 端到端业务流验证脚本（28 项断言，可重复执行）
```

## 本地启动

前置依赖：JDK 17、Maven 3.8+、Node 18+、PostgreSQL 15 服务端二进制（本机已装，位于 `/usr/lib/postgresql/15/bin`）。

### 方式一：一键启动

```bash
bash start.sh
```

### 方式二：分步启动

```bash
# 1. 数据库（首次自动 initdb 到 .pgdata/，端口 5433，自动建库 manhole 与账号 manhole_app/manhole123）
bash scripts/start-db.sh

# 2. 后端（首次启动自动建表并写入种子数据）
cd backend && mvn spring-boot:run
# 或打包后运行：mvn package -DskipTests && java -jar target/manhole-platform-1.0.0.jar

# 3. 前端
cd frontend && npm install && npm run dev
```

访问：前端 http://localhost:5173 ，后端 http://localhost:8080 。

## 测试账号

| 账号 | 密码 | 角色 | 职责 |
|---|---|---|---|
| citizen1 | 123456 | 市民 | 上报井盖问题、查看我的上报 |
| street1 | 123456 | 街道值班员 | 受理派发、临时围护登记、权属排查、费用复核分摊 |
| drain / water / gas / power / telecom | 123456 | 权属单位（排水/水务/燃气/电力/通信） | 认领事件、派发维修工单 |
| repair1 / repair2 | 123456 | 维修队 | 接收工单、登记维修信息、完工 |
| inspector1 | 123456 | 城管巡查员 | 验收（照片/定位/平整度/碾压测试） |
| comp1 | 123456 | 赔付专员 | 赔付立案、证据链查看、责任判定、确认赔付 |
| admin | admin123 | 管理员 | 全部功能 + 单位/账号管理 + 统计分析 |

## 验证方式

### 1. 端到端业务流（已实际执行，28/28 通过）

```bash
bash e2e-test.sh
```

脚本按真实角色依次调用 API 并断言：市民上报 → 街道派发 → 临时围护（费用责任待定）→ 权属排查（结论：责任待定）→ 燃气公司认领 → 派发工单 → 维修队接收/登记（封路+燃气协同）/完工 → 巡查员验收通过 → 结案 → 赔付立案/证据链/判定/赔付 → 应急费用复核分摊给燃气公司 → 统计沉淀校验。脚本基于基线断言，可重复执行。

### 2. 构建与运行验证（已实际执行）

- `cd backend && mvn compile` / `mvn package -DskipTests`：编译打包通过；
- `java -jar target/manhole-platform-1.0.0.jar`：独立启动成功，登录与统计接口返回正常；
- `cd frontend && npm run build`：Vite 构建通过；
- `npm run dev`：页面可访问，`/api` 代理到 8080 正常（curl 验证登录接口）。

### 3. 页面操作验证路径

登录各角色账号后可在页面上完成同样流程：市民在「市民上报」提交；街道值班员在「事件中心 → 事件详情」执行派发/围护/排查；权属单位在详情页认领并派单；维修队在「维修工单」或详情页处理；巡查员在详情页工单行验收；赔付专员在「赔付案件」查看证据链并判定；管理员/街道在「费用复核」分摊待定费用；「统计分析」查看高发路段、权属争议单位、维修超时与赔付分布。

## 主要接口

| 接口 | 说明 |
|---|---|
| POST /api/auth/login | 登录获取 Token（请求头 X-Token 鉴权） |
| POST /api/events | 市民上报（位置/路段/道路类型/照片/损伤/危险程度） |
| POST /api/events/{id}/dispatch、/secure | 应急处置：派发、临时围护（费用责任待定） |
| POST /api/events/{id}/investigations、/claim | 权属排查（五类线索）、权属单位认领 |
| POST /api/events/{id}/work-orders、/api/work-orders/{id}/accept、/report、/finish | 工单派发、接收、维修信息登记、完工 |
| POST /api/work-orders/{id}/acceptance | 巡查验收（照片/定位/平整度/碾压测试） |
| POST /api/events/{id}/compensations、/api/compensations/{id}/decide、/pay | 赔付立案、责任判定、确认赔付 |
| GET /api/compensations/{id} | 责任判定证据链（围护时效/权属耗时/维修时长自动汇总） |
| POST /api/costs/{id}/allocate | 费用复核分摊（权属单位/街道/多方） |
| GET /api/stats/overview、/hot-roads、/disputed-units、/repair-timeouts、/compensations | 统计沉淀 |

## 说明

- 应急处置 SLA 120 分钟、维修 SLA 48 小时，可在 `backend/src/main/resources/application.yml` 调整。
- 上传照片保存于后端 `uploads/` 目录，经 `/uploads/**` 访问。
- 种子数据含 6 个不同状态的示例事件，便于登录后直接查看统计看板与各环节页面。
