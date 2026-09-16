# MC 26.1.2 / NeoForge 26.1.2.109 世界生成查证（M2.8）

> 任务 M2.8 产出（2026-09-15）。所有类名/签名均从本地反编译/源码 jar 逐行核对，供油田世界生成移植与后续
> worldgen 任务直接引用。legacy（1.20.1 Forge）侧事实见文末第 6 节审计结论。
>
> 源码位置：
> - MC 反编译源：`~/.gradle/caches/neoformruntime/intermediate_results/decompile_22b1fa6d79c4d3e6287568f90bcdc1ca404f7202_output.jar`（下称 mc.jar）
> - NeoForge 补丁源码：`~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.109/80f71ba77f8d944c5f819b6d8072c2cd7e40babe/neoforge-26.1.2.109-sources.jar`（下称 neoforge-src.jar）
> - vanilla 数据包 JSON 直接在 mc.jar 的 `data/minecraft/` 下。

## 1. 结论速览：油田生成在 26.1.2 怎么落地

| 组件 | 注册方式（26.1.2 查证结果） | 说明 |
|---|---|---|
| `StructureType`（oil_spout） | 代码注册：`DeferredRegister.create(BuiltInRegistries.STRUCTURE_TYPE, modid)` | 静态注册表，值是 `() -> MapCodec`（lambda supplier） |
| `StructurePieceType`（oil_spout） | 代码注册：`DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE, modid)` | 实现 `StructurePieceType.ContextlessType`（`load(CompoundTag)`） |
| `Structure` 实例（`buildcraftenergy:oil_spout`） | **只能 JSON**：`data/buildcraftenergy/worldgen/structure/oil_spout.json` | 26.1.2 无 `RegisterDataPackValuesEvent`（grep neoforge-src.jar 为 0），datapack 注册表条目不可代码注册 |
| `StructureSet`（oil_spout） | **只能 JSON**：`data/buildcraftenergy/worldgen/structure_set/oil_spout.json` | 同上；格式与 1.20.1 兼容 |
| biome 标签 `#buildcraftenergy:oil_gen` | **只能 JSON**：`data/buildcraftenergy/tags/worldgen/biome/oil_gen.json` | 值 `["#minecraft:is_overworld"]`；`#minecraft:is_overworld` 标签在 26.1.2 仍存在 |

与 legacy 的对齐：legacy 1.20.1 本来就是「STRUCTURE_TYPE/PIECE_TYPE 代码注册 + structure/structure_set 走
datagen 产出的 JSON」，26.1.2 下同样分工成立（JSON 改为静态资源直接随 jar 发布，内容逐字段与 legacy
`buildcraft_resources_generated` 相同）。**没有也不需要 Forge 的 biome_modifier**——legacy 就没有，结构进
biome 由 structure JSON 的 `biomes` 字段控制。

## 2. 静态注册表（代码注册，DeferredRegister 可用）

### 2.1 StructureType
mc.jar `net/minecraft/world/level/levelgen/structure/StructureType.java`：

```java
public interface StructureType<S extends Structure> {
    MapCodec<S> codec();
    private static <S extends Structure> StructureType<S> register(String id, MapCodec<S> codec) {
        return Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, () -> codec);
    }
}
```
注意与 1.20.1 的差异：`codec()` 返回 **MapCodec**（不再是 `Codec`）；注册表值是 supplier lambda。
`BuiltInRegistries.STRUCTURE_TYPE`（mc.jar `net/minecraft/core/registries/BuiltInRegistries.java` L253）是普通
`registerSimple` 注册表 → `DeferredRegister.create(BuiltInRegistries.STRUCTURE_TYPE, "buildcraftenergy")`
（neoforge-src.jar `net/neoforged/neoforge/registries/DeferredRegister.java` L100/L116 两个 create 重载，
`create(Registry<T>, String)` 直接可用）。

`Structure.DIRECT_CODEC`（Structure.java L56）按 `Structure::type()` dispatch 到 `StructureType::codec`，
所以自定义 Structure 类的 `CODEC = Structure.simpleCodec(OilStructureFeature::new)` 产物直接满足接口：

```java
// Structure.java L58-60
public static <S extends Structure> MapCodec<S> simpleCodec(Function<Structure.StructureSettings, S> constructor)
```

`Structure` 构造器与设置（Structure.java L62、L271-）：

```java
protected Structure(Structure.StructureSettings settings)
public record StructureSettings(
    HolderSet<Biome> biomes,
    Map<MobCategory, StructureSpawnOverride> spawnOverrides,
    GenerationStep.Decoration step,
    TerrainAdjustment terrainAdaptation)   // JSON 字段名 biomes/spawn_overrides/step/terrain_adaptation
```
`GenerationStep.Decoration.FLUID_SPRINGS("fluid_springs")` 存在（GenerationStep.java L16）。

### 2.2 StructurePieceType
mc.jar `net/minecraft/world/level/levelgen/structure/pieces/StructurePieceType.java`：

```java
public interface StructurePieceType {
    StructurePiece load(StructurePieceSerializationContext context, CompoundTag tag);
    public interface ContextlessType extends StructurePieceType {
        StructurePiece load(CompoundTag tag);   // default 桥接 load(context, tag)
    }
}
```
注册表：`BuiltInRegistries.STRUCTURE_PIECE`（BuiltInRegistries.java L250）。legacy 的
`StructurePieceType.setPieceId(OilStructure::deserialize, id)` 语义 = 把 `deserialize(CompoundTag)` 注册进
STRUCTURE_PIECE → 26.1.2 写法：注册一个 lambda 转 `ContextlessType`（DeferredRegister 值
`() -> (StructurePieceType.ContextlessType) OilStructure::deserialize`）。

`StructurePiece` 基类签名与 1.20.1 完全一致（StructurePiece.java L45/L77-87）：

```java
protected StructurePiece(StructurePieceType type, int genDepth, BoundingBox boundingBox)
public abstract void postProcess(WorldGenLevel level, StructureManager structureManager,
    ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos,
    BlockPos referencePos);
protected abstract void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag);
```
`createTag`（L84）用 `BuiltInRegistries.STRUCTURE_PIECE.getKey(this.getType())` 写 `id` 字段——piece 的 NBT
`id` 必须是注册名，piece 反序列化按此查表（结构存档循环依赖 piece type 注册，正常）。

## 3. Structure 生成期 API（findGenerationPoint 相关，逐条核对）

mc.jar `net/minecraft/world/level/levelgen/structure/Structure.java`：

```java
protected abstract Optional<Structure.GenerationStub> findGenerationPoint(GenerationContext context);
public Optional<Structure.GenerationStub> findValidGenerationPoint(GenerationContext context)
    // = findGenerationPoint().filter(stub -> context.validBiome.test(biome at stub position))   (L208/L208-211)
protected static Optional<GenerationStub> onTopOfChunkCenter(GenerationContext context,
    Heightmap.Types heightmap, Consumer<StructurePiecesBuilder> generator)                     // (L121-130)
public record GenerationContext(RegistryAccess registryAccess, ChunkGenerator chunkGenerator,
    BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager,
    WorldgenRandom random, long seed, ChunkPos chunkPos, LevelHeightAccessor heightAccessor,
    Predicate<Holder<Biome>> validBiome)                                                       // (L213-224)
public record GenerationStub(BlockPos position, Either<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder>)
```
- biome 采样：`BiomeSource.getNoiseBiome(quartX, quartY, quartZ, Climate.Sampler)`（BiomeResolver.java L6；
  `Climate` 在 `net.minecraft.world.level.biome` 包）；`RandomState.sampler()`（RandomState.java L138）。
  与 1.20.1 同形。
- `WorldgenRandom extends LegacyRandomSource`；`setLargeFeatureSeed(long seed, int chunkX, int chunkZ)`
  存在（WorldgenRandom.java L58）。`new WorldgenRandom(new LegacyRandomSource(seed))` 可用。
- `QuartPos.fromBlock(int)` 存在（QuartPos.java L12）。
- `LevelHeightAccessor.getMinY()/getMaxY()`（LevelHeightAccessor.java L9/L11；**无** 1.20.1 的
  getMinBuildHeight/getMaxBuildHeight，移植 generatePieces 时换名）。
- `Heightmap.Types.WORLD_SURFACE_WG` 存在（Heightmap.java L145，Usage.WORLDGEN）。
- `ChunkGenerator.getBiomeSource()`（ChunkGenerator.java L438）、
  `getFirstOccupiedHeight(x, z, type, levelHeightAccessor, randomState)`（onTopOfChunkCenter 内部用法）。
- `Holder.is(TagKey)`（Holder.java L29；替代 1.20.1 的 `containsTag`）；`Holder.unwrapKey()`（L40）；
  `ResourceKey.identifier()` 返回 `Identifier`（ResourceKey.java L55——**ResourceLocation 在 26.1.2 更名
  Identifier**，与 M2.4/M2.5 查证一致）。

## 4. 结构 JSON 格式（26.1.2 vanilla 自证）

mc.jar `data/minecraft/worldgen/structure/desert_pyramid.json`：

```json
{ "type": "minecraft:desert_pyramid", "biomes": "#minecraft:has_structure/desert_pyramid",
  "spawn_overrides": {}, "step": "surface_structures" }
```
mc.jar `data/minecraft/worldgen/structure_set/villages.json`：`placement{type,salt,separation,spacing[,spread_type]}`
+ `structures[{structure,weight}]`——字段与 1.20.1 完全一致；
`RandomSpreadStructurePlacement.CODEC` 的 `spread_type` 是 `optionalFieldOf(..., RandomSpreadType.LINEAR)`
（RandomSpreadStructurePlacement.java L21），spacing/separation 校验 `Codec.intRange(0, 4096)`（L19-20）。
legacy oil_spout structure_set（spacing=1, separation=0, salt=800672540, 无 spread_type）可原样使用。

biome 标签路径仍是 `data/<ns>/tags/worldgen/biome/<tag>.json`（mc.jar `data/minecraft/tags/worldgen/biome/
is_overworld.json` 自证，标签值为 overworld 全量 biome 清单）。

**datapack 注册表（STRUCTURE/STRUCTURE_SET/BIOME 等）能否代码注册：不能。** neoforge-src.jar 中
`RegisterDataPackValuesEvent` 不存在（grep 0 命中）；只有 `DataPackRegistryEvent.NewRegistry`
（`net/neoforged/neoforge/registries/DataPackRegistryEvent.java`）用于**新建** datapack 注册表，
`DataPackRegistriesHooks` 只往 `RegistryDataLoader.WORLDGEN_REGISTRIES` 清单加新注册表，没有注入条目的口子。
因此 structure/structure_set 只能以资源 JSON（或 MDG datagen 产出同样 JSON）随 jar 发布。

## 5. 探针/运行期相关 API（M2.8 验证用）

- `net/neoforged/neoforge/event/server/ServerStartedEvent.java` 存在（`getServer()` 继承自
  ServerLifecycleEvent）；游戏总线事件经 `NeoForge.EVENT_BUS.addListener(...)`（NeoForge.java L17，
  仓库先例 `MessageManager.java` L108）。
- `net/neoforged/neoforge/event/tick/ServerTickEvent.java`：`Pre`/`Post` 子类，`hasTime()`、`getServer()`。
- `ServerLevel.setChunkForced(int chunkX, int chunkZ, boolean forced)`（ServerLevel.java L1465）——
  等价 /forceload 的编程入口。
- 方块计数：`LevelChunkSection.hasOnlyAir()`（L106）、`getStates()`（L164）→
  `PalettedContainer.count(PalettedContainer.CountConsumer<T>)`（PalettedContainer.java L304，
  consumer 收 `(state, count)`）——按 palette 计数，不用逐格扫描。
- NBT（移植 OilStructure 序列化用，26.1.2 有 API 断代）：
  - `CompoundTag.put(String, Tag)` 仍存在（CompoundTag.java L229）；
  - **`getInt(String)/getDouble(String)/getList(String,int)/getAllKeys()` 已删除**，替换为
    `getIntOr(name, def)`（L309）、`getDoubleOr`（L333）、`getListOrEmpty(name)`（L369）、
    `getCompoundOrEmpty`（L361）、`keySet()`（L199）、`contains(String)`（L281）；
  - `ListTag.getInt(int)` 改为返回 `Optional<Integer>`（L284），取值用 `getIntOr(index, def)`（L288）；
  - `Tag.TAG_LIST=9/TAG_COMPOUND=10/TAG_INT_ARRAY=11` 常量仍在（Tag.java L21-23）；
  - `IntArrayTag(int[])` 构造器在（IntArrayTag.java L52）。
- `ScheduledTickAccess.scheduleTick(BlockPos, Fluid, int)`（ScheduledTickAccess.java L31；
  LevelAccessor 继承它）——`setOil` 的「置油块+调度流体 tick」语义照旧。
- `FluidState.createLegacyBlock()`（FluidState.java L99）；`Block.UPDATE_ALL = 3`（Block.java L103）。
- `LevelReader.getHeightmapPos(Heightmap.Types, BlockPos)`（LevelReader.java L80）、
  `isEmptyBlock(BlockPos)`（L84）、`ChunkAccess.getHighestSectionPosition()`（ChunkAccess.java L147）、
  `BlockBehaviour.blocksMotion()`（BlockBehaviour.java L520）。

## 6. legacy 1.20.1 世界生成审计结论（移植依据）

**审计范围**：仓库根（冻结 1.20.1 树）全部 java + `buildcraft_resources_generated`。
**审计结论：legacy 世界生成只有油田生成一套**（结构体生成），没有 ore feature/PlacedFeature、没有 biome
modifier、没有可用的自定义 biome 生成。

### 6.1 代码清单（`common/buildcraft/energy/generation/` + 关联）

| 文件 | 角色 |
|---|---|
| `structure/OilStructureFeature.java` | `extends Structure`；`findGenerationPoint` 自掷骰（MAGIC_GEN_NUMBER 种子），返回 stub；`CODEC=simpleCodec` |
| `structure/OilStructureRegistry.java` | `STRUCTURE_TYPE = StructureType.register("buildcraftenergy:oil_spout", CODEC)`；`STRUCTURE_PIECE_TYPE = StructurePieceType.setPieceId(OilStructure::deserialize, id)` |
| `structure/OilStructure.java` | 唯一 StructurePiece（装箱 pieces 列表），`postProcess` → `OilPlacer`；NBT 序列化/反序列化 pieces |
| `structure/OilGenerator.java` | 掷 GenType（LARGE/MEDIUM/LAKE/NONE）、拼 pieces：tendril（地表油湖）/sphere（地下油袋）/spout（油喷柱）/tube/spring |
| `structure/OilGenStructurePart.java` | 4 种 piece：GenByPredicate（球/柱谓词）、FlatPattern、PatternTerrainHeight（贴地表高度图）、Spout、Spring（置 `BCCoreBlocks.springOil` 并传 totalSources）；`setOil` = 置 `crudeOil[0]` 源方块 + `scheduleTick` |
| `structure/OilPlacer.java` | 按 chunk 交集执行 pieces；最后把油块总数喂给 Spring |
| `structure/OilFeatureConfiguration.java` | Info 载体（rand/type/x/z），GenType 在 OilGenerator 里 |
| `datagen/BCDatapackBuiltinEntriesProvider.java` | RegistrySetBuilder：STRUCTURE oil_spout（biomes=`#buildcraftenergy:oil_gen`，step=FLUID_SPRINGS，TerrainAdjustment.NONE）+ STRUCTURE_SET（`RandomSpreadStructurePlacement(spacing=1, separation=0, LINEAR, salt=|MAGIC_GEN_NUMBER>>32|=800672540)`） |
| `BCEnergyWorldGen.java` | **空壳**（全是注释），无运行时 worldgen 注册 |
| `generation/biome/*`（BCBiomes/BCBiomeRegistry/BCEnergyBiomeSource/BiomeInitializer/GenLayerAddOil*/GenLayerBiomeReplacer） | **全部死代码**（1.12 时代 GenLayer 遗留，注释态/无人调用）；oil_desert/oil_ocean 两个 biome JSON 存在但无 biome modifier 挂接，1.20.1 实际不会自然生成 |

### 6.2 生成参数（默认值，`BCEnergyConfig.java` L116-216 + `OilGenerator.java`）

- 每 chunk 判定一次（structure_set spacing=1/separation=0 → 每 chunk 都是候选；世界种子 +
  `MAGIC_GEN_NUMBER=0xD046B4E40C7D07C` 的 `setLargeFeatureSeed` 决定oil 点位 x/z 与掷骰）。
- 概率（`nextDouble()` 一次）：LARGE ≤ 0.04%×bonus；MEDIUM ≤ 0.1%×bonus；LAKE ≤ 2%×bonus（**仅**
  surfaceDepositBiomes 命中才生效，默认该表为空 → LAKE 默认从不生成）。合计默认 **≈0.14%/chunk**
  （约 1 油井/714 chunk）。
- bonus = 3.0（surfaceDepositBiomes 命中）× `generationRate(1.0)` × 30.0（excessiveBiomes 命中；
  默认 {buildcraftenergy:oil_desert, buildcraftenergy:oil_ocean}——死 biome，实际无效）。
- biome 过滤：excludedBiomes 黑名单默认 {minecraft:hell, minecraft:sky}（1.20.1 已不存在的 id → 无效）；
  `BiomeTags.IS_END` 且 |x|/|z|<1200 不生成；维度排除默认 {the_nether, the_end}（结构 JSON 只挂
  `#buildcraftenergy:oil_gen`=`#minecraft:is_overworld` 实现同一语义）。
- 规模（`createStructureByType`）：LARGE 湖半径 4、触须 25+r(20)、地下球半径 8+r(9)、球心 y=minY+20+r(10)、
  喷柱高 10..20 半径 1、底部通管 + spring_oil；MEDIUM 湖 2、触须 5+r(10)、球 4+r(4)、喷柱 6..12 半径 0、
  无 spring；LAKE 仅地表湖（湖半径 6、触须 25+r(20)，深 1..2）。`enableOilSpouts` 默认 true。
- 油方块 = `BCEnergyFluids.crudeOil[0]`（注册名 `buildcraftenergy:oil_heat_0`——**1.20.1 没有裸 `oil` 流体**，
  baseline fluids 60 条里原油即 `oil_heat_0/1/2` 三热度的 still/flow）；spring = `buildcraftcore:spring_oil`
  （BE `TileSpringOil`，totalSources=油块总数）。

### 6.3 neo 侧对应资源现状（M2.8 起点）

- `BcEnergyFluids.OIL_HEAT_0/_FLOW`（`buildcraftenergy:oil_heat_0`/`oil_heat_0_flow`）已注册，
  流体方块 `BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_0`（`PlaceholderFluidBlock extends LiquidBlock`）。
- `BcBlocks.SPRING_OIL`（`buildcraftcore:spring_oil`）已注册（普通 Block 占位；BE 类型占位已挂注册中心）。
- parity 基线（registry-baseline.json）只断言 BLOCK/ITEM/BE/ENTITY/FLUID 五表，M2.8 新增
  STRUCTURE_TYPE/STRUCTURE_PIECE 注册不进 parity 断言范围（不新增 blocks/items/be/entities/fluids）。
