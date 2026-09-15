# BuildCraft 迁移进度仪表盘

> **本文件由 migration/scripts/progress.py 自动生成，禁止手改；更新任务请编辑 migration/tasks.json 或用 `--set` 命令。**
>
> 生成时间：2026-09-14 23:38:17 ｜ 数据源：migration/tasks.json（schema=1，updated=2026-09-14）

## 总览

**总任务数 28** ｜ pending(未完成)=23、in_progress(进行中)=0、partial(部分完成)=0、unverified(未验证)=0、done(已完成)=5 ｜ **完成率 18%**（权重：done=1，in_progress/partial/unverified=0.5，pending=0）

## 阶段汇总

| 阶段 | 任务数 | done | 完成率 |
|---|---:|---:|---:|
| Phase 0 基线固化 | 6 | 5 | 83% |
| Phase 1 工程化整备 | 5 | 0 | 0% |
| Phase 2 分层迁移到 MC 26.1.2 + NeoForge 26.1.2.109 | 9 | 0 | 0% |
| Phase 3 功能验证金字塔 | 8 | 0 | 0% |

## 任务明细

| ID | 任务 | 状态 | 验收标准 | evidence / notes |
|---|---|---|---|---|
| M0.1 | 初始化 git 子模块 | done（已完成） | git submodule status 无 '-' 前缀 | git submodule update --init --recursive 后三个子模块全部检出（BuildCraftAPI 262 java） |
| M0.2 | 基线构建跑通 | done（已完成） | ./gradlew build 退出码 0，CI baseline-build job 变绿 | evidence：./gradlew build 退出码0（本机，2026-09-14，BUILD SUCCESSFUL in 3m 58s）；CI run 34920120110 baseline-build 绿（1m32s）、progress-check 绿 |
| M0.3 | 注册表快照工具 | done（已完成） | 工具可导出 buildcraft 全部 registry id（方块/物品/方块实体/实体/流体/配方/标签）到 migration/snapshots/registry-baseline.json 并入库 | evidence：工具 common/buildcraft/datagen/base/BCRegistrySnapshotGenerator 挂在 GatherDataEvent（注册于 BCDataGenerators 末尾，最后执行）；复跑命令 rm -rf buildcraft_resources_generated/.cache && ./gradlew runData；registry-baseline.json 计数 blocks=75, items=974, block_entities=35, entities=17, fluids=60, recipes=1561, tags: block=1/item=66/fluid=1/biome=1，共2791个id全部排序；快照跨两轮全量重跑 md5=6f36e2b1a22d84d2934383ba603c8738 字节级一致 |
| M0.4 | datagen 快照入库 | done（已完成） | datagen 任务产物与 buildcraft_resources_generated/ 现有 4404 个 json diff=0 且纳入版本管理 | evidence：datagen 确定性修复5处：BCItemTagsGenerator.addAllOptional 按RegistryObject id排序（paintbrush+pipe系列tags）；PipeRegistry.getAllRegisteredPipes 按identifier排序（pipe颜色tags+atlas贴图+item model遍历）；SpriteHolderRegistry/ModelHolderRegistry 的 onDatagenTextureRegister 按location排序（atlas sources）；SiliconAssemblyRecipeGenerator input 集合 HashSet→LinkedHashSet（plug_pulsar requiredStacks）；复跑命令 rm -rf buildcraft_resources_generated/.cache && ./gradlew runData 连续两轮全量重跑，4643个产物文件 sha256 两轮完全一致（字节级）；最终 vs HEAD：改65/删1/增0，65处全部为 values/requiredStacks 数组顺序差异（python 多重集比对语义相等0差异，loot/models/recipes/advancements 无一涉改），修复后文件内容稳定按 id 字典序；json 总数 4403（stale data/buildcraft/tags/blocks/tags/blocks/soft.json.json 由 datagen 清理 4404→4403）；migration/snapshots/registry-baseline.json 复跑后字节级不变未触碰；本机 ./gradlew build BUILD SUCCESSFUL |
| M0.5 | 纯逻辑模块特征测试 | done（已完成） | expression 库与配方/蓝图核心逻辑的 JUnit 特征测试基线全绿，测试数量记录在 evidence | evidence：./gradlew test 退出码0（2026-09-14，29个测试类/153个测试全绿，其中基线原有20类54个未被改动）；新增特征测试99个：expression 45个（ExpressionOpsCharacterizationTester 17：整数字面量为long/优先级怪癖[%比*松、^按位异或且绑定最紧]/~取反/移位mod64/整除与浮点除/除零行为/跨类型字符串拼接/交叉类型==；ExpressionFunctionsCharacterizationTester 18：常量与函数大小写不敏感/round-floor-ceil-sign-clamp-min-max-pow-log-trig/字符串length-char_at-substring边界怪癖/VecLong+-length-distance/叉积非标准实现怪癖/vec除零抛异常/编译函数复用/变量大小写不敏感/类型名占用；ExpressionErrorsCharacterizationTester 10：括号/尾运算符/未知变量与函数/参数个数/double异或/'>>>'不可编译怪癖/词法合并怪癖等错误路径）+ 配方蓝图54个（SnapshotIndexCharacterizationTester 6：posToIndex=((z*Y)+y)*X+x具体值/数据尺寸/实例静态一致；TemplateSnapshotCharacterizationTester 9：Template NBT往返/超长数据拒绝/invert/computeKey确定性SHA-256/copy独立性/边界检查；NbtRuleCharacterizationTester 11：NbtPath遍历与NBT_NULL哨兵/EnumNbtCompareOperation按序列化JSON比对/JsonSelector字符串简写与对象形式/规则JSON裸数字变DoubleTag永不匹配int怪癖；FillerPatternCharacterizationTester 11：fill/box/frame/pyramid/stairs/sphere(hollow与filled)/2d圆与方的具体每层格子数基线/none返回false/clear不改模板怪癖；AssemblyRecipeBasicCharacterizationTester 11：输出匹配/数量门槛/空物品栈/多原料AND/相等按id/元数据访问器；BoxCharacterizationTester 6：min-max归一化/setMax语义/半开边界/NBT往返）；测试类路径src/test/java与sub_projects/expression/src/test/java（JUnit4.12），未改生产源码与build.gradle。覆盖边界：RulesLoader/SchematicBlockManager/Blueprint调色板序列化/AssemblyRecipeRegistry依赖Forge运行期注册表或Level，未覆盖 |
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

采集时间：2026-09-14 23:38:17；采集范围：仓库根目录（排除 .git、.gradle、build、buildcraft_resources_generated）。

| 指标 | 当前值 | 调研基线(2026-09) | 目标 |
|---|---:|---:|---|
| .java 文件总数 | 1,782 | 1772（main 1510 + API 子模块 262） | 无目标(参考) |
| .java 总行数 | 196,167 | — | 无目标(参考) |
| Forge import 文件数 | 550 | 549 | 0 |
| Forge import 出现次数 | 1,412 | — | 0 |
| TODO 出现次数 | 222 | 222 | 随 M1.4 下降 |
| FIXME 出现次数 | 24 | 24 | 随 M1.4 下降 |
| @OnlyIn(Dist.CLIENT) 出现次数 | 279 | 279 | 随 M1.2 下降 |
| RegistryObject 涉及文件数 | 42 | 42 | 0 |

## 维护方式

- 更新任务状态：`python3 migration/scripts/progress.py --set M0.2 in_progress "证据文本"`
- 校验数据：`python3 migration/scripts/progress.py --check`
- 重新生成本文件：`python3 migration/scripts/progress.py`
