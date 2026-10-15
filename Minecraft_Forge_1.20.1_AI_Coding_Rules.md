# Minecraft Forge 1.20.1 Mod — AI 开发规范

> 本文件用于约束 AI（包括代码生成、代码修改、重构、排错、补全代码的 AI）参与本 Minecraft Mod 项目时的行为。
>
> **目标：稳定、可维护、可扩展、符合 Forge 1.20.1 规范，而不是“能编译就算完成”。**

---

## 0. 项目固定环境

以下内容视为本项目的**硬约束**，除非用户明确要求修改：

- Minecraft：**1.20.1**
- Mod Loader：**Forge**
- Java：**17**
- 构建工具：Gradle / Forge MDK
- IDE：VS Code（允许使用其他 IDE，但不得改变项目结构）
- Mod ID：以项目现有配置为准，**禁止 AI 擅自修改**
- 包名：以项目现有包名为准，**禁止 AI 擅自重命名**
- Minecraft、Forge、Java 版本：**禁止因为“新版本写法更方便”而升级**
- 默认目标：客户端 + 专用服务器均可正常运行，除非功能明确只属于客户端

### 0.1 版本优先级

AI 必须优先遵守：

1. 当前项目实际使用的 Forge 1.20.1 API
2. 项目中已经存在的代码风格和架构
3. Forge 1.20.1 官方文档
4. Minecraft 1.20.1 原版 API
5. 其他资料

**禁止直接套用 Forge 1.21.x、1.20.2、NeoForge 或 Fabric 的 API。**

如果不确定某个 API 是否存在于 Forge 1.20.1：

- 不得猜测；
- 不得把其他版本代码强行改几个名字后使用；
- 应先搜索官方文档、项目源码或现有代码；
- 无法确认时，应明确告诉用户“不确定”，而不是编造 API。

### 0.2 Gradle 缓存位置（重要）

本机 Gradle 用户目录**已迁移到 `D:\gradle-home`**，不再使用默认的 `C:\Users\<用户名>\.gradle`。

执行任何 Gradle 命令（build / runClient / runServer 等）前，必须显式设置：

```powershell
$env:GRADLE_USER_HOME="D:\gradle-home"
```

注意：

- 在沙箱环境中运行 Gradle 会因无法写入 `C:\Users` 而报 Access Denied / RETRY LIMIT EXCEEDED，构建命令必须关闭沙箱（`dangerouslyDisableSandbox: true`）运行；
- 若 IDE 报找不到依赖，检查 IDE 的 Gradle user home 设置是否同样指向 `D:\gradle-home`。

---

# 1. AI 修改代码前的总原则

## 1.1 先理解，再修改

AI 在修改已有代码前必须：

1. 查看相关类；
2. 查看相关注册类；
3. 查看相关事件监听；
4. 查看相关资源文件；
5. 检查该功能是否已经存在；
6. 检查是否存在相似实现可以复用。

**禁止在没有阅读现有代码的情况下重写整个文件。**

## 1.2 最小修改原则

如果用户只要求：

> “给这个法术增加一个效果。”

那么 AI 应尽可能只修改实现该效果所需要的代码。

禁止：

- 顺手重构整个项目；
- 更换命名体系；
- 更换注册方式；
- 大规模移动文件；
- 删除“看起来没用”的代码；
- 自动升级 API；
- 自动修改 Gradle；
- 自动修改 Mod ID。

除非这些修改是解决问题所必需的。

## 1.3 不得擅自改变设计

用户提出的是**功能需求**时，AI 不应擅自重新设计玩法。

例如用户说：

> “这个法术每 30 秒召唤一次蜘蛛。”

AI 不得自行改成：

> “我觉得 30 秒太久，所以改成 20 秒。”

数值、机制、范围、持续时间、伤害、AI 行为等均属于设计内容。

如果发现设计可能导致严重性能问题，可以提出建议，但**未经用户同意不得直接改变**。

---

# 2. Forge 注册规范

## 2.1 优先使用 DeferredRegister

需要注册的内容应优先使用：

- `DeferredRegister`
- `RegistryObject`

例如：

```java
public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

public static final RegistryObject<Item> EXAMPLE_ITEM =
        ITEMS.register("example_item", () -> new Item(new Item.Properties()));
```

不要使用过时或不符合项目现有架构的手动注册方式。

Forge 1.20.1 官方文档也将 `DeferredRegister` 作为推荐的注册方式。

## 2.2 注册对象命名

注册名统一：

```text
snake_case
```

正确：

```text
spider_mother
swarm_rune
royal_jelly
spider_venom_gland
```

错误：

```text
SpiderMother
SpiderMotherEntity
spiderMother
蜘蛛之母
```

注册 ID 必须使用稳定、清晰、不会轻易改变的英文名称。

## 2.3 RegistryObject 的使用

注册对象应通过：

```java
RegistryObject<T>
```

保存，并在需要时：

```java
OBJECT.get()
```

获取实际对象。

禁止在注册阶段过早获取尚未完成注册的对象。

---

# 3. 包结构规范

推荐结构：

```text
com.example.mod
├── ModMain.java
│
├── registry/
│   ├── ModItems.java
│   ├── ModBlocks.java
│   ├── ModEntities.java
│   ├── ModEffects.java
│   ├── ModSounds.java
│   └── ModSpells.java
│
├── item/
├── block/
├── entity/
├── spell/
├── effect/
├── enchantment/
├── capability/
├── event/
├── network/
├── client/
│   ├── renderer/
│   ├── screen/
│   └── model/
│
├── data/
│   ├── loot/
│   ├── recipe/
│   ├── advancement/
│   └── tags/
│
└── util/
```

不要求所有目录一开始都存在。

**功能少时不要为了“看起来专业”建立几十个空目录。**

---

# 4. 类命名规范

## 4.1 Java 类

使用 `PascalCase`：

```java
SpiderMother
SpiderMotherEntity
SwarmSpell
SpiderVenomEffect
ModEntities
```

## 4.2 方法

使用 `camelCase`：

```java
summonSpider()
applySwarmEffect()
createSpell()
```

## 4.3 常量

使用：

```java
UPPER_SNAKE_CASE
```

例如：

```java
MAX_DISTANCE
SUMMON_INTERVAL
DAMAGE_AMOUNT
```

## 4.4 注册 ID

使用：

```text
lower_snake_case
```

---

# 5. 客户端 / 服务端规范

这是本项目的**重点约束**。

## 5.1 不得在公共逻辑中直接调用客户端专属 API

例如以下内容属于客户端相关内容：

```java
Minecraft.getInstance()
```

以及：

- `net.minecraft.client.*`
- 渲染器
- Model
- Screen
- KeyMapping
- 客户端粒子渲染
- 客户端 GUI

这些内容不得随意出现在：

- Entity 公共逻辑；
- Item 公共逻辑；
- Spell 公共逻辑；
- Server 事件；
- Capability 公共逻辑。

## 5.2 物理端判断

不要滥用：

```java
FMLEnvironment.dist
```

或：

```java
Dist.CLIENT
```

来解决本来应该通过正确事件或客户端初始化解决的问题。

客户端注册内容优先放入：

```text
client/
```

并使用 Forge 推荐的客户端生命周期事件或客户端事件。

## 5.3 Level 逻辑

涉及世界逻辑时，优先明确：

```java
if (!level.isClientSide)
```

服务器端执行：

- 伤害；
- 状态效果；
- 生成实体；
- AI；
- 战斗逻辑；
- 数据保存；
- 游戏状态改变。

客户端主要负责：

- 渲染；
- UI；
- 本地视觉效果；
- 本地输入。

---

# 6. 事件系统规范

## 6.1 正确选择事件总线

必须区分：

### MOD Event Bus

用于 Mod 生命周期相关内容，例如：

- 注册；
- Common Setup；
- Client Setup；
- 数据生成。

### Forge Event Bus

用于游戏运行期间的事件，例如：

- LivingAttackEvent；
- LivingHurtEvent；
- LivingDeathEvent；
- TickEvent；
- EntityJoinLevelEvent；
- 玩家事件等。

**禁止把所有事件全部塞进一个 EventHandler。**

## 6.2 不要使用 TickEvent 解决所有问题

禁止形成：

```java
@SubscribeEvent
public static void onTick(TickEvent event) {
    // 所有游戏逻辑全部塞这里
}
```

如果功能属于某个实体，应优先放进实体自己的：

```java
tick()
```

或 AI Goal。

如果功能属于某个效果，应考虑：

```java
MobEffect
```

如果功能属于某个物品，应考虑：

```java
Item
```

如果功能属于玩家持久数据，应考虑：

```java
Capability
```

---

# 7. Entity 规范

## 7.1 实体注册

实体必须：

1. 注册 `EntityType`；
2. 注册 Spawn Egg（如果需要）；
3. 配置属性；
4. 配置客户端 Renderer；
5. 必要时配置 Spawn Placement。

## 7.2 AI 行为

实体的行为优先使用：

```java
Goal
```

实现。

不要在 `tick()` 中写大量：

```java
if (...) {
    // 移动
}

if (...) {
    // 攻击
}

if (...) {
    // 寻找目标
}
```

除非逻辑确实不适合 Goal。

## 7.3 实体查找

禁止每 tick 无限制扫描巨大范围。

错误示例：

```java
level.getEntitiesOfClass(LivingEntity.class, hugeBox)
```

然后每 tick 执行。

必须考虑：

- 扫描范围；
- 扫描频率；
- 实体数量；
- 是否可以缓存目标；
- 是否可以使用 Goal；
- 是否可以降低检查频率。

---

# 8. Tick 与计时器规范

Minecraft：

```text
20 ticks = 1 秒
```

但不要到处写：

```java
if (tickCount % 600 == 0)
```

应该使用具有语义的常量：

```java
private static final int SUMMON_INTERVAL = 30 * 20;
```

或者：

```java
private static final int SUMMON_INTERVAL_TICKS = 600;
```

复杂系统应避免魔法数字。

例如：

```java
if (tickCount % 600 == 0)
```

应优先改成：

```java
if (tickCount % SUMMON_INTERVAL_TICKS == 0)
```

---

# 9. 性能规范

这是本项目的**硬性要求之一**。

## 9.1 禁止无意义高频操作

以下内容不得在每 tick 无限制执行：

- 大范围实体搜索；
- 大量粒子；
- 大量实体生成；
- 路径计算；
- NBT 读写；
- 网络包发送；
- 复杂随机计算；
- 大量方块扫描。

## 9.2 大量召唤物必须有限制

任何：

- 蜘蛛；
- 蜂群；
- 昆虫；
- 粒子；
- 投射物；
- 临时实体；

都必须考虑最大数量。

例如：

```java
MAX_MINIONS = 20;
```

如果用户没有要求上限，AI 应在实现前提醒用户，而不是偷偷加入一个改变玩法的上限。

## 9.3 粒子

粒子必须考虑：

- 数量；
- 生命周期；
- 生成频率；
- 渲染距离。

不要为了“看起来更炫”无限增加粒子数量。

---

# 10. 随机数规范

优先使用 Minecraft / Java 项目已有的随机源：

```java
level.random
```

或实体自身可用的随机源。

不要无意义地频繁创建：

```java
new Random()
```

尤其不要在 tick 中：

```java
Random random = new Random();
```

---

# 11. 数据保存规范

需要跨：

- 重启游戏；
- 世界保存；
- 玩家死亡；
- 区块卸载；

保持的数据，必须明确决定使用：

- EntityDataAccessor；
- Capability；
- SavedData；
- PersistentData/NBT；
- 其他适合的机制。

禁止仅依靠普通字段保存必须持久化的数据。

例如：

```java
private int swarmPower;
```

如果该值需要世界重载后继续存在，则不能只写这个字段。

---

# 12. Capability 规范

如果功能属于玩家/实体的长期附加数据，可以考虑 Capability。

例如：

```text
玩家：
- 法术资源
- 自定义状态
- 解锁内容
- 特殊计数
```

Capability 应：

- 有清晰的数据接口；
- 有必要的序列化；
- 正确处理玩家死亡/克隆；
- 正确处理客户端同步；
- 避免把所有数据都塞进一个 Capability。

---

# 13. 网络同步规范

任何需要客户端显示、但服务器决定的数据，都必须考虑同步。

原则：

> **服务器是游戏状态的权威来源。**

例如：

```text
服务器：
“玩家拥有 3 层虫群之怒”
↓
网络同步
↓
客户端：
“显示 3 层”
```

禁止：

> 客户端自己判断真实游戏状态，然后让服务器跟随客户端。

尤其是：

- 伤害；
- 召唤；
- 状态；
- 冷却；
- 资源；
- 玩家数据。

必须避免客户端伪造。

---

# 14. NBT 规范

NBT Key 使用稳定英文名称：

```text
SwarmPower
SummonCooldown
HasQueenBee
```

不要使用：

```text
虫群力量
随便一个字符串
```

对于复杂数据，应统一管理 NBT Key，避免同一个数据出现：

```text
"SwarmPower"
"swarm_power"
"swarmPower"
```

三种写法。

---

# 15. 配置规范

如果某个数值未来可能需要调整，可以考虑 Forge Config。

例如：

```text
伤害
召唤间隔
最大召唤数量
生成概率
范围
```

但不要把所有数字都做成 Config。

固定的内部常量：

```java
private static final int TICKS_PER_SECOND = 20;
```

不需要配置。

---

# 16. Data Generation / JSON 规范

可以使用 DataGen 的内容优先使用 DataGen：

- Recipe；
- Loot Table；
- Advancement；
- Tags；
- Block States；
- Item Models；
- Language。

不要在 Java 代码中硬编码本来应该由 JSON / DataGen 管理的数据。

资源目录遵循 Minecraft 标准：

```text
src/main/resources/
├── assets/<modid>/
│   ├── blockstates/
│   ├── models/
│   │   ├── block/
│   │   └── item/
│   ├── textures/
│   ├── lang/
│   └── sounds.json
│
└── data/<modid>/
    ├── recipes/
    ├── loot_tables/
    ├── advancements/
    └── tags/
```

---

# 17. 语言文件规范

不要在 Java 代码中直接写玩家可见文本。

错误：

```java
Component.literal("虫群之怒")
```

应优先使用：

```text
item.modid.example
spell.modid.example
effect.modid.example
```

然后在：

```text
zh_cn.json
en_us.json
```

中翻译。

至少保持：

```text
zh_cn
en_us
```

同步。

---

# 18. 资源命名规范

统一使用：

```text
lower_snake_case
```

例如：

```text
spider_mother.png
spider_mother.json
swarm_rune.png
royal_jelly.png
spider_venom_gland.png
```

禁止混用：

```text
SpiderMother.png
spiderMother.png
蜘蛛之母.png
```

---

# 19. Spell / 法术系统规范

如果本项目存在自定义法术系统：

每个法术应尽量独立：

```text
spell/
├── SwarmProtectionSpell.java
├── SummonBeesSpell.java
├── ConfusionBeeStingSpell.java
├── ParasiteSpell.java
└── SummonSpiderMotherSpell.java
```

不要把所有法术逻辑塞入：

```text
SpellHandler.java
```

成为一个几千行的大类。

## 19.1 法术逻辑建议分层

例如：

```text
Spell
 ↓
Spell Effect / Action
 ↓
Entity / Effect / Projectile
```

法术负责：

- 施法；
- 消耗；
- 冷却；
- 目标选择。

实体负责：

- AI；
- 移动；
- 攻击；
- 生命周期。

效果负责：

- Buff / Debuff；
- 层数；
- 持续时间。

---

# 20. 自定义 Effect 规范

自定义效果应优先使用：

```java
MobEffect
```

而不是在多个事件中手动维护：

```java
Map<UUID, Integer>
```

如果效果确实需要特殊数据，再使用适当的数据结构。

效果必须明确：

- 是否可叠加；
- 是否刷新时间；
- 是否刷新等级；
- 是否有视觉效果；
- 是否显示图标；
- 是否影响实体属性。

---

# 21. 自定义实体生成规范

生成实体时必须考虑：

```java
EntityType
```

以及：

```java
Entity#setPos(...)
```

不要重复生成已经存在的实体。

例如“蜘蛛之母每 30 秒召唤保护者”：

应明确：

```text
每次最多几个？
是否允许多个保护者？
保护者死亡后是否重新召唤？
距离过远怎么办？
蜘蛛之母死亡后怎么办？
```

如果用户没有定义这些规则，AI 应询问或提出明确的默认方案。

---

# 22. AI 不得擅自添加的内容

除非用户明确要求，AI 不得擅自添加：

- 新物品；
- 新方块；
- 新法术；
- 新敌人；
- 新维度；
- 新世界生成；
- 新 Boss；
- 新矿物；
- 新货币；
- 新 UI；
- 新配置项；
- 新依赖；
- 新第三方库；
- 新 Mod Loader；
- 新 Minecraft 版本。

尤其禁止为了实现一个小功能而引入新的第三方依赖。

---

# 23. 第三方 Mod / API

如果项目明确依赖其他 Mod：

AI 必须先确认：

1. Mod 是否真的存在；
2. 目标版本是否为 1.20.1；
3. Loader 是否为 Forge；
4. API 是否在当前版本可用；
5. 是否已经加入 Gradle 依赖。

禁止凭记忆编造第三方 API。

如果只是“兼容某个 Mod”，优先采用：

- Optional Dependency；
- Forge ModList 检测；
- InterModComms；
- 对方提供的 API；

避免硬依赖。

---

# 24. 错误处理规范

不要用：

```java
try {
    ...
} catch (Exception e) {
}
```

吞掉异常。

错误至少应：

- 记录日志；
- 给出明确上下文；
- 或让异常暴露出来以便定位。

禁止：

```java
catch (Exception ignored) {}
```

作为常规错误处理方式。

---

# 25. 日志规范

开发调试可以使用：

```java
LOGGER.debug(...)
LOGGER.info(...)
LOGGER.warn(...)
LOGGER.error(...)
```

不要每 tick 输出日志。

禁止：

```java
LOGGER.info("tick");
```

这种高频日志。

正式代码中避免留下大量临时调试输出。

---

# 26. 注释规范

注释应该解释：

> 为什么这么做。

而不是解释：

> 这一行代码在做什么。

不推荐：

```java
// 给玩家增加 1 点伤害
player.setHealth(player.getHealth() + 1);
```

更推荐：

```java
// 蜘蛛之母死亡后不会继续治疗召唤物，避免实体死亡后的逻辑残留。
```

复杂算法、特殊 Forge 行为、容易踩坑的客户端/服务端逻辑应添加注释。

---

# 27. 禁止“魔法数字”

不推荐：

```java
if (distance < 32)
```

推荐：

```java
private static final double SEARCH_RANGE = 32.0D;
```

不推荐：

```java
addEffect(new MobEffectInstance(EFFECT, 600, 1));
```

推荐：

```java
private static final int EFFECT_DURATION = 30 * 20;
private static final int EFFECT_AMPLIFIER = 1;
```

但非常明显且不会变化的常量，例如：

```java
20 ticks = 1 second
```

不必过度抽象。

---

# 28. 代码复杂度控制

AI 不应该为了“高级”而：

- 引入复杂设计模式；
- 创建大量接口；
- 创建过度泛型；
- 创建几十层继承；
- 使用反射；
- 使用复杂 Lambda；
- 使用不必要的事件总线；
- 创建一个简单功能所不需要的框架。

原则：

> **能用简单、清晰、符合 Forge 习惯的代码解决，就不要复杂化。**

---

# 29. 修改已有代码时的保护规则

AI 修改文件时必须尽量保持：

- 原有命名；
- 原有格式；
- 原有注释；
- 原有功能；
- 原有 API；
- 原有注册方式。

如果需要删除代码：

必须确认该代码确实不再使用。

如果不确定：

> 不删除。

---

# 30. 编译检查规范

完成代码后，AI 应优先进行：

```text
./gradlew build
```

如果项目可以运行开发客户端：

```text
./gradlew runClient
```

如果需要专用服务器：

```text
./gradlew runServer
```

出现错误时：

1. 阅读完整错误；
2. 定位第一个真正的错误；
3. 检查 Import；
4. 检查 Forge 1.20.1 API；
5. 检查注册顺序；
6. 检查客户端/服务端；
7. 再修改代码。

禁止看到错误就随机改代码。

---

# 31. AI 输出代码时的要求

当 AI 给出代码时，应说明：

### 修改了什么

例如：

```text
修改：
- 新增 SpiderMotherEntity
- 新增 ModEntities 注册
- 新增客户端 Renderer
- 新增召唤逻辑
```

### 修改哪些文件

例如：

```text
新增：
src/main/java/.../entity/SpiderMotherEntity.java

修改：
src/main/java/.../registry/ModEntities.java
src/main/java/.../client/ClientEventHandler.java
```

### 是否需要资源文件

例如：

```text
需要：
assets/modid/models/entity/spider_mother.json
assets/modid/textures/entity/spider_mother.png
```

### 是否需要运行测试

例如：

```text
建议测试：
1. 单人世界召唤
2. 专用服务器测试
3. 重载世界
4. 多只实体同时存在
```

---

# 32. AI 排错原则

当用户报告：

> “游戏崩了。”

AI 不应立即重新写一套代码。

必须优先要求：

- `latest.log`
- 崩溃报告
- 完整 Stack Trace
- 出错类名
- 出错行号
- 触发条件

排错顺序：

```text
崩溃信息
↓
第一个 Caused by
↓
自己的类
↓
具体行
↓
调用链
↓
判断客户端/服务端
↓
判断生命周期
↓
修复
```

不要只看最后一行错误。

---

# 33. AI 发现需求存在歧义时

如果需求缺少决定性信息，AI 应询问。

例如：

> “蜘蛛之母每 30 秒召唤蜘蛛。”

需要确认：

- 每次召唤几个？
- 大蜘蛛和冰霜蜘蛛是随机还是固定？
- 是否存在数量上限？
- 蜘蛛之母死亡后召唤物是否消失？
- 蜘蛛巢是否永久存在？
- 蜘蛛巢能否被破坏？

但是如果问题不影响代码正确性，可以使用合理默认值并明确说明。

---

# 34. AI 不应过度询问

如果用户要求：

> “把这个物品的伤害改成 10。”

不要询问：

> “你确定吗？”

直接修改。

只有当缺少的信息会导致：

- 无法编译；
- 无法判断实现方式；
- 可能破坏现有功能；
- 存在多个完全不同的设计方案；

才需要询问。

---

# 35. 设计与实现必须分离

AI 应区分：

```text
用户设计
```

和：

```text
AI 技术实现
```

例如用户决定：

```text
蜘蛛之母每 30 秒召唤一次蜘蛛。
```

AI 决定：

```text
使用 Goal 还是 tick 计时器。
```

用户不需要指定具体 Java 实现。

AI 不应把自己的技术选择伪装成用户的设计要求。

---

# 36. 性能优先级

出现以下情况时，AI 必须主动提醒：

### 高风险

- 每 tick 扫描 32+ 格实体；
- 每 tick 生成实体；
- 每 tick 发送网络包；
- 每 tick 大量粒子；
- 大量 Pathfinding；
- 大量方块遍历；
- 无限递归召唤；
- 实体之间互相无限生成；
- 大量永久存在的临时实体。

### 低风险

- 每秒执行一次；
- 仅对附近少量实体执行；
- 单个实体的简单判断；
- 少量粒子；
- 单次网络同步。

---

# 37. 召唤物系统特别规范

如果某个法术会召唤大量实体，必须考虑：

```text
最大数量
生命周期
召唤冷却
距离限制
死亡处理
区块卸载
主人死亡
主人离线
实体加载
实体重复生成
```

尤其禁止：

```text
A 召唤 B
B 召唤 A
A 再召唤 B
```

形成无限生成链。

---

# 38. 游戏玩法数值规范

除非用户要求，AI 不得擅自改变：

- 伤害；
- 防御；
- 速度；
- 冷却；
- 范围；
- 持续时间；
- 生成概率；
- 召唤数量；
- 最大数量；
- 魔力消耗；
- 法术等级；
- 经验消耗。

如果发现数值可能造成性能或平衡问题：

> 提醒用户，而不是直接修改。

---

# 39. 文件修改规范

修改文件前：

```text
读取原文件
↓
理解上下文
↓
确定最小修改位置
↓
修改
↓
检查 Import
↓
检查括号
↓
检查注册
↓
检查资源路径
↓
编译
```

禁止：

> “我没看到完整文件，所以我猜一下结构。”

---

# 40. 不允许凭空创造 API

如果 AI 写：

```java
SomeForgeClass.doSomething(...)
```

必须确保这个 API：

- 在 Forge 1.20.1 存在；
- 方法名正确；
- 参数正确；
- Import 正确。

如果不确定：

```text
先查资料 / 查源码 / 查项目现有代码。
```

不要用“看起来应该是这样”的 API。

---

# 41. 官方 API 优先

当 Forge 官方 API 可以完成需求时：

优先：

```text
Forge API
Minecraft 原版 API
项目现有工具类
```

再考虑：

```text
第三方库
自定义框架
复杂工具
```

不要为了一个简单功能引入依赖。

---

# 42. 最终检查清单

AI 在完成一次较大的 Mod 修改后，应自行检查：

```text
[ ] Minecraft 仍然是 1.20.1
[ ] Forge API 没有误用其他版本
[ ] Java 仍然是 17
[ ] 没有偷偷修改 Mod ID
[ ] 没有偷偷修改包名
[ ] 注册使用正确
[ ] RegistryObject 使用正确
[ ] 客户端/服务端逻辑分离
[ ] 没有错误使用 Minecraft.getInstance()
[ ] 没有把所有逻辑塞进 TickEvent
[ ] 没有明显的每 tick 性能问题
[ ] 没有无限实体生成
[ ] 没有无限粒子
[ ] 没有高频日志
[ ] 没有吞异常
[ ] 没有硬编码玩家可见文本
[ ] 资源路径正确
[ ] lang 文件正确
[ ] JSON 格式正确
[ ] NBT Key 稳定
[ ] 网络同步合理
[ ] 持久化数据正确
[ ] 没有删除未知用途的代码
[ ] 没有擅自改变用户设计
[ ] 没有擅自改变数值
[ ] 没有擅自增加第三方依赖
[ ] 已进行编译检查
```

---

# 43. 最重要的规则

如果只能记住以下十条：

1. **永远以 Minecraft Forge 1.20.1 为目标。**
2. **Java 固定为 17。**
3. **不要把其他版本的 API 当成 1.20.1 API。**
4. **修改代码前先读取现有代码。**
5. **优先使用 DeferredRegister。**
6. **严格区分客户端和服务端。**
7. **不要用 TickEvent 解决所有问题。**
8. **避免每 tick 大范围扫描、生成实体和发送网络包。**
9. **不要擅自改变用户指定的玩法和数值。**
10. **不确定 API 时先查证，不要编造。**

---

# 44. 给 AI 的执行指令

你正在协助开发一个 **Minecraft Forge 1.20.1 / Java 17** Mod。

在任何代码修改前：

> **先理解项目现有结构，再进行最小范围修改。**

你必须：

- 遵守 Forge 1.20.1 API；
- 遵守 Java 17；
- 遵守项目现有命名和结构；
- 优先使用 Forge 官方 API；
- 优先使用 DeferredRegister；
- 正确处理客户端/服务端；
- 注意实体、Tick、事件、网络和性能；
- 不擅自改变用户设计；
- 不擅自改变数值；
- 不擅自升级 Minecraft / Forge；
- 不凭空创造 API；
- 不确定时先查证；
- 修改后尽可能编译验证。

你的目标不是：

> **“写出一段看起来能用的代码。”**

而是：

> **“写出能够在本项目 Minecraft Forge 1.20.1 环境中长期维护、稳定运行，并与现有代码兼容的实现。”**
