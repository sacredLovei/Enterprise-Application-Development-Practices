# 项目状态与记忆（STATE）

> 本文件是**唯一权威项目记忆**。一切"之前干了什么"以本文件为准，不依赖对话上下文。
> 规则见 `.dsh/skills/project-governance/SKILL.md`：本文件只增不改；决策永不删除只能取代；口径改动必须先改本注册表再改引用点。
> 本文件更新：2026-09-11（最近更新随时间线最新条目）

---

## 1. 当前状态摘要

| 项 | 值 |
|---|---|
| 项目 | 无人机-机器狗空地协同巡检集成平台（选题 1 园区安防，课程：企业应用开发实践） |
| 当前步骤 | **S20 Docker 六组件环境**（in_progress：部署配置 + 运行验收） |
| 最近完成 | S19 开发环境准备（四项验收全过，提交 b396607） |
| 进行中事项 | 无 |
| 当前版本 | 里程碑 v0.1；S19 开始标记提交 2199642；JDK 口径调整提交 3ebe329 |
| 工作树状态 | 干净（随每次提交保持） |

## 2. 版本表

| 版本/tag | 提交号 | 内容摘要 | 日期 |
|---|---|---|---|
| （无 tag） | 3056752 | 基线快照 v0.0：4 份课程 docx + .gitignore + 设计报告初稿 | 2026-09-11 |
| （无 tag） | 7a0e97f | chore：.gitignore 增加 `~$*` 锁文件规则 | 2026-09-11 |
| （无 tag） | 35ec3d7 | chore：移出索引中的 2 个 Word 临时锁文件 | 2026-09-11 |
| v0.1 | 3056752 | 里程碑：选题一设计方案（报告第 2~6 章）定稿 | 2026-09-11 |
| （S02） | dbb68f8 | 治理体系：PLAN/STATE/skill project-governance/修订记录表 | 2026-09-11 |
| （S19 开始） | 2199642 | 计划调整：插入 S19/S21、激活 S20、冻结 S10/S11；S19 开始 | 2026-09-11 |
| （S19 中） | 3ebe329 | 口径调整：JDK 17→21（本机 DevEco JBR 21.0.6 → tools/jdk-21）；设计报告升版 v0.2 | 2026-09-11 |
| （S19 完成） | 本步提交（HEAD） | 环境四项验收全过：JDK21 / Docker 引擎运行（用户目视确认）/ WSL / 磁盘 50GB | 2026-09-11 |

## 3. 变更时间线（只增不改）

| 时间 | 提交号 | 内容 | 执行者 |
|---|---|---|---|
| 2026-09-08 前后 | —（未入库） | 用户收集 4 份课程 docx（指导说明书/任务书/报告模板/测试用例样例） | 用户 |
| 2026-09-11 | 3056752 | git init；基线快照入库（含 S00 材料与 S01 设计报告 v0.1） | AI |
| 2026-09-11 | 7a0e97f | 发现基线误含 2 个 Word 锁文件（`~$业应用开发实践》项目任务书.docx`、`~$应用开发实践课程设计报告.docx`，为 Word 打开期间自动生成）；.gitignore 增加 `~$*` | AI |
| 2026-09-11 | 35ec3d7 | 将上述锁文件移出索引（磁盘文件保留，因 Word 可能仍占用） | AI |
| 2026-09-11 | HEAD（本步） | S02：创建 PLAN.md / STATE.md / skill / 设计报告修订记录表 | AI |
| 2026-09-11 | 2199642 | 计划调整（用户确认）：文档步骤 S10/S11 冻结；插入 S19（环境准备）与 S21（最小业务链路）；激活 S20（六组件环境）；原 S30 拆分为 S30（后端全量）+ S31（仿真全量）。同时记录探测结论：本机 Docker/WSL2 未安装、JAVA_HOME 指向 JDK 1.8.0_151（与设计要求的 JDK 17 不符） | AI |
| 2026-09-11 | 3ebe329 | S19 进展：口径调整 JDK 17→21（本机 DevEco JBR 21.0.6 复制至 tools/jdk-21，java/javac 验证通过）；下载 JDK 不可行（沙箱 TLS 凭据不可用 + 镜像不通），记录于风险表；设计报告 5.1.2/4.3.1/技术栈基线同步更新并升版 v0.2 | AI |
| 2026-09-11 | 本步（HEAD） | 一致性核对（应用户要求）：全量检索确认 JDK 17 表述已全部替换为 21；修正 STATE 当前步骤行残留"JDK17"字样；标注 D-2 的 JDK 版本部分被 D-13 细化；回填各条时间线/版本表提交号 | AI |
| 2026-09-11 | 本步（HEAD） | S19 进展（用户侧）：用户执行 `wsl --install`——WSL 2.7.13 与内核 6.18.33 安装成功（AI 侧实测确认），Ubuntu 发行版因 GitHub DNS 解析失败未下载（项目不需要，Docker Desktop 自带发行版）；Docker Desktop 安装进行中。据此登记风险 #9 与 Docker 镜像加速预案 | AI |
| 2026-09-11 | 本步（HEAD） | S19 验收初核：① JDK21 ✅ ② Docker CLI 29.7.2 已装、引擎运行状态待用户目视确认（沙箱无法访问 named pipe，风险 #11） ③ WSL ✅ ④ 内存 15.7GB ✅ / **磁盘 C 空闲仅 15.8GB ❌**。据此登记 D-14（WSL 内存 8GB）、D-15（S20 开工前置：C 盘 ≥30GB）、风险 #10（磁盘不足与清理预案） | AI |
| 2026-09-11 | 本步（HEAD） | S19 收尾：用户确认 Docker 引擎已启动（鲸鱼变绿）并完成磁盘清理，C 盘空闲 15.8 → **50 GB**；验收标准①②③④全部通过，S19 置 done，风险 #10/#11 销项 | AI |
| 2026-09-11 | 本步（HEAD） | S20 开始（六组件环境）：即将产出 docker-compose.yml、组件配置、初始化脚本 | AI |
| 2026-09-11 | 本步（HEAD） | S20 运行受阻与应对：`docker compose up -d` 因 Docker Hub DNS 污染失败（registry-1.docker.io → 127.0.0.1）；实测镜像源四通一不通（见风险 #12），elastic 官方源直连成功（ES 8.13.0 已拉取）；登记 D-16，产出 `docker/init/pull-images.ps1` | AI |
| 2026-09-11 | 本步（HEAD） | S20 交接（用户决定）：AI 侧后台拉取速度偏慢，取消之；镜像拉取改由**用户在本机系统终端**执行 `docker/init/pull-images.ps1`（含镜像源与重标记逻辑）。镜像就绪后由 AI 继续 compose 启动 + 初始化 + 验收 | AI |
| 2026-09-11 | 本步（HEAD） | S20 口径变更（Kafka 换镜像，D-18）：实测 bitnami/kafka:3.6 与 4.1 在 Docker Hub 均 404（Bitnami 已下架公开 Kafka 镜像），用户直连拉取 apache/kafka:4.0.0 成功；compose/脚本/README/设计报告（v0.3）/注册表全部同步 | AI |

## 4. 决策记录（永不删除，只可被新决策取代）

| 编号 | 决策 | 理由 | 状态 |
|---|---|---|---|
| D-1 | 选题 1：园区安防空地协同巡检集成平台 | 推荐基础题，仿真逻辑最简、闭环完整 | 有效 |
| D-2 | 技术栈：Kafka 3.6(KRaft) + MongoDB 6.0 + Spring Boot 3.2(Java17) + Vue 3 | 用户 2026-09-11 选定推荐组合；论证见设计报告 2.3.3 / 4.3.2 | 有效（JDK 版本部分被 D-13 细化为 21；Kafka 版本/镜像部分被 D-18 取代） |
| D-3 | 文档先 Markdown 后转 Word | 便于反复修改与补截图，定稿再转 | 有效 |
| D-4 | 后端为单体多实例（不引入微服务框架） | 课程考察中间件集成而非服务治理；无状态多实例足以演示负载均衡（设计报告 4.2.2） | 有效 |
| D-5 | MongoDB 为权威数据源，ES 为检索副本；允许秒级不一致，由对账补偿 | 保证告警不因检索组件抖动而丢失（设计报告 2.5.2 / 5.2.2） | 有效 |
| D-6 | Kafka 双监听器部署：容器内 `kafka:9092`，宿主机 `localhost:9094` | 规避 advertised.listeners 地址回传问题（设计报告 4.6.3） | 有效 |
| D-7 | 文件访问用 WebHDFS REST，不用 hadoop-client | 避免传递依赖冲突（设计报告 5.2.3） | 有效 |
| D-8 | HDFS 副本数 = 2；ES 单节点 `number_of_replicas=0` | 单机仿真资源与正确性权衡（设计报告 2.1.3 / 4.5.2） | 有效 |
| D-9 | 测试实际结果/状态/截图一律不预填，实施后据实回填 | 课程学术诚信要求"不虚构测试结果"（设计报告文档状态说明 / 第 6 章） | 有效 |
| D-10 | 本项目一切工作遵守 project-governance 治理纪律（git 分步提交 + PLAN/STATE 唯一权威） | 用户 2026-09-11 要求：随时可回滚、严格分步、持久记忆、杜绝上下文矛盾 | 有效 |
| D-11 | 开发起点：先环境后代码，顺序 S19（环境准备）→ S20（六组件）→ S21（最小链路），再扩展业务；文档步骤 S10/S11 冻结至实现完成后 | 探测确认 Docker/WSL2 未装、JDK 为 1.8——环境是硬阻塞；且课程阶段划分与设计报告 4.6.4 均为"自下而上逐层验证"；用户 2026-09-11 确认 | 有效 |
| D-13 | JDK 口径由 17 调整为 21（细化 D-2 的 JDK 版本部分）：使用本机 DevEco Studio 自带 JBR 21.0.6（完整 JDK，含 javac），复制至 `tools/jdk-21` 使用；构建时以命令内联 `JAVA_HOME` 覆盖全局（全局 JAVA_HOME 仍指向 JDK 8，不改系统环境变量） | 沙箱网络 TLS 凭据不可用（SEC_E_NO_CREDENTIALS），Adoptium 与镜像均无法下载；本机已具备完整 JDK 21，且 Spring Boot 3.2 支持 17~21，21 满足要求；设计报告 5.1.2/4.3.1/技术栈基线已同步更新（v0.2） | 有效 |
| D-14 | WSL2 内存分配 8 GB、swap 8 GB（通过 `.wslconfig` 设置） | 本机总内存实测 15.7 GB，按约 50% 分配留足 Windows 开销；取代此前"10~12 GB"的口头建议 | 有效 |
| D-15 | S20 开工前置条件：C 盘空闲 ≥ 30 GB（当前 15.8 GB，须先清理）；期间不启动 Kibana | 六组件镜像约 7~8 GB + vhdx 增长，15.8 GB 必然爆盘（风险 #10） | 有效 |
| D-16 | docker.io 镜像经国内镜像源拉取并重标记为官方名（`docker/init/pull-images.ps1`），镜像源顺序：docker.1ms.run → docker.xuanyuan.me → docker.m.daocloud.io → hub.rat.dev；ES/Kibana 走官方源 docker.elastic.co 直连；compose 文件保持官方镜像名不变 | 本机 DNS 污染致 Docker Hub 直连失败（风险 #12），四个镜像源实测可用；重标记方案不改 compose、无需改引擎配置，换干净网络后可无缝回直连 | 有效（经 D-17 补充：有代理时直连优先，镜像源降级为兜底） |
| D-17 | Docker 镜像拉取通道升级：用户提供 Clash Verge 代理（mihomo 混合端口 `127.0.0.1:7897`，实测开放）；Docker Desktop 配置手动代理（HTTP/HTTPS 均指向该地址）后，`pull-images.ps1` 改为**官方源直连优先、镜像源兜底** | 代理通道速度快且可直连官方源，镜像源仅作降级；Clash Verge 需开"允许局域网"使 WSL 引擎可达 | 有效 |
| D-18 | Kafka 镜像由 `bitnami/kafka:3.6` 更换为官方镜像 **`apache/kafka:4.0.0`**（取代 D-2 的 Kafka 版本/镜像部分）：Bitnami 已下架全部公开 Kafka 镜像（Apache 邮件列表 [DEPRECATION] 通告，Docker Hub 返回 404），官方镜像持续维护且 4.x 为纯 KRaft 架构；compose 环境变量由 `KAFKA_CFG_*` 改为官方 `KAFKA_*` 命名，数据目录 `/var/lib/kafka/data`，脚本路径 `/opt/kafka/bin` | 2026-09-11 实测：bitnami/kafka:3.6 与 4.1 均 "not found"；apache/kafka:4.0.0 经代理直连拉取成功；官方镜像与设计 KRaft 单节点双监听器方案完全兼容 | 有效 |

## 5. 术语与口径注册表（全项目唯一权威口径，改口径必须先改本表）

| 类别 | 口径 |
|---|---|
| 组件版本 | Hadoop HDFS 3.3.6；MongoDB 6.0（副本集 rs0）；Kafka **4.0.0（官方镜像 apache/kafka，纯 KRaft，ZooKeeper 已移除）**；Elasticsearch/Kibana 8.13.0；Nginx 1.25-alpine；Spring Boot 3.2 + JDK 21（本机 `tools/jdk-21`，源自 DevEco Studio JBR 21.0.6，含 javac；17+ 均可）；Vue 3.4 |
| 端口 | 唯一入口 Nginx **8080**；后端实例 8081/8082；NameNode 9870(WebUI)/8020(RPC)；DataNode 9864；MongoDB 27017；Kafka 内 9092 / 控制器 9093 / 外 9094；ES 9200；Kibana 5601 |
| Kafka 主题 | `uav.telemetry`(3 分区)、`robot.telemetry`(2)、`device.heartbeat`(3)、`inspection.alarm`(3)、`inspection.image.meta`(2)、`task.command`(2，下行)、`task.log`(2)、`inspection.dlq` |
| 消费组 | `biz-storage-consumer`、`biz-alarm-consumer`、`biz-stats-consumer`、`sim-uav`、`sim-robot` |
| MongoDB 集合 | `device`、`device_status`、`task`、`alarm`、`image_meta`、`task_log`；TTL：device_status 30 天、task_log 90 天 |
| ES 索引 | `inspection_alarm_v1`；`dynamic: strict`；`location` 为 geo_point（**顺序 [经度,纬度]**）；中文分词 `ik_smart`（降级 `standard`） |
| HDFS 路径 | `/inspection/{imageType}/{yyyy}/{MM}/{dd}/{deviceId}/{uuid}.{ext}`；imageType ∈ {uav_patrol, dog_infrared, alarm_snapshot} |
| 编号段 | 图：3-x/4-x/5-x（图 3-1~图 5-16）；表：4-x/5-x/6-x；用例 TC001~TC034、IT001~IT015、PT001~PT007；缺陷 BUG-xxx；决策 D-n；计划步骤 Sxx |
| 节奏指标 | 心跳 5 s；遥测 2 s；离线阈值 15 s（3 个心跳周期）；遥测端到端 P95 < 1.5 s；检索 P95 < 500 ms；并发 ≥ 50 msg/s；网关 ≥ 200 QPS |
| 文档文件 | `课程设计报告_第2-6章_园区安防巡检平台.md`（主交付文档，v0.1，基线提交 3056752）；头部含修订记录表 |
| 修订记录约定 | 每次改正式文档须在头部修订记录表加一行：日期/版本/摘要/提交号 |
| 网络通道 | 代理：Clash Verge（mihomo）混合端口 `127.0.0.1:7897`，Docker Desktop 手动代理配置用；镜像拉取：官方源直连优先、镜像源兜底（D-16/D-17）；镜像源可用清单见风险 #12 |

## 6. 已知问题与风险

1. 【待回填】设计报告 5.4 节 16 张截图、6.6 节全部测试结果与结论、6.7 节优化前后对比——须实际运行后据实填写（D-9）。
2. 排错预案 P-1~P-15（设计报告 5.3 节）：Kafka 地址回传、NameNode 重复格式化、WebHDFS 重定向、ES yellow、refresh 语义、geo 经纬序、时区等。
3. 宿主机内存建议 ≥ 12 GB；实测本机总内存 **15.7 GB**（勉强达标）→ WSL2 内存分配定为 **8 GB**（D-14），资源不足时先停 Kibana、减少仿真实例（设计报告 4.6.3(5)）。
4. Word 打开期间会在工作区生成 `~$*.docx` 锁文件——已被 .gitignore 排除；注意 Word 占用时不要删除/覆盖对应 docx。
5. 版本库目前仅存本地，无远端；异地备份为候选步骤 S60。
6. 测试用例 TC/IT/PT 仅完成设计（第 6 章），尚未执行——执行是候选步骤 S50。
7. 全局 `JAVA_HOME` 仍指向 JDK 8（1.8.0_151）：所有 Java 构建命令必须内联覆盖 `JAVA_HOME` 指向 `tools\jdk-21`（见 D-13）；用户可自行修改系统环境变量（可选）。
8. 沙箱网络限制：AI 执行环境的 curl/Invoke-WebRequest 因 TLS 凭据不可用（SEC_E_NO_CREDENTIALS）无法下载外部文件；需要联网下载（Docker Desktop 安装包等）时由用户在系统终端执行。
9. 用户机器访问 GitHub 失败（`wsl --install` 拉取发行版列表时 raw.githubusercontent.com DNS 解析失败，WININET_E_NAME_NOT_RESOLVED）。当前状态：WSL 2.7.13 与内核 6.18.33 已装好（AI 侧实测），仅缺 Ubuntu 发行版——**项目不需要 Ubuntu**（Docker Desktop 自带 docker-desktop 发行版）。引申风险：S20 从 Docker Hub 拉镜像可能同样受阻；预案：Docker Desktop 配置国内 registry mirror（中科大/网易/阿里加速器），S20 第一步先 `docker pull hello-world` 验证。
10. **磁盘空间不达标（S19 验收项④未过）**：C 盘空闲仅 **15.8 GB**（设计要求 ≥40 GB），且本机仅 C 一个盘符。缓解预案：① `powercfg /h off` 关闭休眠回收约 6 GB；② 磁盘清理（cleanmgr/系统临时文件/回收站）；③ `.wslconfig` 设 `memory=8GB swap=8GB`；④ 开发期不启动 Kibana；⑤ Docker Desktop 磁盘镜像上限设合理值。**已解决（2026-09-11）**：用户清理后 C 盘空闲 50 GB，满足 D-15 开工条件；④ 相应解除（Kibana 正常启动）。
11. AI 沙箱无法通过 docker CLI 确认引擎状态（named pipe `dockerDesktopLinuxEngine` 被沙箱拒绝：permission denied）；docker 引擎运行状态须由用户目视鲸鱼图标（绿色=Engine running）或用户自行在系统终端执行 `docker info` 确认。**已确认（2026-09-11）**：用户目视鲸鱼变绿，引擎运行中；S20 起 AI 执行 docker 命令需经沙箱升级授权（danger-full-access，用户已批准该模式）。
12. **Docker Hub DNS 污染（S20 运行验收受阻）**：本机默认 DNS 将 registry-1.docker.io 解析为 127.0.0.1/::1（阿里 DNS 223.5.5.5 可正确解析），docker.io 直连不可用；docker.elastic.co 未受污染可直连。**应对（2026-09-11 实测）**：镜像源通道可用——docker.1ms.run / docker.xuanyuan.me / docker.m.daocloud.io / hub.rat.dev 均通（dockerproxy.net TLS 超时不可用）。**升级（D-17）**：用户 Clash Verge 代理（7897）就绪，Docker Desktop 配置手动代理后官方源直连可用，镜像源降级为兜底。

## 7. 下一步计划

- **S19 进行中**：JDK 21 已就绪（`tools/jdk-21`，验证通过）；待用户完成 Docker Desktop + WSL2 安装（需管理员权限、可能重启、BIOS 虚拟化核对）→ 双方验证验收标准后 S19 置 done。
- 之后依次：S20（六组件环境）→ S21（最小业务链路）→ S30/S31（业务与仿真全量）→ S40（前端）→ S50（测试）→ 解冻 S10/S11（文档收尾）。
- 任何新工作先在此与 PLAN.md 登记，再执行。
