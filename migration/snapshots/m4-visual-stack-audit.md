# M4.0 审计：BuildCraft 1.20.1 客户端视觉栈 → 26.1.2 移植清单

> 只读审计产出。基线分支 `8.0.x-1.20.1`（单源 common 结构，234 个 client 侧 java 文件），目标分支 `neoforge-26.1.2`。
> 所有类名/路径均经 `git ls-tree` / `git show` 逐个核对。路径前缀 `common/buildcraft/` 简写省略。
> 注意：BuildCraftAPI、BuildCraftGuide、BuildCraft-Localization 是 git 子模块（本仓库内无内容），API 侧接口（IPipeFlowRenderer、PluggableModelKey 等）不在本次盘点范围但移植时会碰到。

---

## 0. 26.1.2 分支现状（审计基点）

任务描述称"2 个 BER"，实际 HEAD（`neo/src/client/java/`）已有更多，均为**简化的缩微版**：

| 类 | 路径（HEAD） | 状态 |
|---|---|---|
| StoneEngineBlockRenderer | neo/src/client/java/buildcraft/core/client/render/StoneEngineBlockRenderer.java | M2.7b 参考实现：静态基座（vanilla orientable 模型）+ 手写活塞几何，extract/submit 双阶段 |
| KinesisPipeBlockRenderer | neo/src/client/java/buildcraft/core/client/render/KinesisPipeBlockRenderer.java | M2.7b：全管道体 BER 自绘（自动连接），blockstate 模型只剩粒子贴图（chest 模式） |
| FillerBlockRenderer | neo/src/client/java/buildcraft/builders/client/render/FillerBlockRenderer.java | M2.12：仅画工作区盒框+当前格（缩微版，非完整 1.20.1 蓝图渲染） |
| QuarryBlockRenderer | neo/src/client/java/buildcraft/builders/client/render/QuarryBlockRenderer.java | M2.12：仅画矿区轮廓+当前钻探格（无激光框/钻头/框架动画） |
| RobotEntityRenderer | neo/src/client/java/buildcraft/robotics/client/render/RobotEntityRenderer.java | M2.13：盒子小人（非 1.20.1 分层模型） |
| BcQuad / BcVertex / BcQuadSmoke / BcBoxes | neo/src/client/java/buildcraft/lib/client/render/ | 几何基元（MutableQuad 的 26.1.2 雏形） |
| BcEnergyFluidModels | neo/src/client/java/buildcraft/energy/client/BcEnergyFluidModels.java | 流体 still/flow 模型注册 |

- **无任何 GUI/Menu**（`git grep Menu/Screen neo/src` 为空）。
- **无任何自定义贴图**：`neo/src/main/resources/assets` 里 textures 计数为 0，占位静态模型全部引用 vanilla 贴图（如 engine_stone.json 用 furnace 贴图）。
- `neo/docs/render-pipeline-26.1.2.md`（M2.7a）已查清 26.1.2 渲染管线：**extract（构建不可变 RenderState）/ submit（发射绘制命令）双阶段**，1.20.1 的 `render(be, partialTick, poseStack, MultiBufferSource, light, overlay)` 单体签名已不存在。所有 BER 移植必须遵守该范式。

---

## 1. 渲染器清单（8.0.x-1.20.1）

### 1.1 注册链路
- BER：各模块 `BC*Models.java`（builders 在 `BCBuilders.java` 内）持有 `@SubscribeEvent onTesrReg(EntityRenderersEvent.RegisterRenderers)`，经 `lib/misc/RegistryUtil.java` 的 `regTesrIfTilePresent` 注册。
- 实体渲染器：`BCRoboticsModels` 里 `EntityRenderers.register(robot, RenderRobot::new)`。
- 全局世界内叠加渲染（体积盒/激光路径）：`core/client/RenderTickListener.java` 监听 `RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS`。

### 1.2 core（3 BER + 全局渲染）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderEngineWood | core/client/render/RenderEngineWood.java | 木引擎 TileEngineRedstone_BC8（燃烧状态/朝向动态） | 29 |
| RenderEngineCreative | core/client/render/RenderEngineCreative.java | 创造引擎 TileEngineCreative | 29 |
| RenderMarkerVolume | core/client/render/RenderMarkerVolume.java | 体积标记 TileMarkerVolume 的激光盒 | 124 |
| RenderVolumeBoxes | core/client/render/RenderVolumeBoxes.java | 静态工具：所有玩家体积盒的 DetachedRenderer 实现 | ~100 |
| RenderTickListener | core/client/RenderTickListener.java | AFTER_TRANSLUCENT_BLOCKS 阶段驱动所有 DetachedRenderer（体积盒/路径激光） | 283 |
| BuildCraftLaserManager | core/client/BuildCraftLaserManager.java | 激光类型常量表（POWER_LOW/MED/HIGH/FULL 等，引 `buildcraftcore:textures/lasers/`） | 91 |

### 1.3 energy（4 BER）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderEngineStone | energy/client/render/RenderEngineStone.java | 石引擎（jsonbc 动态模型） | 29 |
| RenderEngineIron | energy/client/render/RenderEngineIron.java | 铁引擎 | 29 |
| RenderEngineRF | energy/client/render/RenderEngineRF.java | RF 引擎 | 26 |
| RenderDynamoMJ | energy/client/render/RenderDynamoMJ.java | MJ 发电机 TileDynamoMJ（jsonbc） | 50 |
| LedgerDynamoMJ + GuiDynamoMJ 等 GUI | 见 §3 | — | — |

引擎公共基类：**lib/client/render/tile/RenderEngine_BC8.java（59 行）**——从 `BCEnergyModels`/`BCCoreModels` 取变量化 jsonbc 四边形（progress/stage/direction 三变量），逐面片乘光照后经 RenderType.cutout() 画。铁/石/RF/木/创造 5 个子类都只有 26-29 行（提供 quads getter）。

### 1.4 factory（6 BER）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderMiningWell | factory/client/render/RenderMiningWell.java | 采矿井：井管 + 管内下降的钻头物品 | 165 |
| RenderPump | factory/client/render/RenderPump.java | 抽液泵：吸管 + 管内流体柱 | 186 |
| RenderTank | factory/client/render/RenderTank.java | 罐内流体（FluidRenderer，连通罐顶/底自适应） | 129 |
| RenderDistiller | factory/client/render/RenderDistiller.java | 蒸馏器 3 个内部流体罐（静态机身之外） | 206 |
| RenderHeatExchange | factory/client/render/RenderHeatExchange.java | 热交换器：全 BER（blockstate=builtin/entity），含两侧流体+火焰 | 266 |
| RenderTube | factory/client/render/RenderTube.java | 采矿管 TileMiner（Tube 方块） | 58 |
| ModelHeatExchange | factory/client/model/ModelHeatExchange.java | 热交换几何/变量上下文（配合 heat_exchange_static.jsonbc） | 157 |

### 1.5 builders（5 BER + 快照渲染）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderBuilder | builders/client/render/RenderBuilder.java | 建造机蓝色盒 + 当前快照 | 106 |
| RenderFiller | builders/client/render/RenderFiller.java | 填充器工作区 + 当前模式蓝图 | 66 |
| RenderQuarry | builders/client/render/RenderQuarry.java | 采石场：激光框架（LaserBoxRenderer）+ 钻头 + 能量光束 | 312 |
| RenderArchitectTable | builders/client/render/RenderArchitectTable.java | 建筑师台（委托 RenderArchitectTables） | 63 |
| RenderArchitectTables | builders/client/render/RenderArchitectTables.java | 共享 DetachedRenderer：蓝图盒渲染 | 225 |
| RenderSnapshotBuilder | builders/client/render/RenderSnapshotBuilder.java | 静态工具：建造机/填充器共用的快照体渲染 | 140 |
| RenderMarkerConstruction | builders/client/render/RenderMarkerConstruction.java | 建造标记连线盒 | 167 |
| HudSingleSchematic | builders/client/render/HudSingleSchematic.java | 手持蓝图 HUD（准星处小图） | 31 |
| AdvDebuggerQuarry | builders/client/render/AdvDebuggerQuarry.java | 调试用 | 89 |
| AddonRendererFillerPlanner | builders/addon/AddonRendererFillerPlanner.java | 填充规划器区域渲染 | 105 |

### 1.6 silicon（2 BER + 2 门插件动态渲染器）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderLaser | silicon/client/render/RenderLaser.java | 激光发射台光束（LaserRenderer_BC8） | 83 |
| RenderProgrammingTable | silicon/client/render/RenderProgrammingTable.java | 编程台桌面物品旋转展示 | 67 |
| PlugGateRenderer | silicon/client/render/PlugGateRenderer.java | 门的动态部分（IPlugDynamicRenderer，gate_dynamic.jsonbc：LED/扩展状态） | 50 |
| PlugPulsarRenderer | silicon/client/render/PlugPulsarRenderer.java | 脉冲器指示灯 | 45 |

### 1.7 transport（1 总 BER + 6 子渲染器）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderPipeHolder | transport/client/render/RenderPipeHolder.java | TilePipeHolder 总入口：分发 wires→pluggables→flows→behaviours | 108 |
| PipeWireRenderer | transport/client/render/PipeWireRenderer.java | 管上红石线（4 色，含未连接/连接两态贴图） | 319 |
| PipeFlowRendererItems | transport/client/render/PipeFlowRendererItems.java | 管内移动物品（ItemRenderer + 路径插值） | 98 |
| PipeFlowRendererFluids | transport/client/render/PipeFlowRendererFluids.java | 管内流体滴（FluidRenderer） | 166 |
| PipeFlowRendererPower | transport/client/render/PipeFlowRendererPower.java | MJ 能量管内部条纹（power_*/overload 贴图） | 161 |
| PipeFlowRendererRf | transport/client/render/PipeFlowRendererRf.java | RF 管内部条纹 | 155 |
| PipeBehaviourRendererStripes | transport/client/render/PipeBehaviourRendererStripes.java | 条纹管动画 | 41 |
| PipeTabButton | transport/client/render/PipeTabButton.java | 管道帽盖渲染 | 279 |
| PipeBlockColours | transport/client/PipeBlockColours.java | 管道染色（BlockColor） | ~60 |

注意：管道**本体**不在这层——它是 chunk 烘焙模型（见 §2 管道几何生成）。

### 1.8 robotics（1 BER + 1 实体渲染器 + 地图）

| 类 | 路径 | 渲染对象 | 行数 |
|---|---|---|---|
| RenderRobot | robotics/client/render/RenderRobot.java | EntityRobot：分层模型（本体/能量层/睡眠/盔甲层），577 行，最复杂实体渲染器 | 577 |
| RenderZonePlanner | robotics/client/render/RenderZonePlanner.java | Zone Planner 方块 + 悬浮地图 | 202 |
| ZonePlannerMapRenderer | robotics/zone/ZonePlannerMapRenderer.java | 世界地图贴图离屏渲染（供 zone planner 选区） | 180 |
| PlugRobotStationRenderer | robotics/client/render/PlugRobotStationRenderer.java | 机器人停靠站动态部分 | 49 |
| EntityRobotEnergyParticle | robotics/client/particle/EntityRobotEnergyParticle.java | 机器人能量粒子 | 144 |

### 1.9 lib 渲染基础设施（被上面全部依赖）

| 子系统 | 类（路径 lib/client/render/...） | 行数 | 职责 |
|---|---|---|---|
| 流体 | fluid/FluidRenderer.java | 570 | 任意 Box 内流体渲染（still/flow sprite、光照、半满插值），罐/蒸馏器/泵/管流体全靠它 |
| | fluid/FluidSpriteType.java、fluid/SpriteFluidFrozen.java | — | 流体贴图枚举 / 冻结流体贴图 |
| 激光 | laser/LaserRenderer_BC8.java | 189 | 激光束渲染（贴图行进动画） |
| | laser/LaserBoxRenderer.java | 130 | 用激光拼盒框（采石场框架/标记盒） |
| | laser/LaserData_BC8.java（139）、CompiledLaserRow（173）、CompiledLaserType、LaserContext（170）、LaserCompiledBuffer（122）、LaserCompiledList（149）、ILaserRenderer | 共 ~900 | 激光类型编译/缓存体系 |
| 通用 | DetachedRenderer.java | 114 | "脱离方块"的世界内渲染接口（体积盒/激光路径全局层） |
| | HudRenderer.java、MarkerRenderer.java | — | 手持 HUD / 标记渲染基类 |
| | ItemRenderUtil.java | 281 | 物品世界内渲染工具 |
| 引擎基类 | tile/RenderEngine_BC8.java（59）、tile/RenderPartCube.java（107）、tile/RenderMachineWave.java（31） | — | 见 §1.3 |
| 字体 | font/ConfigurableFontRenderer（~100）、font/DelegateFontRenderer（242）、font/SpecialColourFontRenderer（162） | — | GUI 特殊颜色码字体（§3 依赖） |

**BER 总数：20 个 BlockEntityRenderer + 1 EntityRenderer + 9 个插件/流体/行为子渲染器。**

---

## 2. buildcraftlib 模型系统

### 2.1 总管线（1.20.1 实况，git 核对）

```
.jsonbc / BC格式.json (assets/<modid>/models/...)
   │  ModelHolderStatic/ModelHolderVariable 构造时（模块静态块）注册进 ModelHolderRegistry
   ▼
JsonVariableModel.deserialize()   ← 直接开 ResourceManager 的流，不走 vanilla ModelManager
   │  (parent 字段自动拼 ".jsonbc" 后缀递归展开；textures/#引用 解析)
   ▼
JsonModelPart/JsonVariableModelPart 树 + JsonModelRule（表达式规则）
   │  配 FunctionContext（sub_projects/expression 表达式引擎）里的 NodeVariableDouble/Object/Boolean
   ▼
MutableQuad[]（getCutoutQuads/getTranslucentQuads）
   ├─ BER 路径：AdvModelCache 或直接 get*Quads()，逐面乘光照后 render
   └─ 烘焙替换路径：ModelEvent.ModifyBakingResult 把 ModelItemSimple/ModelPipe/ModelPluggableItem
      等塞进 vanilla baked model 表（物品栏渲染 + 管道方块 chunk 模型）
贴图：ModelHolderRegistry.onTextureStitchPre 收集 → TextureStitchEvent 缝进方块图集 →
      TextureStitchEvent.Post 后才可 getSprite（1.20.1 特有的 spriteTasks 延迟链，Calen 补丁）
```

### 2.2 核心类（路径 `common/buildcraft/lib/client/model/`）

| 类 | 行数 | 职责（≤5 行） |
|---|---|---|
| ModelHolder.java | 37 | 抽象基类：持有模型资源位置，资源重载时自动刷新。 |
| ModelHolderVariable.java | 174 | 变量化模型持有者。构造时给 jsonbc 路径+FunctionContext；变量改动后 getCutoutQuads() 返回求值后的四边形。所有引擎/门/脉冲器/热交换模型用它。 |
| ModelHolderStatic.java | 243 | 不可变模型持有者（BC 格式 .json），标注 @deprecated（与 Variable 重复），但 robotics 的 robot_station 仍在用。 |
| ModelHolderRegistry.java | 81 | 全部 Holder 的注册表；onTextureStitchPre/onModelBake 分发；datagen 贴图注册（ExistingFileHelper）。 |
| AdvModelCache.java | 178 | 按 ModelVariableData 状态从 ModelHolderVariable 取当前 cutout 四边形并缓存（引擎 BER / 门 BER 用）。 |
| ModelCache.java + IModelCache.java + ModelCacheJoiner.java(72) + ModelCacheMultipleSame.java(60) | 41+18+72+60 | "无限变体"缓存：key→List<BakedQuad>，按时间/数量过期。管道模型的核心（连接方向×颜色×贴图组合爆炸）。 |
| MutableQuad.java / MutableVertex.java | 680 / 650 | 自研四边形/顶点：位置、UV、法线、染色、光照、overlay、toBakedBlock()/toBakedItem() 转 vanilla BakedQuad。**26.1.2 已有雏形 BcQuad/BcVertex**。 |
| ModelItemSimple.java | 211 | 用四边形列表+预设 Transform 实现的 BakedModel，物品栏里渲染引擎/管道/发电机等动态物品。 |
| ModelPluggableItem.java | 102 | 插头物品模型（静态+动态两段四边形，如机器人停靠站）。 |
| ModelNotifyOfRF.java | 72 | RF 相关物品模型包装。 |
| ModelUtil.java | 220 | createFace 等几何生成工具（管体/面片）。 |
| ResourceLoaderContext.java | 52 | jsonbc 读取上下文，记录资源依赖（reload 链）。 |
| plug/PlugBakerSimple.java | 55 | 把 ModelHolder 四边形转 IPluggableStaticBaker（插头静态部分烘进管道 chunk 模型）。 |

### 2.3 json/ 反序列化体系（路径 `lib/client/model/json/`）

| 类 | 行数 | 职责 |
|---|---|---|
| JsonModel.java | 98 | 静态 BC 格式模型解析（robot_station 等用）。 |
| JsonVariableModel.java | 355 | **jsonbc 入口**：deserialize() 直读资源流；textures/parent/values(变量声明)/cutout+translucent elements/rules；parent 自动拼 .jsonbc。 |
| JsonVariableModelPart.java（163）/ JsonModelPart.java（102） | | 元素节点：cuboid/led/textureExpand 等的容器与求值。 |
| JsonModelRule.java | 211 | 规则（when 表达式→then 参数），门/引擎朝向与阶段切换靠它。 |
| JsonQuad.java（71）/ JsonVertex.java（29） / JsonTexture.java（85） / JsonVariableFaceUV.java（103） | | 面/顶点/贴图引用/动态 UV。 |
| VariablePartCuboid.java（70）/ VariablePartCuboidBase.java（90）/ VariablePartLed.java（39）/ VariablePartTextureExpand.java（107）/ VariablePartContainer.java（34） | | 可变长方体（引擎活塞行程！）/LED 方块/贴图扩展/部件容器。 |

### 2.4 管道几何生成与缓存（路径 `transport/client/model/`）

| 类 | 行数 | 职责 |
|---|---|---|
| ModelPipe.java | 136 | BakedModel 单例：`getQuads(state, side, rand, ModelData, RenderType)`，从 `TilePipeHolder.getModelData()`（PROP_TILE）拿管道 tile，查缓存返回 cutout+translucent。由 BCTransportModels.onModelBake 替换进 `pipe_holder` 槽位。 |
| PipeModelCacheAll.java（137）/ PipeModelCacheBase.java（147）/ PipeModelCachePluggable.java | | 管体+插头合并缓存：key = PipeModelKey（定义×中心/边贴图×6 向连接×染料色）。 |
| PipeBaseModelGenStandard.java | 321 | **程序化生成管体几何**：6 种连接形态的 6 面四边形模板×PipeFaceTex 贴图；贴图来自 `PipeDefinition.textures[]`（如 `buildcrafttransport:pipes/items_cobblestone`）。 |
| PipeBaseModelGenConnected.java / IPipeBaseModelGen.java | | 连接贴图式生成交替实现（未默认启用）。 |
| ModelPipeItem.java | 297 | 所有管道物品的动态 item 模型（按 PipeDefinition 贴图生成带封盖的管形；含插头预览）。onModelBake 时替换所有 `pipe_*#inventory`。 |
| key/PipeModelKey.java + key/KeyPlugBlocker/PowerAdaptor | | 缓存 key。 |

### 2.5 门/插头模型（路径 `silicon/client/model/`）

| 类 | 行数 | 职责 |
|---|---|---|
| GateMeshDefinition.java | 32 | 按门变体选择 item 模型。 |
| ModelGateItem.java | 118 | 门物品模型（材质/逻辑/修饰三轴组合变体）。 |
| plug/ModelFacadeItem.java | 131 | 幻象板物品模型（按方块状态生成）。 |
| plug/ModelLensItem.java | 125 | 透镜物品模型（染色）。 |
| plug/PlugGateBaker.java | 43 | 门静态部分（gate.jsonbc 变体求值）→ 烘进管道 chunk 模型。 |
| plug/PlugBakerFacade.java | 317 | 幻象板静态烘焙：抓取目标方块 BakedModel 顶点重排成面板（最复杂的 baker）。 |
| plug/PlugBakerLens.java | | 透镜烘焙。 |
| key/KeyPlugGate/Gate/Lens/LightSensor/Pulsar/Timer（silicon）、key/KeyPlugRobotStation（robotics） | | 插头缓存 key（材质×方向×修饰）。 |

### 2.6 每模块模型入口类（jsonbc 声明 + BER 注册 + 烘焙替换都集中在这）

| 类 | 路径 | 声明的 jsonbc / 注册内容 |
|---|---|---|
| BCCoreModels.java | core/BCCoreModels.java | engine_redstone.jsonbc、engine_creative.jsonbc（木/创造引擎）+ 物品模型替换 + 3 BER。 |
| BCEnergyModels.java | energy/BCEnergyModels.java | engine_stone/iron/rf.jsonbc、mj_dynamo.jsonbc + 4 BER + 4 物品模型（progress/stage/direction 三变量求值）。 |
| BCFactoryModels.java | factory/BCFactoryModels.java | distiller.jsonbc、heat_exchange_static.jsonbc + 6 BER + ModelHeatExchange。 |
| BCSiliconModels.java | silicon/BCSiliconModels.java | plugs/gate{,_dynamic}.jsonbc、lens/filter/light_sensor/timer/pulsar_{static,dynamic}.jsonbc + baker/renderer 注册 + ModelGateItem。 |
| BCTransportModels.java | transport/BCTransportModels.java | plugs/blocker.jsonbc、power_adapter.jsonbc、pipes/stripes.jsonbc + ModelPipe/ModelPipeItem 替换 + RenderPipeHolder + 5 flow/behaviour 渲染器注册。 |
| BCRoboticsModels.java | robotics/BCRoboticsModels.java | plugs/robot_station_static.json（BC 格式）+ robot_station_dynamic.json + RenderRobot/RenderZonePlanner。 |
| BCBuilders.java | builders/BCBuilders.java | 无 jsonbc（盒/激光渲染直接实现）；注册 5 BER + frame RenderType。 |

### 2.7 辅助子系统

| 子系统 | 路径 | 说明 |
|---|---|---|
| sprite/（11 类） | lib/client/sprite/ | SpriteHolderRegistry（244 行：自定义 sprite 缝图集+重载刷新）、AtlasSpriteVariants（127：.mcmeta 动画/变体）、DynamicTextureBC、SpriteNineSliced、White（纯白 sprite）等。**所有激光/触发器图标/管内条纹动画贴图靠它**。 |
| reload/（6 类） | lib/client/reload/ | ReloadManager（171）等：资源重载回调注册表（IReloadable），F3+T 热更新模型/GUI。 |
| resource/（2 类） | lib/client/resource/ | DataMetadataSection/MetadataLoader：png 附加元数据加载。 |
| 表达式引擎 | sub_projects/expression + lib/expression + lib/misc/ExpressionCompat | jsonbc 规则求值底层（独立子项目，纯 Java 无 MC 依赖，可直接搬）。 |

### 2.8 全部 .jsonbc 文件（20 个，git ls-tree 核对）

```
buildcraft_resources/assets/
├── buildcraftlib/models/tile/engine_base.jsonbc                ← 引擎父模型（#trunk_*/#chamber/#back/#side）
├── buildcraftcore/models/tile/engine_redstone.jsonbc           ← 木引擎（#back/#side=wood）
├── buildcraftcore/models/tile/engine_creative.jsonbc
├── buildcraftenergy/models/tile/{engine_stone,engine_iron,engine_rf,mj_dynamo}.jsonbc
├── buildcraftfactory/models/tile/{distiller,heat_exchange_static}.jsonbc
├── buildcraftrobotics/models/robot.jsonbc                       ←（common 内无引用，疑似 API 子模块用）
├── buildcraftsilicon/models/plugs/{filter,gate,gate_dynamic,lens,light_sensor,
│                                   pulsar_dynamic,pulsar_static,timer}.jsonbc
└── buildcrafttransport/models/pipes/stripes.jsonbc、plugs/{blocker,power_adapter}.jsonbc
```
另有 BC 格式 .json：`buildcraftrobotics/models/plugs/robot_station_{static,dynamic}.json`。

---

## 3. GUI 清单

### 3.1 GUI 框架（lib/gui，~90 类 + guide 76 类）

| 体系 | 类 | 说明 |
|---|---|---|
| 基类 | gui/GuiBC8.java（308） | extends AbstractContainerScreen；BuildCraftGui 元素树 + ledger（侧边折叠页）+ 帮助元素；支持 json 定义。 |
| | gui/BuildCraftGui.java、Widget_Neptune、GuiElementSimple 等 | 元素树/容器/滚动窗/tooltip（elem/ 11 类）。 |
| | gui/GuiIcon.java（238）、GuiSpriteScaled、GuiStack | 贴图切片与物品绘制。 |
| 布局 | gui/pos/（11 类：GuiRectangle、IGuiArea、IGuiPosition…） | 声明式定位。 |
| 槽位 | gui/slot/（10 类：SlotPhantom、SlotOutput、SlotValidated…） | 幻影槽（配方锁定）等。 |
| 按钮 | gui/button/（10 类：GuiImageButton、StandardButtonTextureSets…） | 贴图按钮。 |
| ledger | gui/ledger/（LedgerEngine、LedgerHelp、LedgerOwnership、Ledger_Neptune） | 引擎能量侧页等。 |
| **门编程** | gui/statement/（7 类：GuiElementStatement{,Param,Source,Variant,Drag}、ParameterRenderer） | 触发器/动作/参数三段选择 UI——GuiGate 的核心。 |
| json GUI | gui/json/（19 类：BuildCraftJsonGui、JsonGuiInfo、ElementType{Button,Slot,Text,Sprite,Ledger,Statement*}…） | `assets/<modid>/gui/*.json` 定义布局。 |
| 配方书 | gui/recipe/（6 类：GuiRecipeBookPhantom、RecipeListPhantom…） | 幻影配方书。 |
| 字体 | lib/client/render/font/（3 类） | §1.9。 |
| 配置 GUI | core/client/ConfigGuiFactoryBC、lib/gui/config/（7 类） | Forge 配置界面。 |
| **游戏内指南** | lib/client/guide/（**76 类**：GuiGuide 752、GuideManager 472、XmlPageLoader 786、MarkdownPageLoader 179、loader/entry/node/parts/ref/world…） | 完整电子书系统；页面内容在 BuildCraftGuide 子模块。 |

### 3.2 各模块 GUI / Container / 菜单注册

**注册链**：`BC*MenuTypes.java`（7 个，ForgeRegistries.MENU_TYPES.register）+ 客户端 `MenuScreens.register(TYPE, GuiXxx::new)`。GUI 类大多在 `common/buildcraft/<mod>/gui/`（energy 在 `client/gui/`，因其直接用 GuiGraphics）。

| 模块 | Screen 类（行数） | Container 类 | MenuTypes |
|---|---|---|---|
| core | core/list/GuiList.java、core/list/ContainerList.java、ListTooltipHandler | （列表物品过滤器 GUI） | BCCoreMenuTypes |
| builders | gui/GuiArchitectTable(126)、GuiBuilder(78)、GuiElectronicLibrary(128)、GuiFiller(77)、GuiFillerPlanner(54)、GuiReplacer(120) | container/Container{ArchitectTable,Builder,ElectronicLibrary,Filler,FillerPlanner,Replacer} + IContainerFilling | BCBuildersMenuTypes |
| energy | client/gui/GuiEngineStone_BC8(93)、GuiEngineIron_BC8(65)、GuiEngineRF(143)、GuiDynamoMJ(150)、LedgerDynamoMJ | container/Container{EngineStone_BC8,EngineIron_BC8,EngineRF,DynamoMJ} | BCEnergyMenuTypes |
| factory | gui/GuiAutoCraftItems(415)、GuiChute(34)、GuiTank(60) | container/Container{AutoCraftItems,Chute,Tank} | BCFactoryMenuTypes |
| silicon | gui/GuiAdvancedCraftingTable(364)、GuiAssemblyTable(114)、GuiChargingTable(45)、**GuiGate(101)**、GuiIntegrationTable(65)、GuiProgrammingTable_Neptune(245)、LedgerTablePower | container/Container{AdvancedCraftingTable,AssemblyTable,ChargingTable,Gate,IntegrationTable,ProgrammingTable_Neptune} | BCSiliconMenuTypes |
| transport | gui/GuiDiamondPipe(71)、GuiDiamondWoodPipe(133)、GuiEmzuliPipe_BC8(157)、GuiFilteredBuffer(96) | container/Container{DiamondPipe,DiamondWoodPipe,EmzuliPipe_BC8,FilteredBuffer_BC8} | BCTransportMenuTypes |
| robotics | gui/GuiZonePlanner(**662**，含区地图交互)、GuiRequester(119) | container/Container{ZonePlanner,Requester} | BCRoboticsMenuTypes |
| lib | client/guide/GuiGuide(752) | container/ContainerGuide | BCLibMenuTypes + BCLibScreenConstructors |

### 3.3 GUI 资源

- JSON GUI 定义：`buildcraft_resources/assets/buildcraftbuilders/gui/filler{,_base,_planner}.json`、`buildcraftsilicon/gui/gate.json`、`buildcraftlib/gui/ledger/{controllable,engine_power}.json`。
- GUI 贴图（整图 blit）：`textures/gui/` 各模块共 30+ 张（steam_engine_gui.png、combustion_engine_gui.png、mj_dynamo_gui.png、rf_engine_gui.png、filler.png、quarry.png、tank.png、distiller.png…，见 §4 统计）。

---

## 4. 贴图使用方式

### 4.1 路径规律（全部 git show 核对）

| 资源类型 | 引用形式 | 实际文件 | 例子 |
|---|---|---|---|
| jsonbc 模型贴图 | `modid:block/xxx` | `assets/<modid>/textures/block/xxx.png` | engine_stone.jsonbc `#side -> buildcraftenergy:block/engine/stone/side` ⇒ `assets/buildcraftenergy/textures/block/engine/stone/side.png`；父模型 engine_base.jsonbc 的 `#trunk_blue -> buildcraftlib:block/engine/trunk_blue`、`#chamber -> buildcraftlib:block/engine/chamber_base`（共享引擎部件贴图全在 buildcraftlib） |
| 管道贴图 | `buildcrafttransport:pipes/<prefix><suffix>` | `textures/pipes/*.png`（**无 block/ 段**，208 张） | `BCTransportPipes.DefinitionBuilder.texPrefix`: `"buildcrafttransport:pipes/" + prefix`，如 items_cobblestone、fluids_wood_clear/_filled（木管双态）、power_*、rf_*、daizuli_黑…16 色 |
| 管线红石线 | `buildcrafttransport:wires/...` | `textures/wires/`（17 张） | PipeWireRenderer 四色 on/off |
| 门贴图 | `buildcraftsilicon:gates/gate_and` 等 | `textures/gates/`（17 张）；门材质直接引用 vanilla（`minecraft:block/iron_block` 等） | gate.jsonbc `#material_iron -> minecraft:block/iron_block` |
| 激光 | `buildcraftcore:textures/lasers/`（power_low…带 .mcmeta 动画、marker_*） | lasers/ 17 张 | BuildCraftLaserManager + RenderQuarry 的 frame/drill LaserType |
| 触发器/动作图标 | `modid:triggers/...` | `textures/triggers/`：core 61 + transport 78 + silicon 8 | 门编程 GUI 与手持列表 GUI |
| GUI | 硬编码 `modid:textures/gui/xxx.png` | textures/gui/ 各模块 | GuiEngineStone_BC8: `buildcraftenergy:textures/gui/steam_engine_gui.png` |
| 实体贴图 | `buildcraftrobotics:textures/entities/...` | robotics entities/ | RenderRobot overlay_side/bottom |
| 流体 | `buildcraftlib:block/fluid/{oil,fuel,heat_*}_{still,flow}.png`（.mcmeta 动画） | lib 20 张 | FluidRenderer + AtlasSpriteFluid |
| SpriteHolder（动画 sprite） | `SpriteHolderRegistry.getHolder("buildcraftbuilders:block/frame/default")` 等 | 任意图集路径 | 激光行、管内条纹动画、distiller power_0..7（JsonTexture 可带 uv 子区） |

### 4.2 贴图总量（git ls-tree 统计，`buildcraft_resources/assets/*/textures`，共 1039 png）

builders 178 / core 167 / transport 339 / robotics 107 / silicon 96 / factory 72 / lib 62 / energy 18（另 `buildcraft_resources_generated` 无贴图，静态模型 json 与 blockstate 都在 generated 里）。

### 4.3 静态模型 vs BER 动态渲染分界

**A. 纯静态 vanilla 模型就够（blockstate→`models/block/*.json`，无 BER 参与）**：
- builders：builder（multipart 含 slot_empty/slot_blueprint/slot_template 三态）、frame、architect、library、replacer、marker_construction 机身
- factory：chute、autoworkbench_item、flood_gate、water_gel、tank 玻璃罐（joined_below 两变体）、tube 机身
- core：marker_path/marker_volume（multipart 朝向旋转）、spring_oil/water、装饰方块（decorated_*）
- energy：流体方块（generated fluid_block_*）

**B. builtin/entity（chunk 模型为空，全靠 BER）**：
- transport **pipe_holder**（ModelPipe 经 ModelEvent 替换后走 ModelData 动态 quads + RenderPipeHolder BER 画动态层）
- factory **heat_exchange**（RenderHeatExchange 全画）
- 全部 5 种引擎（blockstate 指向 `block/engine_stone` 等静态 json，但这些 json 的 parent 是 `minecraft:builtin/entity` + 仅 particle 贴图——实际形体全由 RenderEngine_BC8 从 jsonbc 画）
- mj_dynamo 同上

**C. 静态机身 + BER 叠加动态层（最常见模式）**：
- factory：distiller（静态机身 + BER 画 3 内罐流体）、tank（静态罐 + BER 流体）、mining_well（静态 + BER 井管钻头）、pump（静态 + BER 吸管流体）、tube（静态 + RenderTube）
- builders：quarry（静态机身 + BER 激光框/钻头）、builder/filler（静态 + RenderSnapshotBuilder 蓝图盒）、architect（静态 + RenderArchitectTables）、marker_construction（静态 + BER 连线）
- silicon：laser（静态发射台 + RenderLaser 光束）、programming_table（静态 + BER 桌面物品）、门（静态部分烘进管道模型 + PlugGateRenderer 动态 LED）
- robotics：zone_planner（静态 + BER 地图）

**26.1.2 映射要点**：B/C 类都能沿用"A=blockstate 静态模型 + BER 动态层"策略（26.1.2 已验证 chest 模式，见 KinesisPipeBlockRenderer 注释）；A 类只需资源搬迁；难点集中在 B 类管道——1.20.1 走 ModelData+chunk 烘焙，26.1.2 若沿用现有 BER 路线则失去 chunk 批量优势（可先 BER 后优化）。

---

## 5. 建议移植顺序（收益/工作量排序，含依赖）

> 代号：P0 基础 → P1 世界视觉 → P2 GUI → P3 长尾。每项标依赖。

**P0-A. 贴图资源搬迁**（工作量：小，收益：极大）
- 把 `buildcraft_resources/assets/*/textures`（8 命名空间 1039 png + .mcmeta）整体拷入 neo resources，替换现有 vanilla 占位贴图引用（engine_stone.json 等改指 `buildcraftenergy:block/engine/stone/side`）。
- 依赖：无。做完后所有现有占位模型立刻"变 BuildCraft"，且为后面全部任务铺路。

**P0-B. 几何/图集基元补全**（工作量：中）
- 扩展现有 BcQuad/BcVertex/BcBoxes 到 MutableQuad 能力面（染色/光照/shade/UV）；落地 SpriteHolderRegistry 等价物（26.1.2 用 SpriteId/SpriteGetter + 图集注册，StoneEngineBlockRenderer 已示范）。ModelUtil.createFace 几何生成。
- 依赖：P0-A。为 P1 所有项共用。

**P1-1. 引擎家族完整视觉**（工作量：中，收益：大）
- RenderEngine_BC8 基类 + 5 子类（wood/stone/iron/RF/creative）+ engine_base.jsonbc + 3 引擎 jsonbc + RenderDynamoMJ；先把 JsonVariableModel 最小实现（cutoutElements+textures+parent，不含规则）搬过来。
- 依赖：P0-B。已有 M2.7b 石引擎可对照重构。

**P1-2. 激光系统**（工作量：中，收益：大——采石场/标记/激光台全靠它）
- lib/client/render/laser/ 9 类（~900 行）+ BuildCraftLaserManager + lasers/ 贴图（.mcmeta 动画需 26.1.2 动画 sprite 等价）。
- 依赖：P0-B。无 jsonbc 依赖，可并行 P1-1。

**P1-3. 采石场/标记完整 BER**（工作量：中）
- RenderQuarry 完整版（激光框+钻头+能量束，M2.12 已有盒框骨架）+ RenderMarkerVolume/RenderVolumeBoxes/RenderTickListener（全局层 → 26.1.2 RenderLevelStage 等价事件）+ RenderMarkerConstruction。
- 依赖：P1-2（LaserBoxRenderer）。

**P1-4. 管道系统**（工作量：最大，收益：最大——BuildCraft 的门面）
- ModelPipe + PipeModelCache{Base,All,Pluggable} + PipeBaseModelGenStandard + RenderPipeHolder + PipeWireRenderer + 4 个 FlowRenderer + stripes + ModelPipeItem + 208 张管道贴图。
- 26.1.2 策略建议：先全 BER（KinesisPipeBlockRenderer 路线推广到通用管道+插头），ModelData/chunk 烘焙作为后续优化。依赖：P0-A/B；插头静态部分（PlugBakerSimple）依赖 P1-4 自身。
- 注：完整 ItemPipe/FluidPipe 逻辑是服务端任务，这里只列视觉层。

**P1-5. 流体渲染 FluidRenderer**（工作量：中）
- lib 570 行 + SpriteFluidFrozen + FluidSpriteType；随后 RenderTank/RenderDistiller/RenderPump/RenderHeatExchange（C 类叠加模式，静态机身已有）。
- 依赖：P0-B；RenderHeatExchange 需 heat_exchange_static.jsonbc（jsonbc 解析器，与 P1-1 共享）。

**P2-1. lib GUI 框架**（工作量：大，收益：大——解锁一切交互）
- GuiBC8/BuildCraftGui/pos/slot/button/ledger/elem（~80 类）+ font 3 类 + GuiIcon + json gui 加载器（19 类）。
- 依赖：P0-A（gui 贴图）。可早启动（纯客户端）。

**P2-2. 第一批 GUI**（每项独立小任务）
- 顺序建议：引擎 4 GUI（贴图齐、逻辑简单）→ GuiTank/GuiChute → GuiFiller（json gui）→ 钻石管/木钻石管/过滤缓冲（幻影槽体系）→ GuiGate + statement 元素体系（依赖 triggers 贴图 147 张 + ParameterRenderer）→ 装配/充电/集成/编程台 → GuiAutoCraftItems(415)/GuiAdvancedCraftingTable(364) → GuiZonePlanner(662)。
- 依赖：P2-1；门 GUI 另依赖 P1-4（管道存在才有门）。

**P3. 长尾**（按需）
- 门/插头全量视觉（PlugBakerFacade 317 行幻象板、lens/filter/timer/light_sensor/pulsar jsonbc、ModelGateItem/GateMeshDefinition）
- RenderRobot 完整版（577 行分层模型+盔甲）+ ZonePlannerMapRenderer（离屏地图）
- 指南系统（76 类 + BuildCraftGuide 子模块内容，独立可最后）
- ReloadManager 热重载、ChristmasHandler 节日贴图、特殊字体、HudSingleSchematic、机器人粒子
- ModelPipeItem 之外的动态物品模型（门/透镜/幻象板/插头）

### 依赖图（简化）

```
P0-A 贴图 ──> P0-B 基元 ──> P1-1 引擎(jsonbc最小集) ─┐
                    │────> P1-2 激光 ──> P1-3 采石场/标记 │
                    │────> P1-4 管道(含插头) <────────────┘(门依赖管道)
                    └────> P1-5 流体渲染器
P0-A ──> P2-1 GUI框架 ──> P2-2 各GUI(门依赖P1-4)
全部 ──> P3 长尾
```

---

## 6. 附：风险与注意点

1. **26.1.2 双阶段范式**：所有 BER 必须 createRenderState/extractRenderState/submit 三件套（M2.7a 文档 + StoneEngineBlockRenderer 样板），1.20.1 的 MultiBufferSource 直画代码不能照搬。
2. **jsonbc 表达式引擎**（sub_projects/expression）是纯 Java 子项目，可近乎原样搬迁；但 JsonVariableModel 与 MC 资源管理器/图集耦合部分需重写（SpriteGetter、RegisterAdditional）。
3. **管道 chunk 烘焙路线**在 26.1.2 的可行性未验证（ModelData/RenderType 分层 getQuads 的 NeoForge 等价物），建议先 BER 保正确性。
4. **spriteTasks 延迟链**（1.20.1 Calen 补丁：TextureStitchEvent.Post 后才能 getSprite）在 26.1.2 的对应时序需重新查证，避免 NPE。
5. builders 模块没有独立 Models 类、energy 的 GUI 在 client/gui 而其他模块在 gui/——移植时保持 26.1.2 现有 `Bc*` 命名惯例即可，不必复刻 1.20.1 的目录不一致。
