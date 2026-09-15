# 静态分析基线（M0.6）

- 日期：2026-09-15（机器本地时间；报告文件生成于 00:02~00:03）
- 代码基点：分支 `neoforge-26.1.2`，HEAD = `28806f6ae`，叠加 M0.6 的构建配置改动（`build.gradle` + `config/checkstyle/checkstyle.xml`）
- 定位：迁移前静态分析**基线快照**，只记录现状、不修违规、不上门禁（两工具均 `ignoreFailures = true`，`build`/`check` 不因违规失败）。门禁留待 Phase 3。

## 工具与版本

| 工具 | Gradle 插件 | 工具版本 |
| --- | --- | --- |
| Checkstyle | Gradle 内置 `checkstyle` 插件 | 10.12.5 |
| SpotBugs | `com.github.spotbugs` 6.0.28 | 4.8.6 |

配置要点：

- 规则集：`config/checkstyle/checkstyle.xml`（刻意宽松：FileTabCharacter、LineLength(max=240)、UnusedImports、RedundantImport、NeedBraces、EmptyStatement、SimplifyBooleanExpression、EqualsHashCode、DefaultComesLast；支持 `@SuppressWarnings` 抑制）。
- SpotBugs：`effort = MAX`、`reportLevel = LOW`（记录全部等级），仅分析 main。
- 报告输出（不入库）：`build/reports/checkstyle/main.{xml,html}`、`build/reports/spotbugs/main.{xml,html}`。
- 复现：`./gradlew checkstyleMain spotbugsMain`（或 `./gradlew check`）。

## 分析范围

main 源集 Java 代码（含 `build.gradle` 中 srcDir 拼装的全部目录），共 **1745** 个 `.java` 文件：

| 源目录 | 文件数 |
| --- | --- |
| `src/main/java` | 0（空目录） |
| `common` | 1317 |
| `BuildCraftAPI/api` | 262 |
| `sub_projects/expression/src/main/java` | 64 |
| `sub_projects/expression/src/autogen/java` | 102 |

未覆盖：`api` 源集（与 BuildCraftAPI/api 完全重复、且历史上从未参与编译，无 MC classpath 一编即失败，本次保持其不编译现状）；`test` 源集（非基线范围）。Checkstyle 实际扫描 1745 个文件，与上述范围一致。

## Checkstyle 违规

**总数：1228**（severity 均为 warning），分布在 **327 / 1745** 个文件。

| 规则 | 数量 |
| --- | --- |
| NeedBraces | 650 |
| UnusedImports | 290 |
| RedundantImport | 200 |
| LineLength（>240） | 72 |
| DefaultComesLast | 7 |
| EqualsHashCode | 5 |
| SimplifyBooleanExpression | 4 |
| FileTabCharacter | 0 |
| EmptyStatement | 0 |

## SpotBugs 违规

**总数：2525**

按等级（SpotBugs priority，1=High / 2=Medium / 3=Low）：

| 等级 | 数量 |
| --- | --- |
| High | 274 |
| Medium | 1507 |
| Low | 744 |

按类别：MALICIOUS_CODE 1097、STYLE 595、BAD_PRACTICE 591、CORRECTNESS 113、PERFORMANCE 98、MT_CORRECTNESS 20、I18N 11。

Top 15 BugPattern：

| BugPattern | 数量 |
| --- | --- |
| PA_PUBLIC_PRIMITIVE_ATTRIBUTE | 391 |
| EI_EXPOSE_REP2 | 238 |
| MS_CANNOT_BE_FINAL | 232 |
| EI_EXPOSE_REP | 205 |
| URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD | 156 |
| MS_PKGPROTECT | 124 |
| PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE | 107 |
| MS_SHOULD_BE_FINAL | 97 |
| MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR | 88 |
| DLS_DEAD_LOCAL_STORE | 75 |
| MS_MUTABLE_ARRAY | 59 |
| BC_UNCONFIRMED_CAST | 59 |
| RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE | 58 |
| NP_NONNULL_PARAM_VIOLATION | 57 |
| CT_CONSTRUCTOR_THROW | 53 |

## 构建验证

- `./gradlew check --daemon`：BUILD SUCCESSFUL（exit 0），生成上述两份报告。
- `./gradlew build --daemon`：复验 BUILD SUCCESSFUL（exit 0）。
