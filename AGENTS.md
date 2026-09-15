# AGENTS.md — BuildCraft NeoForge 26.1.2 迁移

## 0. 新 agent 开工流程（三步）

1. 通读本文件。
2. 读 `migration/PROGRESS.md`，了解迁移进度与代码实时指标。
3. 从 `migration/tasks.json` 挑选 `status=pending` 或 `in_progress` 的任务，认领后将其置为 `in_progress`。

开工前环境准备：新 clone 必须先执行 `git submodule update --init --recursive`。本仓库含三个子模块：BuildCraftAPI、BuildCraft-Localization、BuildCraftGuide。API 源码与全部语言文件都在子模块里，缺了无法编译。

## 1. 项目背景（事实，供决策参考）

- 本仓库是 BuildCraft 8.0.x 的 fork，共 7382 个提交，长期从 1.12.2 手工逐版本移植。当前基线：Minecraft 1.20.1 + Forge 47.3.12 + Java 17 + Gradle 8.8 + ForgeGradle 6。
- 规模：主体 1510 个 Java 文件约 18.3 万行；BuildCraftAPI 子模块另有 262 个文件；一个 jar 包含 8 个 mod（buildcraftlib、buildcraftcore、buildcraftbuilders、buildcraftenergy、buildcraftfactory、buildcraftsilicon、buildcrafttransport、buildcraftrobotics）。
- 架构收口（迁移改造的杠杆点）：
  - 注册集中在 `common/buildcraft/lib/registry/RegistrationHelper.java`——全仓仅 9 处 `DeferredRegister.create`，但 `RegistryObject` 有 38 个文件共 362 处引用。
  - 网络集中在 `common/buildcraft/lib/net/MessageManager.java`——自研 IMessage 体系，共 16 个消息类。
  - datagen 共 56 个文件，生成 4404 个 json。
- 已知坑：
  - client 代码与主源集混编：362 个文件靠 267 处 `@OnlyIn(Dist.CLIENT)` 区分，没有独立 client 源集。
  - 60 条 AccessTransformer；mixins 配置存在但为空。
  - 双许可证并存（LICENSE = MMPL 1.0.1 与 LICENSE-NEW = MPL 2.0），切换未决。
- 分支约定：`neoforge-26.1.2` 是迁移工作分支；`8.0.x-1.20.1` 是基线分支，只读参照，禁止改动。

## 2. 迁移目标（钉死，不得自行变更）

迁移终点：Minecraft 26.1.2 + NeoForge 26.1.2.109 + Java 25 + Gradle 9.1+ + ModDevGradle 2.0.141+。

禁止"顺手升级"或自行调整任何版本；版本调整只能由用户/主 agent 决策。

## 3. 路线图与量化门禁

四个阶段（详细任务与验收标准见 `migration/tasks.json`）：

- **Phase 0 基线固化**（1.20.1 上）：构建跑通、注册表/datagen 快照、特征测试、静态分析基线。
- **Phase 1 工程化整备**（1.20.1 上）：多项目拆分、client 源集、完整 CI、死代码清理、许可证决策。
- **Phase 2 分层迁移**：构建系统 → 垂直切片（buildcraftcore）→ 纯逻辑 → 注册层 → 网络 → DataComponents → 渲染 → 世界生成 → 清零。
- **Phase 3 验证金字塔**：GameTest、注册表/datagen/语言键对拍、覆盖率门禁、冒烟。

原则：Phase 0/1 未完成不得开始 Phase 2 大面积铺开；每阶段退出以 `tasks.json` 里的 acceptance 命令输出为准，不以"感觉完成"为准。

## 4. 进度追踪规则（强制，每个工作会话必须遵守）

- `migration/tasks.json` 是唯一事实源；`migration/PROGRESS.md` 是生成物，禁止手改。
- 会话结束前必须完成：
  1. `python3 migration/scripts/progress.py --set <ID> <STATUS> ["evidence 描述"]` 更新所做任务；
  2. `git commit` 同时包含 `tasks.json` 与重新生成的 `PROGRESS.md`。
- 状态定义与判断标准：

  | STATUS | 含义 | 判断标准 |
  |---|---|---|
  | `pending` | 未完成 | 未开工 |
  | `in_progress` | 进行中 | 已认领，有部分产出但未达 acceptance |
  | `partial` | 部分完成 | acceptance 只满足一部分（evidence 必须写明已满足哪些、缺哪些） |
  | `unverified` | 未验证 | 实现已写完，但 acceptance 里的验证命令未执行或未通过——不允许直接标 done |
  | `done` | 已完成 | acceptance 的验证命令已实际执行且通过；evidence 必须包含可复现命令与结果摘要 |

  没有命令输出的 `done` 视为无效，会被 `--check` 与验收打回。
- 新增子任务：追加到 `tasks.json` 对应 phase，id 按现有序列递增（如 M2.10），acceptance 必填。

## 5. CI 与提交规则

- push 前本地必须通过：`python3 migration/scripts/progress.py --check`。
- `.github/workflows/ci.yml` 有两个 job：
  - `progress-check`：必须绿。
  - `baseline-build`：M0.2 完成前允许红——让基线构建变绿本身就是 M0.2 任务；M0.2 完成后必须绿，红了不得合入后续任务。
- 提交规范：
  - 主题行以任务号前缀，如 `M0.2: fix gradle build` 或 `migration: ...`。
  - 小步提交，一个提交对应一个可验收单元。
  - 禁止 `push --force`；禁止修改/禁用 CI workflow 来"让灯变绿"。

## 6. 命令速查

- 子模块：`git submodule update --init --recursive`
- 构建/测试：`./gradlew build`、`./gradlew test`（Phase 2 迁移构建系统后，命令以新构建文件为准并更新本节）
- 进度：
  - `python3 migration/scripts/progress.py`——生成报告
  - `python3 migration/scripts/progress.py --check`——CI 校验
  - `python3 migration/scripts/progress.py --set ID STATUS [EVIDENCE]`——更新任务状态
- 基线参照：`migration/snapshots/` 下存放 Phase 0 产生的对拍基线（如 registry-baseline.json），只增不改。

## 7. 硬性约束（红线）

1. `buildcraft_resources_generated/` 是 datagen 产物，禁止手改，只能通过 datagen 任务重新生成。
2. 禁止改动 `.gitmodules` 与子模块 pointer；禁止改动 `8.0.x-1.20.1` 基线分支。
3. `migration/snapshots/` 基线文件一旦入库不得修改（保证对拍基准的唯一性）。
4. 许可证未决（M1.5）前，新增源码文件不加许可证头；不得批量改动现有文件头。
5. 大面积注释代码/Calen 移植注释的清理只在 M1.4 任务内进行，日常改动不顺手删除。
6. 机械化替换（如 RegistryObject→DeferredHolder）前后必须用 grep 计数对比并写入 evidence（防止被 3546 行注释代码污染匹配）。
7. 不引入新第三方依赖，除非任务 acceptance 明确要求。

## 8. 任务执行边界

- 每个任务的完成判据 = acceptance 中命令的实际输出；报告"完成"必须附 evidence。
- 遇到与 AGENTS.md 冲突、验收标准含糊、需要架构决策的情况：停下标注"待主 agent/用户决策"，不要自行发挥。
