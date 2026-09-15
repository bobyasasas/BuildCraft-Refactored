# BuildCraft 迁移进度仪表盘

> **本文件由 migration/scripts/progress.py 自动生成，禁止手改；更新任务请编辑 migration/tasks.json 或用 `--set` 命令。**
>
> 生成时间：2026-09-14 22:07:44 ｜ 数据源：migration/tasks.json（schema=1，updated=2026-09-14）

## 总览

**总任务数 28** ｜ pending(未完成)=26、in_progress(进行中)=0、partial(部分完成)=0、unverified(未验证)=0、done(已完成)=2 ｜ **完成率 7%**（权重：done=1，in_progress/partial/unverified=0.5，pending=0）

## 阶段汇总

| 阶段 | 任务数 | done | 完成率 |
|---|---:|---:|---:|
| Phase 0 基线固化 | 6 | 2 | 33% |
| Phase 1 工程化整备 | 5 | 0 | 0% |
| Phase 2 分层迁移到 MC 26.1.2 + NeoForge 26.1.2.109 | 9 | 0 | 0% |
| Phase 3 功能验证金字塔 | 8 | 0 | 0% |

## 任务明细

| ID | 任务 | 状态 | 验收标准 | evidence / notes |
|---|---|---|---|---|
| M0.1 | 初始化 git 子模块 | done（已完成） | git submodule status 无 '-' 前缀 | git submodule update --init --recursive 后三个子模块全部检出（BuildCraftAPI 262 java） |
| M0.2 | 基线构建跑通 | done（已完成） | ./gradlew build 退出码 0，CI baseline-build job 变绿 | evidence：./gradlew build 退出码0（本机，2026-09-14，BUILD SUCCESSFUL in 3m 58s）；CI run 待补记 baseline-build 绿 |
| M0.3 | 注册表快照工具 | pending（未完成） | 工具可导出 buildcraft 全部 registry id（方块/物品/方块实体/实体/流体/配方/标签）到 migration/snapshots/registry-baseline.json 并入库 | — |
| M0.4 | datagen 快照入库 | pending（未完成） | datagen 任务产物与 buildcraft_resources_generated/ 现有 4404 个 json diff=0 且纳入版本管理 | — |
| M0.5 | 纯逻辑模块特征测试 | pending（未完成） | expression 库与配方/蓝图核心逻辑的 JUnit 特征测试基线全绿，测试数量记录在 evidence | — |
| M0.6 | 静态分析基线 | pending（未完成） | SpotBugs/Checkstyle 基线报告入库，违规总数记录在 evidence | — |
| M1.1 | Gradle 多项目拆分 | pending（未完成） | settings.gradle 正式 include BuildCraftAPI/expression/主体模块，各模块可独立构建，srcDir 拼装方式移除 | — |
| M1.2 | client 源集分离 | pending（未完成） | client 类移入专用源集，@OnlyIn(Dist.CLIENT) 从基线 279 处显著下降，具体数字记录在 evidence | — |
| M1.3 | 完整 CI 流水线 | pending（未完成） | GitHub Actions 含 build+test+JaCoCo 覆盖率报告并上传 artifact | — |
| M1.4 | 死代码与卫生清理 | pending（未完成） | 注释掉的代码行（基线 3546）与 Calen 移植注释（基线 688）清零或显著下降；test.py、testsuite/、.travis.yml 移除；数字记录在 evidence | — |
| M1.5 | 许可证决策落地 | pending（未完成） | MMPL 1.0.1 与 MPL 2.0 二选一，mods.toml 与 LICENSE 一致，license_checker 脚本通过 | — |
| M2.1 | 构建系统迁移 | pending（未完成） | ModDevGradle + Java 25 + Gradle 9.1，空 mod 骨架在 26.1.2 下 runClient 可启动 | — |
| M2.2 | 垂直切片：buildcraftcore | pending（未完成） | 核心方块（发动机/基础管道）在 26.1.2 全链路（注册/逻辑/渲染/GameTest）可用 | — |
| M2.3 | 纯逻辑模块迁移 | pending（未完成） | expression 等纯逻辑模块 0 个 Forge import 且特征测试全绿 | — |
| M2.4 | 注册层迁移 | pending（未完成） | RegistrationHelper/BC*Blocks 等 3 处注册基类重写为 NeoForge DeferredRegister，RegistryObject 引用（基线 42 文件 362 处）全部改为 DeferredHolder 并清零 | — |
| M2.5 | 网络层迁移 | pending（未完成） | MessageManager 基于 CustomPacketPayload/StreamCodec 重写，16 个消息类全部适配，客户端-服务端握手可用 | — |
| M2.6 | 数据组件迁移 | pending（未完成） | 物品 NBT 持久化全部迁移到 DataComponents（物品过滤/门配置/机器人参数），序列化行为与基线对拍一致 | — |
| M2.7 | 渲染迁移 | pending（未完成） | 19 个 BlockEntityRenderer 与自研模型系统在 26.1.2 渲染管线（extractRenderState 模式）下正常工作 | — |
| M2.8 | 世界生成迁移 | pending（未完成） | biome/结构注册迁移完成，油田世界生成验证可用 | — |
| M2.9 | 迁移清零 | pending（未完成） | grep -r "net.minecraftforge" 全部 .java 结果为 0，8 个 mod 在 26.1.2 全部加载 | — |
| M3.1 | GameTest 基建 | pending（未完成） | headless GameTest 可在 CI 中运行并输出 x/y passing 统计 | — |
| M3.2 | 核心功能 GameTest | pending（未完成） | 管道传输/引擎功率/采石场/填充器/门逻辑/机器人各有至少 1 个用例且全部通过 | — |
| M3.3 | 注册表对拍 | pending（未完成） | 26.1.2 registry dump 与 migration/snapshots/registry-baseline.json diff=0（白名单外） | — |
| M3.4 | datagen 对拍 | pending（未完成） | 26.1.2 datagen 产物与基线 diff=0（白名单外） | — |
| M3.5 | 语言键对拍 | pending（未完成） | BuildCraft-Localization 语言键集合与基线 diff=0 | — |
| M3.6 | 覆盖率门禁 | pending（未完成） | JaCoCo 对核心包设行覆盖率阈值并在 CI 强制失败 | — |
| M3.7 | 冒烟与性能 | pending（未完成） | headless runServer 启动无 crash，8 个 mod 全部加载，启动时间记录在 evidence | — |
| M3.8 | 迁移仪表盘 | pending（未完成） | progress.py 输出含 Forge import 等指标的趋势记录，随 CI 自动更新 | — |

## 代码实时指标

采集时间：2026-09-14 22:07:44；采集范围：仓库根目录（排除 .git、.gradle、build、buildcraft_resources_generated）。

| 指标 | 当前值 | 调研基线(2026-09) | 目标 |
|---|---:|---:|---|
| .java 文件总数 | 1,772 | 1772（main 1510 + API 子模块 262） | 无目标(参考) |
| .java 总行数 | 194,560 | — | 无目标(参考) |
| Forge import 文件数 | 549 | 549 | 0 |
| Forge import 出现次数 | 1,409 | — | 0 |
| TODO 出现次数 | 222 | 222 | 随 M1.4 下降 |
| FIXME 出现次数 | 24 | 24 | 随 M1.4 下降 |
| @OnlyIn(Dist.CLIENT) 出现次数 | 279 | 279 | 随 M1.2 下降 |
| RegistryObject 涉及文件数 | 42 | 42 | 0 |

## 维护方式

- 更新任务状态：`python3 migration/scripts/progress.py --set M0.2 in_progress "证据文本"`
- 校验数据：`python3 migration/scripts/progress.py --check`
- 重新生成本文件：`python3 migration/scripts/progress.py`
