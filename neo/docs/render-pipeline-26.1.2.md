# MC 26.1.2 / NeoForge 26.1.2.109 方块实体渲染链路查证

> 任务 M2.7a 产出（2026-09-15）。所有类名/签名均从本地反编译/源码 jar 逐行核对，供 M2.7b（19 个 BlockEntityRenderer 迁移）及后续渲染任务直接引用。
>
> 源码位置：
> - MC client 反编译源：`~/.gradle/caches/neoformruntime/intermediate_results/decompile_22b1fa6d79c4d3e6287568f90bcdc1ca404f7202_output.jar`（下称 client.jar）
> - NeoForge 补丁源码：`~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.109/80f71ba77f8d944c5f819b6d8072c2cd7e40babe/neoforge-26.1.2.109-sources.jar`（下称 neoforge-src.jar）
> - 文中「路径」均指 jar 内条目路径。

## 1. 总链路（每帧两阶段：extract → submit）

```
GameRenderer.renderLevel (client.jar: net/minecraft/client/renderer/GameRenderer.java)
  └─ L417: levelRenderer.extractLevel(deltaTracker, mainCamera, worldPartialTicks)   ← 阶段一：抽取
       └─ LevelRenderer.extractLevel (net/minecraft/client/renderer/LevelRenderer.java, L569)
            ├─ extractVisibleBlockEntities(camera, deltaPartialTick, levelRenderState)  (L860)
            │    对每个可见 section 的 renderableBlockEntities：
            │      breakProgress = ModelFeatureRenderer.CrumblingOverlay(进度, pose)   ← 破坏动画覆盖
            │      state = blockEntityRenderDispatcher.tryExtractRenderState(be, partialTick, breakProgress)
            │      levelRenderState.blockEntityRenderStates.add(state)
            │    再扫 level.getGloballyRenderedBlockEntities()（全局 BE，无 section 归属）
            └─ 其余抽取：天气/天空/粒子等（各自 extractRenderState）
  └─ levelRenderer.addMainPass(...) (L521→L617)
       └─ L687: submitBlockEntities(poseStack, levelRenderState, submitNodeStorage)     ← 阶段二：提交
            对每个 BlockEntityRenderState：
              poseStack.translate(pos - cameraPos)
              blockEntityRenderDispatcher.submit(state, poseStack, submitNodeStorage, levelRenderState.cameraRenderState)
```

要点：**渲染数据与渲染动作彻底分离**。`extract` 在逻辑帧尾构建不可变 RenderState；`submit` 读 state、向
`SubmitNodeCollector` 发射绘制命令，之后由引擎按 RenderType/管线排序执行。1.20.1 的
`render(be, partialTick, poseStack, MultiBufferSource, light, overlay)` 单体式签名已不存在。

## 2. 核心接口与类（签名原文）

### 2.1 BlockEntityRenderer
`client.jar: net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java` — 双泛型：

```java
@OnlyIn(Dist.CLIENT)
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> {
    S createRenderState();                                   // 每渲染器一次性创建 state 实例
    default void extractRenderState(T blockEntity, S state, float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }
    void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera);                       // 原 render() 的位置
    default boolean shouldRenderOffScreen() { return false; }
    default int getViewDistance() { return 64; }
    default boolean shouldRender(T blockEntity, Vec3 cameraPosition);   // 默认按视距裁剪
}
```

### 2.2 BlockEntityRenderState（基类 state）
`client.jar: net/minecraft/client/renderer/blockentity/state/BlockEntityRenderState.java`

```java
public class BlockEntityRenderState {
    public BlockPos blockPos;
    private BlockState blockState;              // getBlockState() 访问器
    public BlockEntityType<?> blockEntityType;
    public int lightCoords;                     // 打包光照 (block | sky<<16)
    public ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress;

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress);
    // extractBase: blockPos/blockState/blockEntityType 逐项拷贝；
    // lightCoords = LevelRenderer.getLightCoords(level, pos)，无 level 时 15728880（全亮）
    public void fillCrashReportCategory(CrashReportCategory category);
}
```
自定义渲染器：`S` 继承此类并补自己的字段，`extractRenderState` 覆写时**必须先 `super.extractRenderState(...)`**（或自己调 `extractBase`）。全部 vanilla state 子类在同级 `state/` 目录（ChestRenderState、SignRenderState 等 30+ 个），可作模板。

### 2.3 BlockEntityRenderDispatcher（调度器）
`client.jar: net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher.java`

```java
public <E extends BlockEntity, S extends BlockEntityRenderState> @Nullable
    BlockEntityRenderer<E, S> getRenderer(E blockEntity);          // 按 BlockEntityType 查
public <E extends BlockEntity, S extends BlockEntityRenderState> @Nullable S
    tryExtractRenderState(E blockEntity, float partialTicks,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress);
    // 内部：getRenderer → hasLevel/isValid 校验 → shouldRender 裁剪 →
    //       renderer.createRenderState() → renderer.extractRenderState(...)
public <S extends BlockEntityRenderState> void submit(S state, PoseStack poseStack,
    SubmitNodeCollector submitNodeCollector, CameraRenderState camera);   // try/catch 填崩溃报告
public void onResourceManagerReload(ResourceManager);   // 重建 renderers 表（资源重载时）
```

### 2.4 注册入口（NeoForge）
`neoforge-src.jar: net/neoforged/neoforge/client/event/EntityRenderersEvent.java`
（vanilla 的 `BlockEntityRenderers.register` 是包私有，模组唯一合法入口是这个事件，mod 事件总线，仅 client）：

```java
public static class RegisterRenderers extends EntityRenderersEvent {
    public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
        BlockEntityType<? extends T> blockEntityType,
        BlockEntityRendererProvider<T, S> blockEntityRendererProvider);
}
// 事件时机：资源管理器就绪后构建 renderer 表（对应 dispatcher.onResourceManagerReload）
```
`BlockEntityRendererProvider`（`client.jar: .../blockentity/BlockEntityRendererProvider.java`）是函数式接口：

```java
BlockEntityRenderer<T, S> create(BlockEntityRendererProvider.Context context);
public record Context(
    BlockEntityRenderDispatcher blockEntityRenderDispatcher,
    BlockModelResolver blockModelResolver,
    ItemModelResolver itemModelResolver,
    EntityRenderDispatcher entityRenderer,
    EntityModelSet entityModelSet,
    Font font,
    SpriteGetter sprites,
    PlayerSkinRenderCache playerSkinRenderCache) {
    public ModelPart bakeLayer(ModelLayerLocation id);   // 供模型烘焙
}
```

## 3. submit 阶段发射接口

### 3.1 SubmitNodeCollector / OrderedSubmitNodeCollector
`client.jar: net/minecraft/client/renderer/SubmitNodeCollector.java`、`OrderedSubmitNodeCollector.java`。
`submit(state, poseStack, collector, camera)` 拿到的 collector 常用发射方法：

```java
// 自定义几何体——BcQuad 的直接出口（含 PoseStack.Pose + 裸 VertexConsumer）
void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
    SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer);
public interface CustomGeometryRenderer { void render(PoseStack.Pose pose, VertexConsumer buffer); }

// 其它：模型/部件/方块模型/物品/文字/阴影/火焰/牵绳等
<S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
    int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite,
    int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay);
void submitModelPart(ModelPart, PoseStack, RenderType, int light, int overlay, @Nullable TextureAtlasSprite, ...);
void submitBlockModel(PoseStack, RenderType, List<BlockStateModelPart> parts, int[] tintLayers,
    int lightCoords, int overlayCoords, int outlineColor);
void submitItem(PoseStack, ItemDisplayContext, int light, int overlay, int outlineColor,
    int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType);
void submitText(PoseStack, float x, float y, FormattedCharSequence, boolean dropShadow,
    Font.DisplayMode, int lightCoords, int color, int backgroundColor, int outlineColor);
SubmitNodeCollector order(int order);   // 返回带排序的子 collector（半透明层控序）
```
生产实现：`net/minecraft/client/renderer/SubmitNodeStorage.java`（LevelRenderer 每帧传入的即它）。

### 3.2 VertexConsumer（发射原语，已核对）
`client.jar: com/mojang/blaze3d/vertex/VertexConsumer.java`

```java
VertexConsumer addVertex(float x, float y, float z);
VertexConsumer setColor(int color);          // ARGB
VertexConsumer setUv(float u, float v);
VertexConsumer setUv2(int u, int v);  +  default setLight(int packedLight);   // block|sky<<16
VertexConsumer setNormal(float x, float y, float z);
default void addVertex(float x, float y, float z, int color, float u, float v,
    int overlayCoords, int lightCoords, float nx, float ny, float nz);   // 一步到位
default void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance);
```

## 4. 相关类型速查

| 类型 | 26.1.2 全限定名（jar 内路径同） | 备注 |
|---|---|---|
| RenderType | `net.minecraft.client.renderer.rendertype.RenderType` | **包已从 `...renderer.RenderType` 迁走**，1.20.1 import 全要改 |
| BakedQuad | `net.minecraft.client.resources.model.geometry.BakedQuad` | record：4×`Vector3fc` position + 4×packed long UV + `Direction` + `MaterialInfo(sprite, layer, itemRenderType, tintIndex, shade, lightEmission)` |
| TextureAtlasSprite | `net.minecraft.client.renderer.texture.TextureAtlasSprite` | `getU(float)/getV(float)`、`getU0()...` |
| ARGB 工具 | `net.minecraft.util.ARGB` | `color(alpha,r,g,b)`、`multiply(int,int)`、`alpha/red/green/blue(int)` |
| PoseStack.Pose | `com.mojang.blaze3d.vertex.PoseStack$Pose` | `pose()`→`Matrix4f`、`transformNormal(Vector3fc, Vector3f)` |
| CameraRenderState | `net.minecraft.client.renderer.state.level.CameraRenderState` | `pos`/`orientation`/`cullFrustum`/`fogData` 等，submit 时作上下文 |
| LevelRenderState | `net.minecraft.client.renderer.state.level.LevelRenderState` | `blockEntityRenderStates` 列表在 extract 期填充、submit 期消费、`reset()` 清空 |
| 破坏覆盖 | `net.minecraft.client.renderer.feature.ModelFeatureRenderer$CrumblingOverlay` | record(progress, pose)，extract 期传入 |
| Feature 渲染器 | `net.minecraft.client.renderer.feature.FeatureRenderDispatcher` 等 | 名称牌/阴影/火焰/拴绳等已下沉为独立 feature，渲染器不再自绘 |
| RenderPipelines | `net.minecraft.client.renderer.RenderPipelines` | 26.1 渲染管线（GpuShaderDevice 时代）注册走 NeoForge `RegisterRenderPipelinesEvent` |

NeoForge 侧相关事件（`neoforge-src.jar: net/neoforged/neoforge/client/event/`）：
`ExtractLevelRenderStateEvent`（往 LevelRenderState 塞自定义 state）、`RenderLevelStageEvent`、
`RegisterRenderPipelinesEvent`、`RegisterBlockModelsEvent`、`RegisterFluidModelsEvent`、`EntityRenderersEvent.*`。

## 5. 1.20.1 → 26.1.2 BE 渲染迁移对照（给 M2.7b）

| 1.20.1 | 26.1.2 |
|---|---|
| `BlockEntityRenderer<T>` 单泛型 | `BlockEntityRenderer<T, S extends BlockEntityRenderState>` 双泛型 |
| `render(T be, float pt, PoseStack, MultiBufferSource, int light, int overlay)` | `createRenderState()` + `extractRenderState(...)` + `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` |
| 直接用 `MultiBufferSource.getBuffer(RenderType)` 即时画 | submit 阶段向 collector 发射命令（含 `submitCustomGeometry`），引擎统一执行 |
| light/overlay 参数传入 | `state.lightCoords`（extractBase 已填）；overlay 语义并入 feature/参数 |
| `RenderType` 在 `net.minecraft.client.renderer` | 移至 `net.minecraft.client.renderer.rendertype` |
| 事件：`EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer(BlockEntityType, provider)` | 同名同位，provider 变双泛型 |

## 6. 本仓库基建（M2.7a 已落地）

- `neo/src/client/java/buildcraft/lib/client/render/BcQuad.java` / `BcVertex.java`：不可变四边形 record +
  颜色(`withColor`/`multiplyColor`，ARGB)/UV(`mapUv`)/光照(`withLight`)/位姿(`transform`/`emit`)助手，legacy
  `MutableQuad/MutableVertex` 的最小对应移植（取舍说明见包 javadoc）。
- 发射方式：M2.7b 渲染器在 `submit(...)` 里经
  `submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> quad.emit(pose, buffer))`
  出四边形；state 字段直接持有 `List<BcQuad>`（extract 时构建）。
- 烟烟类：`BcQuadSmoke.check()`（client 入口 `BuildCraftEnergyClient` 启动时 log PASS/FAIL）。
- client 源集边界：`neo/build.gradle` sourceSets/runs/mods 配置 + `neo/src/client/java`；main→client 引用为 0（grep 取证）。

## 7. M2.7b 实证补充（StoneEngine / KinesisPipe 两个 BER 落地后新增结论）

### 7.1 BE 客户端数据同步（update tag API，26.1.2 原生路径）
- 服务端 BE 覆写两个方法（vanilla `BeaconBlockEntity` 同款）：
  `getUpdateTag(HolderLookup.Provider)` → `saveCustomOnly(registries)`；
  `getUpdatePacket()` → `ClientboundBlockEntityDataPacket.create(this)`。
- 客户端收包后走 `loadWithComponents` → `loadAdditional(ValueInput)`，键与 `saveAdditional` 一致，无需单独的
  网络 DTO。
- 服务端推送时机：改完 BE 后 `level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)`（vanilla
  `ConduitBlockEntity` 同款；`Block.UPDATE_CLIENTS == 2`）。周期性数据再配一个节流间隔即可。
- `/data merge block` 对 BE 等效于 loadWithComponents + setChanged + sendBlockUpdated(…,3)，因此内置数据包
  函数可直接点燃引擎做视觉取证。

### 7.2 submit 阶段的位姿语义
- `submit(state, poseStack, …)` 收到的 poseStack **已平移到方块原点**（LevelRenderer 先
  `translate(pos − cameraPos)` 再分发），custom geometry 顶点直接写方块内 0..1 坐标即可，勿再按 camera 偏移。
- 朝向旋转与 blockstate y-variant 表一致：几何按朝北创作后
  `poseStack.rotateAround(Axis.YP.rotationDegrees(facing.toYRot() − 180), 0.5, 0, 0.5)`
  （toYRot：south=0/west=90/north=180/east=270）。

### 7.3 实体 RenderType 的顶点格式含 overlay（UV1）——必踩坑
- `Sheets.cutoutBlockSheet()` 等实体侧 RenderType 的顶点格式带 overlay 元素；逐顶点漏调
  `setOverlay(OverlayTexture.NO_OVERLAY)` 会在绘制期抛
  `IllegalStateException: Missing elements in vertex: UV1`（编译期无感，世界渲染一帧即崩）。
- 安全发射顺序（`BcQuad.emit` 已固化）：`addVertex → setColor → setUv → setOverlay(NO_OVERLAY) → setLight → setNormal`。

### 7.4 图集 sprite 与 RenderType 取用
- `BlockEntityRendererProvider.Context#sprites()` 拿 `SpriteGetter`；`get(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
  Identifier.withDefaultNamespace("block/iron_block")))` 取方块图集 sprite（注意 `SpriteId` 与 1.20.1 的
  Material 不同名）。
- `TextureAtlasSprite.getU(offset)/getV(offset)` 入参是 **0..1 归一化**（源码 `u0 + (u1−u0)·offset`），
  BcQuad 的 0..1 UV 直接喂即可。
- RenderType 用 `net.minecraft.client.renderer.rendertype.RenderType`（已迁包）+ `Sheets.cutoutBlockSheet()`。

### 7.5 BER 自绘方块的“箱子式”空模型模式
- 方块本体交给 BER 画时，block model 只留 `{"textures":{"particle":…}}`（无 elements，vanilla chest 同款），
  烘焙零面、无 Missing-model/告警；物品形态另配 `*_inventory` 模型并在
  `assets/<ns>/items/<item>.json` 指向它。
- `BlockEntityRenderState` 在 `net.minecraft.client.renderer.blockentity.state` **子包**（不是
  `...blockentity` 本包）。
