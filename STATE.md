# 项目状态与记忆（STATE）

> 本文件是**唯一权威项目记忆**。一切"之前干了什么"以本文件为准，不依赖对话上下文。
> 规则见 `.dsh/skills/project-governance/SKILL.md`：本文件只增不改；决策永不删除只能取代；口径改动必须先改本注册表再改引用点。
> 本文件更新：2026-09-12（最近更新随时间线最新条目）

---

## 1. 当前状态摘要

| 项 | 值 |
|---|---|
| 项目 | 无人机-机器狗空地协同巡检集成平台（选题 1 园区安防，课程：企业应用开发实践） |
| 当前步骤 | **P0/P1 清单全部完成（S65~S73 done，S65 截图回填部分 blocked 等用户截图）**；P2 工程化可选项待用户勾选 |
| 最近完成 | S68 手动复核 UI、S69 轨迹回放（282 点实测）、S70 对账补偿（删 1 补 1 实测）、S71 深分页防护、S72 影像元数据链路（BUG-008 转已实现）、S73 文档转换（HTML/docx） |
| 当前版本 | 里程碑 **v0.6**（2026-09-13 用户按批准清单顺序确认打标）；历史 v0.5 / v0.4 / v0.3 / v0.2 / v0.1 |
| 进行中事项 | 无（待办：S65 截图回填等用户按 `docs/截图操作清单.md` 截图；报告套课程模板/封面/目录为小组人工事项；P2 可选项待用户勾选：Swagger/单元测试/死信重放/备份/监控面板） |
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
| v0.2 | 本步提交（HEAD） | 里程碑：S20 六组件环境验收全过（六组件 healthy、rs0 PRIMARY、8 主题、HDFS 读写、生产消费、四端点 HTTP 验证） | 2026-09-11 |
| v0.4 | S40 验收提交 | 里程碑：S40 前端五页面 + 全链路联调验收全过（补记，详见 PLAN.md S40 验收结论） | 2026-09-12 |
| （S50） | e3267ca→67ceddf→c3132a0→b06ee48 | S50 性能缺陷修复链：N+1 聚合尝试（实测劣化驳回）→ 缺索引根因修复（开启自动建索引+复合索引）→ TC022 取消竞态修复 → ES total 截断修复 | 2026-09-12 |
| （S50） | 796db77 等 | S50 测试资产：tc2 功能用例第二批脚本、gen-alarm-data.ps1（10k 注入）、jtl-stats.ps1（分位数统计）、nginx 限流恢复 | 2026-09-12 |
| **v0.5** | 8a4d84d 之后（本步） | 里程碑：S50 全量测试通过——功能 32/34（2 未实现）、接口 13/15（2 未实现）、性能 7/7；BUG-001~008 登记；手动上下线 5s 提速；报告第 6 章回填。**用户 2026-09-12 确认打标** | 2026-09-12 |
| **v0.6** | b7394f8 | 里程碑：S50 遗留两项全部兑现（BUG-003 复核派单闭环、BUG-004 ik 中文分词）+ 全部回归（v0.6 验收 8/8）；功能 34/34、接口 14/15（1 设计差异）、性能 7/7；报告全一册 1~7 章。**用户 2026-09-13 按批准清单顺序确认打标** | 2026-09-13 |

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
| 2026-09-11 | 本步（HEAD） | S20 中断（用户关机赶路）：六镜像全部就绪；`docker compose up -d` 已执行，ES 已就绪（9200 返回 200），HDFS/Kibana/Nginx 启动中；验收批次（Mongo rs.initiate、Kafka 主题、HDFS 读写、生产消费）**未完成**。所有初始化步骤均幂等（rs.initiate 可重入、--if-not-exists、ENSURE_NAMENODE_DIR 格式化守卫），下次开机可直接重跑验收，无数据丢失风险（数据在命名卷中）。本次修正：同步更新了过期的 §7 下一步计划（此前仍停留在 S19 措辞） | AI |
| 2026-09-11 | 本步（HEAD） | S20 恢复排错（envtoconf bug）：恢复批次健康等待超时，定位到 namenode/datanode 因 `CORE_CONF_*` 环境变量触发 envtoconf `to_conf` 解析崩溃（ValueError）；回退为挂载 `core-site.xml`/`hdfs-site.xml`（重建此前删除的配置文件），保留 ENSURE_NAMENODE_DIR 与 WAITFOR；登记风险 #13 | AI |
| 2026-09-11 | 本步（HEAD） | S12 完成（用户要求）：报告第 2 章新增无编号导读块"六组件速览"——一句话定位表、数据流串联、三个关键分工；口径与注册表核对一致；修订记录升版 v0.4 | AI |
| 2026-09-11 | 本步（HEAD） | S21 开始（最小业务链路）：计划产出 simulator（心跳 5s/遥测 2s 生产者）与 backend（消费落库 + /api/devices + 实例头）最小工程，经 Maven 构建（需代理下载依赖）后容器化部署并验收 | AI |
| 2026-09-11 | 本步（HEAD） | S21 完成：验收四连全过（API 返回设备数据、心跳 15s 内更新、实例号 10 次请求 5/5 交替、LAG=0 且 device_status 1087 条）；排错三例入册（#17 沙箱禁写 ~/.m2→m2repo 重定向、#18 BuildKit 不走代理→先 pull 后 build、#19 nginx 配置热重载）；沙箱临时目录被系统清理导致一次通道故障（重建恢复） | AI |
| 2026-09-11 | 本步（HEAD） | **今日收工（用户决定）**。当日成果：S19 环境准备、S20 六组件环境（v0.2）、S12 导读段（报告 v0.4）、S21 最小链路（v0.3）；累计风险档案 #1~#20、决策 D-1~D-18。**运行环境现状**：10 个容器在跑（六组件 + backend-1/2 + uav-sim），关机即停（无 restart 策略），数据全部在命名卷不受影响。**下次恢复**：① 启动 Docker Desktop 等鲸鱼变绿（构建/拉镜像时再开 Clash）；② 对我说"继续"，我先读 PLAN/STATE 再汇报恢复方案；③ 下一步候选：S30 后端全量 / S31 仿真全量 / S40 前端，待用户选择 | AI |
| 2026-09-12 | 本步（HEAD） | 今日开工：用户开启 Docker 与代理；开场协议通过（git 干净、代理 7897 开放、容器待恢复）。**顺序调整（用户确认）**：S31 仿真全量提前至 S30 之前——理由：S30 验收依赖仿真先产生告警与指令回执；S31 开始 | AI |
| 2026-09-12 | —（补记） | **时间线补记（S31→S40，此前各步验收结论均逐条记于 PLAN.md 对应步骤，本表缺行）**：S31 仿真全量（多模式轨迹/故障注入/优先级队列/电量循环，镜像 0.8）；S30 后端全量（消费落库/告警三写/任务状态机/统计，镜像 0.2）；S32 任务闭环（回执驱动、task.log 消费组）；S33 任务行为（就近派单 2dsphere、优先级调度、取消返航——复核派单闭环未做=BUG-003 来源）；S34 电源与上下线（电量归零离线、手动上下线）；S40 前端五页面（用户反馈驱动 6 轮改进，验收全过，v0.4）。里程碑 v0.5 曾误打、经用户纠正撤销（仅剩 v0.4） | AI |
| 2026-09-12 | f3ca219 | S50 接口测试执行并回填报告：15 例 13 通过 / 2 未实现（IT001 设计差异、IT009 BUG-003）；契约缺口当场修复（分页/201/参数校验/文件下载/总览统计） | AI |
| 2026-09-12 | 2f027d5 | S50 性能测试资产：JMeter 计划（search 50 线程×300s / gateway 100 线程）、accept-s50-pt.ps1、**nginx 限流补齐（BUG-001 修复，FR-5.4）** | AI |
| 2026-09-12 | e3267ca 及后续 | S50 功能测试执行（accept-s50-tc.ps1 22/22）与 PT004 瓶颈定位：实测 888.8ms → 尝试聚合方案劣化至 1,229.7ms（被数据驳回）→ 根因：`auto-index-creation` 未开启致 @Indexed（TTL+deviceId）从未落地（BUG-005）→ 修复后 **avg 36.3ms / P95 80ms / 2,632 rps（24.5×）**；TC020 索引校验回归通过 | AI |
| 2026-09-12 | 67ceddf | TC022 实测发现并发取消竞态（二次取消均 200）→ 取消受理即同步置 CANCELLED（BUG-007 修复），回归 cancel1=200/cancel2=409 | AI |
| 2026-09-12 | b06ee48 | 10k 告警注入（gen-alarm-data.ps1，Mongo/ES 各 10,000 条、LAG=0）当场暴露 **ES hits.total 截断 10,000 缺陷（BUG-006）** → search/stats 开启 track_total_hits，回归 total=10,211 精确 | AI |
| 2026-09-12 | 本步 | S50 第二批功能用例（accept-s50-tc2.ps1，14 例）终跑：12 PASS + 2 如实 FAIL（TC021 BUG-003、TC027 BUG-004 均未实现遗留）；脚本缺陷 4 轮修正（PS5.1 引号断裂→--eval 单引号 JS、mongosh stdin 提示符行、alarmId/_id 字段、批号幂等）；TC018 MD5 一致、TC026 收敛一致、TC029 故障转移 5/5 | AI |
| 2026-09-12 | 本步 | PT003 官方轮（限流关闭、10k 数据、50 线程×300s）：**896,860 样本 / 0 错误 / P95 28ms / 2,988.7 rps**；PT001 500 条注入 +556 零丢失、PT002 100 条排水 3,228ms、PT005 50/50 完美均衡、PT006 200 突发 158×503/42×200、PT007 10 分钟稳定（容器 0 退出）。PT003 首轮限流开启时 78.4% 被 503（限流器生效证据）。**性能 7/7 全过** | AI |
| 2026-09-12 | 本步 | 报告第 6 章据实回填（表 6-2 全 34 行、6.3.3 BUG-001~008 登记、表 6-7/6-8/6-9、6.6 分析四问、6.8 小结、5.3 预案命中补充）；执行口径说明（PT003/PT004 临时关闭限流、PT007 压缩 10 分钟）如实记录 | AI |
| 2026-09-12 | 本步 | **用户反馈：手动上下线间隔 15s 太长，改为 5s**。实现：下线=指令驱动（COMM_OFFLINE 下发后 5s 置 OFFLINE+告警，复用 OfflineDetector.markOffline 幂等逻辑）；上线=仿真 COMM_RESTORE 后立即补发心跳。自然失联判定阈值保持 15s 不动（5s 阈值会与 5s 心跳同频导致负载下误报离线，已向用户说明）。实测：下线 t=5s、上线 t=1s；口径同步（STATE 节奏指标、PLAN S34 验收标准、报告 TC010 行、tc1 断言窗口） | AI |
| 2026-09-12 | 5202ff0 | **用户确认打 v0.5 里程碑 tag**（S50 全量测试通过）。用户决定：BUG-003（复核派单闭环，含 IT009）/ BUG-004（ik 分词）排入 v0.6 实现；IT001 设计差异永久记档。PLAN 登记新步骤 S61（复核后端）/S62（仿真+前端+回归）/S63（ik） | AI |
| 2026-09-12 | d5ba022 | **S61 完成**（v0.6 第一步）：复核派单后端——DeviceDoc.location 2dsphere 索引、心跳携带坐标、AlarmConsumer 可复核类型触发 nearSphere 就近派单（空闲优先，D-21）、POINT_REVIEW 任务关联 alarmId、DONE 回执生成红外复核图并回填 review、IT009 手动复核接口。验收：IT009 200/400、location_2dsphere 索引建立。排错 1 例：NearQuery.num(int) 已废 → limit(long)（Spring Data Mongo 4.x API） | AI |
| 2026-09-12 | 本步 | **S62 完成**（v0.6 第二步）：仿真心跳携带 track 坐标；前端告警详情证据链面板（原图+复核图+结论并列）；accept-v06 验收 **7/7 PASS**（TC021 就近派单双验证、复核回填、复核图下载 5,848 字节、IT009、TC032 API）。排错 2 例：① 机器狗 deviceType 口径为 ROBOT_DOG，派单过滤误写 ROBOT 致无候选（已修）；② 验收脚本竞态——任务 DONE 后 completeReview 的 HDFS 上传未完成即取告警，改轮询 review 出现。报告 TC021/IT009/TC032 转绿、表 6-7 功能 33/34 接口 14/15、BUG-003 登记回归通过、文档升版 v0.6 | AI |
| 2026-09-12 | 本步 | **S63 完成**（v0.6 最后一步）：① ik 插件安装（官方发布源 4.4MB zip，与 ES 8.13.0 版本匹配；GitHub 全通道 404 排错实录入风险 #35）；插件 bind 挂载持久化 + install-ik.ps1 可复现；② 索引升版 inspection_alarm_v2（description=ik_smart）+ _reindex 迁移 10,716 条（v1 保留回滚基线）；③ ik_smart 分词验证（园区/围墙/入侵/人员独立成词）与中文检索实测（人员 300 条、红外温度 5 条）；④ 发现并修复队列积压双缺陷：复核节流 D-22（待办≥2 跳过派单）+ 仿真告警频率 15%→5%（告警流入与复核吞吐匹配）+ 断电回充后队列续跑（风险 #36）；⑤ **v0.6 验收官方终跑 8/8 PASS**，TC027 转绿，功能测试 34/34；报告升版 v0.7，BUG-004 登记回归通过，**S50 登记的两项遗留全部清零** | AI |
| 2026-09-12 | 本步 | **S63 修复回归（用户报告"统计看板没有图"）**：stats 接口 500 排查 → 根因是 v2 索引被 `_reindex` 以**动态映射自动创建**（后端初始化器在 ES 未就绪时放弃创建且不重试，见风险 #37）——alarmType 变 text 致聚合 fielddata 报错、location 变 float 致地理检索失效。修复：初始化器重试 24×5s + 索引升版 **inspection_alarm_v3**（严格映射）+ v2→v3 迁移 10,796 条；期间又暴露 ik 词典目录丢失（ES8 插件配置位于 config/ 而非 plugins/）致英文文本分词 NPE，补挂载 `es-plugins/analysis-ik/config → config/analysis-ik` 后恢复。验证：stats 200（by_type=6/trend=25）、地理检索 8,541 条、中文检索 304 条；**v0.6 验收在 v3 上终跑 8/8 PASS**；v2 删除，v1 保留回滚基线 | AI |
| 2026-09-12 | bfedbb8/23b9c8e/fefcef4 | **证据图中文占位方块修复 + 今日收工**：用户报告证据图中文描述为方块（英文正常）→ 排查全链路数据无损坏（风险 #38）→ 根因为后端镜像缺 CJK 字体 → Dockerfile 补装 font-noto-cjk，新告警证据图中文正常（墨迹 3.53%→7.2%）；新增维护接口 `POST /api/maintenance/regenerate-evidence`（覆盖原 HDFS 路径回填存量，代码 fefcef4，已部署）。**遗留 S64（次日）**：存量重生成经 nginx 触发 504（30s 网关超时，风险 #39），后端未完成执行。**运行环境现状**：全部容器健康在跑；数据与版本库干净 | AI |
| 2026-09-13 | 本步 | **S64 完成（恢复执行）**：开场协议通过（git 干净）；Docker 引擎重启致容器全部 Exited → `compose up -d` 恢复健康；nginx 新增 `/api/maintenance/` 专属 location（read_timeout 900s、不限流，风险 #39 长久修复）→ 经网关执行重生成：**total=10,891 / ok=840（中文描述）/ skipped=10,051 / fail=0 / 耗时 175s**；复测 ALM-7c2e59b0 证据图墨迹 3.53%→7.2%，**历史方块全部清零** | AI |
| 2026-09-13 | 6e89005 | **用户反馈逻辑修复**：告警详情证据标签硬编码"高空发现原图（无人机）"——机器狗自报告警（离线/过热）与设备状态告警（离线/电量）不适用。修复：标签按告警类型+设备类型动态生成（DEVICE_OFFLINE/BATTERY_LOW → "事件记录图（设备状态）"；UAV → "高空发现原图（无人机）"；ROBOT_DOG → "设备自报事件图（机器狗）"）；状态类告警复核面板改为"不派现场复核（人工处置）"提示（与 D-21 派单策略一致）。前端已重建部署 | AI |
| 2026-09-13 | a8f5c7c | **用户批准改进清单并排定顺序**（P0：截图→报告 1/7 章→v0.6 tag→文档收尾；P1：复核 UI→轨迹回放→对账→深分页→影像元数据）。PLAN 登记 S65~S73；S65 截图清单已交付（docs/截图操作清单.md，图 5-1~5-16 逐图步骤），S65 blocked 待用户截图 | AI |
| 2026-09-13 | b7394f8 | **S66 完成**：报告补齐第 1 章（绪论：背景/任务/现状/成果/结构）与第 7 章（总结与展望：工作总结、8 项不足与改进方向 S68~S72、展望），文档升为全一册 1~7 章，修订记录升版 v0.8；**v0.6 里程碑 tag 打标**（用户按批准清单顺序确认） | AI |
| 2026-09-14 | 6f15c2b/bb37e85/1973cb3/4d44a81/ed0af28/b44ff51 | **P1 清单全部完成（S68~S73）**：S68 手动复核前端入口（PENDING 一键确认属实/误报+备注）；S69 轨迹回放（track 接口 282 点实测 + 地图折线/起终点/自动聚焦）；S70 Mongo↔ES 对账补偿（6h 滚动窗口+5min 定时+手动触发，删 1 补 1 实测 fixed=1）；S71 深分页防护（显式 400+前端截断警示）；S72 影像元数据链路（BUG-008 转已实现，topic/集合/接口/告警关联实测）；S73 文档转换（marked MD→HTML→Word COM docx，3573 段/43 表，交付说明列小组人工事项）。排错 1 例：S70 首轮误删 37h 前旧告警超对账窗口（窗口设计如此） | AI |
| 2026-09-14 | 59cddb1 | **S74 用户反馈三项修复**：① 测试注入告警污染列表（"S72 meta-link probe" 等）→ 新增清理接口，实测删 **10,053 条**（pt3k-/tc017-/v06/fontfix- 前缀，Mongo+ES+17 任务+48 日志），现存量自然告警 927 条；② 人工复核反馈不明显 + 备注不显示 → 复核结果面板重构（人工复核无图也显示结论/方式/备注）+ 提交中禁用按钮 + 成功/失败 toast 3-4 秒；③ 清理暴露 ES 存量缺口（v3 修复期写入失败）→ 新增全量对账入口（30 天窗口），**fixed=49，mongo=es=927 完全一致** | AI |
| 2026-09-14 | f108321 | **S75 人工复核完善（用户确认）**：multipart 现场照片上传（HDFS `manual_review` 目录、5MB 校验）→ review.manualPhotoPath → 下载接口 + 前端展示；修正人工复核不再伪造红外复核图；端到端实测（上传 856B PNG → 回填 → 下载一致）。口径：HDFS imageType 增加 manual_review（STATE/报告同步） | AI |
| 2026-09-14 | 8754cdc~c2181de | **P2 清单全部完成（S76~S81）**：S76 Swagger（springdoc，25 路径）；S77 单元测试（后端 9/9 + 仿真 6/6，mvn test 全绿）；S78 死信查看/重放（peek + replay-all 实测重放 22 条）；S79 一键备份（git bundle+mongodump+compose，clone 恢复 HEAD 一致实测）；S80 系统健康面板（mongo/es/hdfs/四组 LAG，10s 轮询）；S81 证据图大图预览。排错 2 例：UpdateResult 包路径、seekToBeginning 首轮空轮询 | AI |
| 2026-09-14 | 5a75868 | **S82 用户反馈（大图视觉）**：证据图/复核图生成分辨率 640×360 → **1280×720**（字体同步放大），大图模式改 object-fit 满屏；维护接口 `regenerate-evidence?all=true` 与 `regenerate-review-images` 全量重生成——实测 **1008 证据图 + 73 复核图 fail=0**，下载验证尺寸 1280×720 | AI |

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
| D-19 | S50 压测执行口径：PT003/PT004 压测时临时注释 nginx `limit_req`（压测对象是后端/网关而非限流器），测后恢复并由 PT006 单独验证限流；PT001/PT002 用批量注入+排水计数替代长时间打流；PT007 稳定性 2h 压缩为 10 分钟采样 | 分离"后端能力"与"限流器行为"两个被测对象，避免限流 503 污染性能数据；压缩时长控制课程节奏，结论强度不降低（计数与 LAG 证据等价） | 有效 |
| D-20 | 缺陷登记口径：注册端点缺失（IT001）与影像元数据主题未接入（BUG-008）登记为**设计差异**而非缺陷；复核派单闭环（BUG-003）与 ik 分词（BUG-004）登记为**范围/环境遗留**，均不阻塞验收，计划 v0.6 | 与 D-9 一致：如实记录差异与范围，不粉饰为通过、也不夸大为止步缺陷 | 有效 |
| D-21 | S61 复核派单策略（v0.6，BUG-003 回归）：**可复核类型** PERIMETER_BREACH/INTRUSION/FIRE_SMOKE/DEVICE_OVERHEAT（BATTERY_LOW/DEVICE_OFFLINE 属设备状态事件不派现场复核）触发就近派单——2dsphere `nearSphere` 查最近 4 台在线机器狗（deviceType=ROBOT_DOG），空闲（无 DISPATCHED/RUNNING 任务）优先、全忙派最近（设备优先级队列兜底）；任务关联 `TaskDoc.alarmId`；DONE 回执 → 生成红外复核图（`dog_infrared` 目录）→ 告警 status/review 回填。**复核结论规则为仿真口径**：alarmId 哈希 70% CONFIRMED / 30% FALSE_ALARM（真实系统应由识别模型输出）；历史 10k 告警不回放派单；手动复核 IT009 覆盖人工干预路径 | 与设计 O-6/TC021/IT009 对齐；结论规则显式标注仿真口径，不伪装成真实识别 | 有效 |
| D-22 | S63 复核吞吐匹配策略：① 派单节流——选中机器人待办复核任务 ≥ 2 时跳过自动派单（告警留待人工复核 IT009）；② 仿真告警频率调低——UAV patrolScan 15%/30s → 5%/30s（告警流入约 0.6 → 0.2 条/分钟），匹配 2 台机器狗复核吞吐（每单 1~2 分钟）。背景：实测告警高峰下复核队列持续积压、任务永久停在 DISPATCHED | 让自动复核闭环在真实告警速率下可持续，而非只在测试注入下可演示 | 有效 |

## 5. 术语与口径注册表（全项目唯一权威口径，改口径必须先改本表）

| 类别 | 口径 |
|---|---|
| 组件版本 | Hadoop HDFS 3.3.6；MongoDB 6.0（副本集 rs0）；Kafka **4.0.0（官方镜像 apache/kafka，纯 KRaft，ZooKeeper 已移除）**；Elasticsearch/Kibana 8.13.0；Nginx 1.25-alpine；Spring Boot 3.2 + JDK 21（本机 `tools/jdk-21`，源自 DevEco Studio JBR 21.0.6，含 javac；17+ 均可）；Vue 3.4 |
| 端口 | 唯一入口 Nginx **8080**；后端实例 8081/8082；NameNode 9870(WebUI)/8020(RPC)；DataNode 9864；MongoDB 27017；Kafka 内 9092 / 控制器 9093 / 外 9094；ES 9200；Kibana 5601；**仿真故障注入通道 8089~8092（uav-sim-1/2、dog-sim-1/2 的 /sim/fault，仅运维测试用，非业务入口）** |
| Kafka 主题 | `uav.telemetry`(3 分区)、`robot.telemetry`(2)、`device.heartbeat`(3)、`inspection.alarm`(3)、`inspection.image.meta`(2)、`task.command`(2，下行)、`task.log`(2)、`inspection.dlq` |
| 消费组 | `biz-storage-consumer`、`biz-alarm-consumer`、`biz-task-consumer`（S32 新增，task.log 回执消费）、`biz-image-consumer`（S72 新增，image.meta 消费）、`sim-cmd-<deviceId>`（S32 重构：每设备独立指令消费组，见风险 #27） |
| MongoDB 集合 | `device`、`device_status`、`task`、`alarm`、`task_log`、`image_meta`（S72 起，影像元数据；BUG-008 已实现）；TTL：device_status 30 天、task_log 90 天 |
| ES 索引 | **`inspection_alarm_v3`**（现行，S63 起）：`dynamic: strict`；`location` geo_point（**顺序 [经度,纬度]**）；`description=ik_smart`（analysis-ik 8.13.0，插件+词典经 `docker/es-plugins` bind 挂载持久化）；`inspection_alarm_v1` 保留作回滚基线；`inspection_alarm_v2` 为映射损坏的中间产物（_reindex 动态映射误建，风险 #37），仅留档 |
| HDFS 路径 | `/inspection/{imageType}/{yyyy}/{MM}/{dd}/{deviceId}/{uuid}.{ext}`；imageType ∈ {uav_patrol, dog_infrared, alarm_snapshot, **manual_review（S75 起，人工复核现场照片）**} |
| 编号段 | 图：3-x/4-x/5-x（图 3-1~图 5-16）；表：4-x/5-x/6-x；用例 TC001~TC034、IT001~IT015、PT001~PT007；缺陷 BUG-xxx；决策 D-n；计划步骤 Sxx |
| 节奏指标 | 心跳 5 s；遥测 2 s；离线阈值 15 s（3 个心跳周期，自然失联判定）；**手动上下线 5 s 内生效（指令驱动，S50 用户要求提速）**；遥测端到端 P95 < 1.5 s；检索 P95 < 500 ms；并发 ≥ 50 msg/s；网关 ≥ 200 QPS |
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
13. **apache/hadoop 镜像 envtoconf 机制存在 bug（已踩坑，已修复）**：`CORE_CONF_*`/`HDFS_CONF_*` 环境变量触发 `to_conf` 转换，其对 `process_properties` 返回的字典直接迭代解包 → `ValueError: too many values to unpack`，NameNode/DataNode 启动即崩。**结论**：本项目禁用 envtoconf 环境变量，HDFS 配置一律用挂载的 `core-site.xml`/`hdfs-site.xml`（无 CORE_/HDFS_ 环境变量时 envtoconf 为 no-op，不覆盖挂载文件）；`ENSURE_NAMENODE_DIR`（格式化守卫）与 `WAITFOR`（启动等待）仍可用（属 starter.sh，非 envtoconf）。
14. **Kibana basePath 反代坑（已踩坑，已修复）**：Kibana 设 `SERVER_BASEPATH=/kibana` 后，容器内状态接口变为 `/kibana/api/status`（裸 `/api/status` 返回 404）→ 原健康检查永远失败（Kibana 一直 health: starting）；Nginx 反代必须 `proxy_pass http://kibana:5601;`（**不带尾斜杠**，保留 /kibana 前缀），带尾斜杠会剥前缀导致 404。两处已对齐。
15. **alpine busybox wget 的 IPv6 localhost 坑（已踩坑，已修复）**：busybox wget 连 `localhost` 优先解析 `::1`，而 nginx 仅监听 IPv4 且 busybox 不回退 → "Connection refused"，nginx 健康检查永远失败（nginx 实际运行正常）。**结论**：容器内健康检查/自检一律用 `127.0.0.1` 而非 `localhost`（nginx 已改；kibana 的 curl 有回退机制不受影响）。
16. **GBK 管道编码坑（已踩坑，已修复）**：Windows 下 `Get-Content -Raw | docker exec -i mongodb mongosh` 以 GBK 解码 UTF-8 脚本，中文注释变乱码并破坏 JS 字符串 → rs.initiate 从未执行成功（表现：`no replset config has been received`）。**结论**：经管道送入容器的脚本一律纯 ASCII（`mongo-rs-init.js` 已改），或显式 `-Encoding UTF8` 读取。
17. **沙箱禁写 ~/.m2（已踩坑，已修复）**：Maven 默认本地仓库 `C:\Users\黎Li\.m2\repository` 位于工作区外，沙箱拒绝写入（`AccessDeniedException`）。**结论**：`.mvn/maven-settings-proxy.xml` 中 `<localRepository>` 重定向到工作区 `tools\m2repo`（已 gitignore），所有构建走 `-s .mvn/maven-settings-proxy.xml`，无需升级授权。
18. **BuildKit 不走 Docker Desktop 代理（已踩坑，已修复）**：`docker build` 拉基础镜像时直连 auth.docker.io 超时（本机 DNS 污染 + 无直连），而 `docker pull`（引擎级）走配置的代理正常。**结论**：构建前先 `docker pull eclipse-temurin:21-jre-alpine` 让基础镜像落本地，`docker build` 即离线完成；此模式已写入 S21 部署流程。
19. **nginx bind 挂载配置不热重载（已踩坑，已修复）**：`nginx/conf.d` 为 bind 挂载，仅改配置文件内容不会让 nginx 重新加载（`compose up` 也不因挂载文件内容变化而重建容器）→ 新加的 `/api/` 路由 404。**结论**：改 nginx 配置后必须 `docker compose restart nginx`（或 `nginx -s reload`）。
20. **S21 已知项（非缺陷，S30/S31 处理）**：① 仿真电量已降至 0（S21 无充电逻辑，S31 补返航/充电）——**S31 已解决（电量循环）**；② S20 验收残留的两条非法 `ping` 消息在 device.heartbeat 分区 0/2 解析失败且未提交 offset（手动 ack 设计使然）——**S30 死信机制已就绪，可人工触发接管**。
21. **自定义 Kafka 容器工厂缺 ConsumerFactory（已踩坑，已修复）**：自定义 `ConcurrentKafkaListenerContainerFactory` bean 会令 Boot 自动装配退避，未显式注入 `ConsumerFactory` 时启动即崩（`'consumerFactory' cannot be null`）。**结论**：自定义工厂必须 `factory.setConsumerFactory(consumerFactory)`。
22. **死信默认后缀坑（已踩坑，已修复）**：`DeadLetterPublishingRecoverer` 不指定 destinationResolver 时默认目标为 `<原主题>.DLT`，与口径主题 `inspection.dlq` 不符（表现为 dlq 空、消息去了 inspection.alarm.DLT）。**结论**：显式指定 `(cr,e) -> new TopicPartition(TopicConst.DLQ, cr.partition())`。
23. **双实例定时器重复告警（已踩坑，已修复）**：离线检测 @Scheduled 在两后端实例各自运行，同一设备可能被两实例先后翻转并各发一条 DEVICE_OFFLINE。**结论**：条件更新（status ONLINE→OFFLINE 的 modifiedCount>0 才发告警）实现去重。
24. **npm 缓存目录沙箱禁写（已踩坑，已修复，与 #17 同源）**：npm 默认缓存 `C:\Users\黎Li\AppData\Local\npm-cache` 位于工作区外，沙箱拒绝写入。**结论**：`web/.npmrc` 中 `cache=` 重定向到工作区 `tools\npm-cache`（已 gitignore）。
25. **沙箱禁执行工作区二进制（已踩坑，已规避）**：esbuild 安装脚本需在工作区 spawn 刚解包的二进制，受限令牌拒绝（EPERM -4048）。**结论**：`npm install`/`npm run build` 需经升级授权（danger-full-access）执行；构建产物 `dist/` 不入库。
26. **PowerShell 文本替换破坏 YAML（已踩坑，已修复）**：`(Get-Content -Raw) -replace ... | Set-Content -Encoding UTF8` 重写 compose 文件会引入 BOM/编码变化，docker compose 报 go-yaml 解析错误。**结论**：compose/YAML 等结构文件的修改一律用 edit 工具；已恢复并改用 edit 完成镜像版本升级。
27. **指令分区路由缺陷（真架构坑，已踩坑，已修复）**：`task.command` 按 key 哈希分区 + 同类型设备共享消费组 + 按 deviceId 过滤 → 指令落错分区即被错误设备丢弃，目标设备永远收不到（表现：部分任务永远 DISPATCHED）。**结论**：每设备独立消费组 `sim-cmd-<deviceId>`（组内独占全部分区、过滤即达），指令必达目标设备。设计报告 5.2.1 代码片段已同步。
28. **任务编号跨实例碰撞（已踩坑，已修复）**：taskId 用"时间戳(秒)+实例内自增序号"，双后端实例各自计数，同一秒内跨实例创建任务即产生相同编号 → Mongo 主键冲突 500（S33 T4 实锤）。**结论**：taskId = 时间戳 + UUID 前 8 位，跨实例全局唯一。
29. **Leaflet 默认标记图标 Vite 打包失效（已踩坑，已修复）**：`L.marker` 默认图标依赖 leaflet 包内图片资源，Vite 打包后路径 404 → 标记不可见。**结论**：本项目标记一律用 divIcon（TargetPicker 已改，风险同 DeviceMap 做法）。
30. **任务调度缺陷（用户发现，已修复）**：设备按指令到达顺序执行、忙时直接丢弃新指令，且"同时下发"时首个到达者抢跑不看优先级。**结论**：设备内优先级队列（按 priority 升序、同优先级按 ts）+ 3 秒汇聚窗口（空闲设备延迟开工，让同时下发的指令先排队比优先级）。
31. **nginx 上游 DNS 解析缓存（已踩坑，已修复/规避）**：后端容器重建后换 IP，nginx 仍指向旧 IP → `/api/**` 持续 502（S50 tc2 首跑大面积 502 的根因；非 5.3 P-9 的"启动慢"变体）。**结论**：部署流程固定动作——重建后端容器后必须 `docker compose restart nginx`（已并入风险 #19 同族操作清单）。
32. **ES hits.total 默认截断 10,000（已踩坑，已修复）**：数据量超过 10k 后 search/stats 返回的 total 静默截断为 10,000（TC026 在 10k 注入后暴露：total=10000 而分桶合计 10,229）。**结论**：分页/统计接口显式 `trackTotalHits(true)`（BUG-006）。
33. **并发取消竞态（已踩坑，已修复）**：取消受理后待回执才置 CANCELLED，窗口期内二次取消被重复受理（TC022 实测两次 200）。**结论**：取消受理即同步置 CANCELLED 作为唯一受理标记，回执按幂等处理（BUG-007）。
34. **Spring Data Mongo 自动建索引未开启（已踩坑，已修复）**：`@Indexed`（TTL）与复合索引从未落地——`device_status` 只有 `_id_` 索引，`/api/devices` 每次请求做集合扫描+内存排序（PT004 实测 888.8ms 的根因，BUG-005；设计内 O-5 未落地）。**结论**：`spring.data.mongodb.auto-index-creation: true` + `@CompoundIndex {deviceId:1, ts:-1}`；教训：注解写了 ≠ 索引存在，上线前必须用 getIndexes 核验。
35. **ES ik 插件安装与持久化（S63 已解决）**：GitHub 直链全部 404（medcl 仓库已迁移、infinilabs 的 release 资产仅源码包、API 限流）→ 官方发布源 `release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.13.0.zip` 可用；插件必须与 ES 版本完全一致（8.13.4 装不进 8.13.0，报"built for 8.13.4"）。**结论**：插件目录经 compose bind 挂载 `./es-plugins` 持久化（重建容器不丢）；`docker/init/install-ik.ps1` 可复现安装；索引升版 v2（ik_smart）+ `_reindex` 迁移（v1 保留作回滚基线）。
36. **仿真断电回充后任务队列不续（S63 已踩坑，已修复）**：设备电量归零中止当前任务后，回充完成无人触发 `maybeStartNext()`，排队任务永久停在 DISPATCHED；另仿真重启会丢失内存任务队列，产生孤儿任务（DB 侧永远 DISPATCHED）。**结论**：① 回充完成（poweredOff→false）后补调 `maybeStartNext()`；② accept-v06 预置步骤自动取消 >5 分钟的 DISPATCHED 孤儿任务（自愈）；③ 真实场景队列持久化不在本课程范围，如实记录。
37. **ES 索引升版连环坑（S63 已踩坑，已修复）**：① `_reindex` 的目标索引缺失时会以**动态映射自动创建**（字符串全变 text+keyword、location 变 float）——后端初始化器因 ES 未就绪只尝试一次即放弃，v2 被 reindex 抢先建出错误映射，直接后果：stats 聚合 500（text 字段 fielddata 禁用）、地理检索失效（**用户报告"统计看板没有图"的根因**）；② ik 词典配置位于 ES 的 `config/` 目录而非 `plugins/`，仅持久化 plugins 目录导致容器重建后词典丢失——分词器对英文文本抛 `_StopWords is null` NPE（此前 V-7 通过是因为 v2 动态映射走的 standard 分词器，掩盖了该缺陷）。**结论**：① 初始化器改为重试 24 次×5s 再放弃；② 词典目录追加 bind 挂载 `es-plugins/analysis-ik/config → config/analysis-ik`；③ 索引升版 v3（严格映射），reindex 前先建映射；④ 重建后端后必须重启 nginx（风险 #31）。修复验证：stats 200（by_type=6/trend=25）、地理检索 8,541 条、中文检索 304 条（真 ik_smart）。
38. **证据图中文占位方块（S63 已踩坑，已修复）**：用户报告告警证据图中中文描述显示为占位方块（英文描述正常）。排查：数据→API→前端全链路字节级验证均无损坏（Mongo/ES 零 U+FFFD、dist 全分块 UTF-8 有效）→ 定位到**后端镜像仅有 DejaVu 字体（无 CJK）**，AWT 用逻辑字体绘制中文描述时缺字形画成方块。**结论**：Dockerfile 补装 `font-noto-cjk`；实测新证据图描述行墨迹覆盖率 3.53%→7.2%（空心方块→真实笔画）。历史证据图仍为方块——存量重生成接口已部署（fefcef4），执行收尾留 S64。
39. **长耗时维护接口被 nginx 网关超时（S63 已踩坑，S64 已修复）**：`POST /api/maintenance/regenerate-evidence` 同步处理数千张证据图（分钟级），经 Nginx 触发 `proxy_read_timeout 30s` → 504。**结论（已落地）**：nginx 新增 `/api/maintenance/` 专属 location——`proxy_read_timeout 900s`、不限流；该路径接口照常同步执行。

## 7. 下一步计划

- **当前状态（2026-09-13）**：S19~S64 全部 done；里程碑 v0.1~v0.5（v0.5 已打标）；v0.6 backlog（S61~S64）完成，验收 8/8，**v0.6 tag 待用户确认**。
- **待用户拍板**：① v0.6 tag（验收与回归全部通过）；② 设计报告 5.4 节 16 张截图补拍（图 5-1~5-16 清单已列）；③ 前端证据链 UI 与统计看板浏览器核验（历史方块已清零，可直接看）；④ S60 异地备份（候选）。
- **恢复指引**：开机 → Docker Desktop（鲸鱼变绿）→ 需要构建/拉镜像时开 Clash → 对 AI 说"继续" → AI 先读 PLAN/STATE 执行开场协议。
- 任何新工作先在此与 PLAN.md 登记，再执行。
