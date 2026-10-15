# 芙莉艾丝 Boss 机制数值表（r90）

> 数据来源：`entity/FriliasEntity.java`、`util/Mechanics.java`、`config/FRBConfig.java`（config/frb_boss.toml）。
> 所有 cd 单位：tick（20 tick = 1 秒）。最终触发间隔 = 基础 cd × 阶段倍率（默认 1.0，最小 1 tick）；最终伤害 = 基础伤害 × 全局倍率（damageMultiplier）× 阶段伤害倍率。

---

## 一、基础属性

| 属性 | 数值 |
|---|---|
| 最大生命 | **600** |
| 护甲 | **8** |
| 攻击力（近战） | **7**（悬浮贴身 2.4 格内触发，间隔 16 tick） |
| 移动速度 | **0.33**（天空阶段 0.45，枯萎阶段 0.24） |
| 击退抗性 | **1.0**（完全免疫击退） |
| 追踪范围 | **64** |

阶段血量变化：
- **自然~虚空（phase 0-5）**：每阶段打空血 → 锁 1 HP → 100 tick（5s）转阶段演出 → **回满 600** 进入下一阶段。
- **枯萎（phase 6）**：最大生命改为 **10**，生成 3 朵死亡之花护命（花存活时 boss 完全无敌）。
- **时间（phase 7，二周目）**：最大生命改为 **400**、护甲 8、进入时回满；打空血 → 锁 1 HP 启动万花凋零咏唱。

---

## 二、减伤计算（`FriliasEntity.hurt()`，按顺序叠加）

仅对 **非 `bypasses_invulnerability`** 类伤害生效：

```
1. 单次封顶：amount = min(amount, 最大生命 × maxHitFraction)      ← 默认 15%（600×0.15=90 点封顶）
2. 连击衰减：距上次受击 < comboWindowTicks(5t) → amount × comboFactor  ← 默认 ×0.4
3. 滑窗减伤：60 tick 滑窗内受击 tick 数 > 40 → amount × 0.6
```

**豁免规则**：
- **丰饶花瓣**（PetalVolleyEntity 直击）：豁免 2、3 两条衰减（只吃单次封顶）。
- **时间阶段**：受击时 **10% 概率**整次伤害无视（回到过去），不对 bypasses_invulnerability 类生效。
- **召唤物海等高频多源伤害**：吃满全部三层衰减。
- 花灵伤害（`floral` 伤害类型）挂 `minecraft:bypasses_cooldown` tag：无视原版受击无敌帧（0.5s），与上述减伤正交。

**锁血/反作弊**：
- `kill()` 重写为空；`setHealth(≤0)` 仅允许合法转阶段路径；`isInvulnerableTo` 拦截 BYPASSES_INVULNERABILITY（虚空//kill 等）直到 phase 6 之前。
- 唯一逃脱：难度切 **peaceful** 时 boss 直接离场（战斗判负、饰品解封）。

配置项（frb_boss.toml → defense）：
| 键 | 默认 | 说明 |
|---|---|---|
| maxHitFraction | 0.15 | 单次伤害上限（占最大生命比例，1.0=关闭） |
| comboProtectionEnabled | true | 连击保护总开关 |
| comboWindowTicks | 5 | 连击窗口（tick） |
| comboFactor | 0.4 | 连击窗口内伤害折算系数 |

---

## 三、机制解锁档位（mechanicTier）

| 档位 | 解锁条件 |
|---|---|
| 0 | 阶段初始可用 |
| 1 | 当前阶段血量跌破 **70%** |
| 2 | 当前阶段血量跌破 **40%** |

---

## 四、自然阶段（phase 0，600 HP / 护甲 8）

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 荆棘双弹（SpikeVolley） | 200t（10s） | 0 | 身后两侧各 1 颗荆棘飞弹，锁定玩家位置快照，命中 **5 点物理**；r92 落点提前 60t 显示绿色 aoe_circle 预警 |
| 花瓣光束（自然） | 100t（5s） | 0 | 24t 读条 + **自然法阵（nature.png）**展开 → **3 点魔法** + 中毒 II 5s（激光类，不延后） |
| 花之摇篮 | 240t（12s） | 0 | 随机玩家脚下法阵 3s → 落花砸击 + 花灵之助 buff；空摇篮惩罚 8 点；r92 法阵圆周 + 离地 1.5 格高 12 颗绿色 dust 粒子包边（FF14 踩塔风格） |
| 荆棘共生（多人专属） | 360t（18s） | 0 | 点名 4s 红圈预警 → 3 格内玩家平分 **16 点魔法**（落单独吞） |
| 自然回血 | — | — | 每 5 tick 恢复（r72 加速版） |

## 五、焚尽阶段（phase 1，600 HP / 护甲 8）

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 点燃附近花草 | 200t（10s） | 0 | 12×3×12 内原木/树叶/花草原地铺生之焰附层 |
| 玩家脚下生焰 | 80t（4s） | 0 | 32 格内玩家脚下 ±4 格随机 3 处铺生之焰 + FLAME 粒子 |
| 灼热之种 | 200t（10s） | 0 | 红圈+红标 → 引信 **60t（3s）** 后爆炸：1.5 格内 **8 点爆炸伤害**（不破坏地形） |
| 烈焰击退 | 100t（5s） | 0 | 30t 蓄力 → 直径 6 格爆炸 + 击退 6 格，**6 点基础** |
| 光辉炎柱 | 240t（12s） | 0 | boss 周围 16~24 格随机落 8 个半径 3 圆形预警，50t 后爆发：**12 点花灵伤害 + 6 点爆炸伤害**；冲天炎柱视觉（FLAME 粒子柱 + 爆炸核心） |

## 六、霜华阶段（phase 2，600 HP / 护甲 8）

常驻：36 格内玩家持续【霜之华】（攻击伤害 -50%）。

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 冰刺双弹（SpikeVolley） | 200t（10s） | 0 | 身后两侧各 1 颗冰刺飞弹，命中 **6 点魔法** + 缓慢 III 3s；r92 落点提前 60t 显示冰蓝色 aoe_circle 预警（与荆棘绿区分） |
| 花瓣光束（霜华） | 100t（5s） | 0 | 24t 读条 → **4 点魔法** + 缓慢 III 5s |
| 霜凝之视 | 240t（12s） | 0 | 30t 蓄力 → 视线判定（画面内有 boss 吃满）**8 点冰伤** + 失明 + 黑暗 60t |
| 随机冰刺法阵 | 240t（12s） | 1 | boss 周围 16~24 格随机落 6~8 颗冰刺，40t 后同时爆发 |
| 钢铁月环 | 140t（7s） | 0 | 圆形/环形 AOE（同虚空潮汐结构），**60t（3s）读条**，随机先环后圆或先圆后环；环形外半径 18 / 内安全 8，圆形半径 9；每次结算 **8 点魔法** + 160 粒 SNOWFLAKE；贴图 frost_moon_ring/circle |

## 七、云雨阶段（phase 3，600 HP / 护甲 8）

常驻：40 格内玩家持续【云之怜】；4 枚环绕符文 = 4 次传送（每掉 20% 血传送 10 格），**4 次用尽后强制回血 15%**（90 点）。

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 雨之华叠层 | 200t（10s） | 0 | 32 格内全员叠 1 层雨之华（最高 12 层，每层 -5% 最大生命） |
| 落雷 | 80t（4s） | 0 | 按人数点名 1/2/3 人，50t 后落雷；3 格内有队友伤害翻倍（6→12） |
| 积雨云 | 240t（12s） | 1 | 最近玩家头顶 +3 格生成（r96 修正：原 +6 太高看不见），悬停追踪最近玩家头顶，8 格内每 20t 附加潮湿 1200t |
| 万变水波 | 转阶段生成 | — | boss 周围 8 格生成 4 个半径 3 圆形水面（正点 N/S/E/W 或斜向随机），持续存在；踏入获得 5s【浸泡】（受伤 +25%）+ 1 层雨之华 |
| 云雨全屏 AOE | 480t（24s） | 0 | 领域内全员 **6 点花灵伤害 + 2 格击退**；释放时从 boss 中心向外扩散一波蓝色 SPLASH/FALLING_WATER 粒子冲击波（可视化范围）；释放后万变水波位置正点↔斜点互换 |

## 八、天空阶段（phase 4，600 HP / 护甲 8，移速 0.45）

常驻：进阶段时以当前位置为锚点固定悬停（地面上方 3 格，NoGravity），**不再跟随玩家跑动**；仅原地转头（yaw/pitch）看向玩家并追踪视线。

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 盘旋冲击波 | 80t（4s） | 0 | 16 格内玩家推力 0.9 + 击退，**3 点物理**；击退结算后**必接风眼月环** |
| 风刃 | 200t（10s） | 1 | 随机方向双风刃（相反方向），**6 点物理** + 击退 3 格，寿命 80t |
| 风眼月环（独立） | 320t（16s） | 1 | boss 位置 60t 预警（wind_eye_4_18 贴地贴图，内安全 4 格 / 外危险 18 格），结算帧环带内 **8 点物理 + 向上击飞约 4 格**（v0≈0.82，环带撒 160 粒 CLOUD/END_ROD）；击退链式不受此 cd 限制，`windEyeLockTicks=75` 防叠加 |
| 羽剑冲锋 | 240t（12s） | 1 | 朝当前目标方向释放羽剑实体：60t 贴地**红色半透明长条预警**（长 14 格 × 宽 3 格，贴图 `feather_sword_warn.png`，沿冲锋方向）→ 16t 冲锋扫掠（3 格宽伤害带），命中 **8 点物理** + 击退；剑模型按范围放大（scale = 2.0 + (range-3)/3，range 6~20） |

## 九、虚空阶段（phase 5，600 HP / 护甲 8）

常驻：**离地 2.5 格悬浮**（NoGravity）、静止、不索敌；进阶段立刻喷 6 条裂隙。

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 虚空裂隙 | 100t（5s） | 0 | 每次 2 条（场上上限 12）；2 格内吸引 + 传送至 9~16 格空地 + **10 点魔法** |
| 虚空潮汐 | 240t（12s） | 0 | 随机顺序先环形后圆形 aoe（或先圆后环）。环形：外半径 18 / 内半径 8（贴图 void_aoe_ring，内外比 8:18）；圆形：半径 9（贴图 void_aoe_circle，环形的一半）。每次 60t 预警 → **8 点魔法** + 160 粒 PORTAL/REVERSE_PORTAL；两次间隔 20t |
| 虚空侵蚀（地块） | — | — | 裂隙生成时地面铺附层，踩踏每 20t 2 点 + 叠层（最高 20，每层 -1% 生命/攻击/移速） |

## 十、枯萎阶段（phase 6，10 HP / 移速 0.24）

常驻：boss 无敌（花在则免疫一切伤害）；全屏褪色 FX_GRAY 每 20t 刷新。

| 机制 | 内置 cd | 档位 | 数值 |
|---|---|---|---|
| 死亡之花复活 | 240t（12s） | 0 | 每 12s 检查：花被击杀超过 60t 且仍有花存活且 <3 → 复活该 slot |
| 凋零剥夺（r95） | 20t（1s） | — | 每秒清除领域 24 格内所有玩家的全部正面 buff（BENEFICIAL 类），配合封印黑灵草禁止偷血 |
| 枯萎凋零 AOE（r95） | 140t 周期（40t 读条 + 100t cd = 7s） | 0 | 2s 读条（激光同路径：6 锚点灰黑 dust 汇聚 + 脚下**枯萎法阵（wither.png）**展开 + boss 头顶 decay_progress 读条条）→ 读条归零对 **24 格** 内所有生存/冒险玩家造成 **8 点花灵伤害**（`floral` 伤害类型，无视受击无敌帧），同时从 boss 中心向外扩散一波紫黑色 SOUL_FIRE_FLAME/WITCH 粒子冲击波；读条期间播放 WITHER_PREPARE_SUMMON 音效 + 施法喊话 |
| 万花凋零（终结） | — | — | 三花全灭 → 咏唱 120t（6s），咏唱池 120 HP、**无额外减伤**，玩家可随时打死 boss 打断；黑洞 + 冲击波 + 虫洞镜头；**咏唱读满 120t 后未被击杀 → 24 格内所有玩家秒杀级伤害（1e9 魔法）+ 地面浮现 `mille_florum.png` 魔法阵（半径 24 格，60t）**；初见触发，二周目跳过进时间阶段 |

## 十一、额外阶段·时间（phase 7，400 HP / 护甲 8，二周目）

进入条件：三花全灭时领域内任意玩家持有「解封的丰饶」或「万花万草之灵」。
常驻：10% 概率受击无视（回到过去）；血条使用 `time_progress_frame/fill.png` 贴图（外框+内填充，与其他阶段色块填充不同）。

| 机制 | 内置 cd | 数值 |
|---|---|---|
| 预支生命 | 200t（10s） | 吸取领域内所有玩家当前生命 **25%**，合计全额回复自身 |
| 时间停止 | 240t（12s） | 持续 4s 万物静止（快照拉回）；第 30t 必接连招：自然光束/灼热之种/风刃 三选一不重复轮换 |
| 时间倒流 | 360t（18s） | 领域内玩家回溯 3 秒前位置与视角（不回溯生命/效果） |
| 旧技能轮换 | 240t（12s） | 随机复用：自然光束(3)/灼热之种(8)/霜凝(8)/积雨云/风刃(6) |
| 时间潮汐 | 240t（12s） | 随机顺序先环形后圆形 AOE（或先圆后环）。环形：外半径 18 / 内半径 8（贴图 `time_aoe_ring`）；圆形：半径 9（贴图 `time_aoe_circle`）。每次 60t 预警 → **8 点魔法**（× `dmgTimeTide`）+ 160 粒金色圣星（TIDE_TIME_STAR）；两次间隔 20t |
| 万花凋零（终局） | — | 打空血 → 咏唱 120t（120 HP 池、无额外减伤，可随时打断）；**读条结束未被击杀 → 24 格内所有玩家秒杀 + `mille_florum.png` 魔法阵** |

**终结掉落与音效（r90）**：
- 无论玩家强杀（`die()`）还是咏唱承认路径（`milleFlorumExit(true)`），均掉落：原始花瓣 ×6、月之水晶碎片 ×1、世界之花意志 ×2。
- 终结时播放 `FRBSounds.FRILIAS_DEATH` 音效。
- BGM 不再硬停，改为 `TickableSoundInstance` 在 40 tick（2s）内线性渐弱至静音后自动停止。

---

## 附：通用框架参数

| 项 | 数值 |
|---|---|
| 开场演出 | 200t（10s）无敌 |
| 转阶段演出 | 100t（5s）无敌，结束回满当前阶段血量 |
| 激光读条 | 24t（1.2s），读条满瞬间发射 |
| 预警标记 | 红圈 80t（4s）/ 头顶 marker 30~80t |
| 领域半径 | 机制 24~40 格，预警广播 64 格 |

---

## 十、r92 改动笔记

### 修 BUG：花之摇篮"双法阵"

历史上荆棘双弹的预警直接复用 `FlowerCradleSigilEntity.spawnWarn`（同一贴图实体），
与 B1 花之摇篮机制（240t）并行触发时玩家会看到同一时刻脚下叠两张一模一样的法阵。
r92 拆出独立 `AoeWarnSigilEntity` + `textures/effect/aoe_circle.png` 贴图：

| 用途 | 实体 | 贴图 | 染色 |
|---|---|---|---|
| B1 花之摇篮（踩塔，含结算） | `FlowerCradleSigilEntity` | `flower_cradle_sigil.png` | 白 |
| 荆棘双弹预警 | `AoeWarnSigilEntity` | `aoe_circle.png` | 绿 `(0.49, 0.90, 0.55)` |
| 冰刺双弹预警 | `AoeWarnSigilEntity` | `aoe_circle.png` | 冰蓝 `(0.62, 0.86, 1.00)` |

`FlowerCradleSigilEntity.spawnWarn` / `DATA_WARN_ONLY` 已删除。

### 新增：花之摇篮粒子包边（FF14 踩塔风格）

参考 FF14 数据包 `memory_torrent_song/color.mcfunction` 的 dust 圆环塔身方案，
r92 在 `FlowerCradleSigilRenderer` 渲染时附加粒子包边：
- 半径 2.0 格（与判定范围一致）
- 离地 1.5 格高
- 12 颗绿色 dust 粒子沿圆周均匀分布（客户端本地生成，不占网络）

---

## 十一、r93 改动笔记

### 修 BUG：创造模式玩家仍被 boss 机制点名

根因不是开场 introIsMultiplayer 一次性判定，而是机制取目标的总闸 Mechanics.playersInDomain
（getEntitiesOfClass(ServerPlayer.class, AABB)）不过滤游戏模式；另有数处手动索敌
（getNearestPlayer/getRandomPlayer）会点到创造玩家。r93 修复：
- playersInDomain 统一过滤 isCreative()||isSpectator()（fightersInRange 顺带修正）；
- 索敌 Goal 显式拒绝创造/旁观，canContinueToUse 让中途切创造的玩家立即脱战；
- 云雨传送落点、天空阶段跟随、积雨云点名、时间阶段 setTarget 等改走 rbNearestFighter。

### 新增：芙莉艾丝说话系统（仿梦幻终焉 UomWither）

参考 LongLookAtPlayerGoal（盯人说话）与 ChanBegGoal（只对创造玩家害羞）：
- 转阶段台词：phase_1~6 + 时间阶段 phase_7；
- 施法喊话：机制调度处 30% 概率 + 10s 冷却（cast_0~3）；
- 战斗闲聊：有目标时低频随机（idle_0~3，约 24s+ 间隔）；
- 创造模式害羞：同一创造玩家准星连续对准 boss 3 秒（视角点积>0.985+视线）触发，
  boss 盯回去 + 爱心粒子 + 紫水晶音效 + shy_0~2 台词，30s 冷却；
- 仁慈救援（MercyEvents 传送回出生点）后 boss 定向私聊 mercy 台词。
- 台词统一「<芙莉艾丝> …」聊天栏格式，键 frb.boss.say.*（zh_cn/en_us 对齐）。

---

## 十二、r95 改动笔记

### 新增：枯萎阶段大范围 AOE + 凋零剥夺

枯萎阶段（phase 6）此前只有死亡之花复活 + 万花凋零终结，boss 全程纯挨打缺乏主动压制。
r95 新增两个常驻机制：

1. **凋零剥夺**：每秒（20t）清除领域 24 格内所有玩家的全部正面 buff（`MobEffectCategory.BENEFICIAL`）。
   配合 r77 已有的封印黑灵草（BLACK_SPIRIT），玩家在最后阶段无法靠黑死病光环或回血 buff 偷血续命。

2. **枯萎凋零 AOE**：7 秒周期循环——40t（2s）读条 → 24 格 8 点花灵伤害 → 100t（5s）cd。
   - 读条完全复用激光（花瓣光束）的渲染路径：
     - 服务端 `tickDecayAoe` 每 tick 递减 `decayAoeChargeTicks`，通过 `DATA_DECAY_AOE_CHARGE` 同步给客户端；
     - 6 锚点（头顶/身侧/脚下各两）绕竖直轴旋转，半径随进度从 1.5 收拢到 0.25；
     - 枯萎灰黑（`PHASE_COLORS[6]=0x3c3c3c`）`DustParticleOptions` + 每 4t 发射 `CONVERGING_PETAL` 汇聚到 boss 身体中心；
   - 客户端 `FriliasRenderer`：
     - `render()` 把 `getDecayAoeCharge() > 0` 纳入读条判定，`DECAY_AOE_CHARGE_TICKS=40` 作为进度分母；
     - 脚下展开**按阶段切换的属性法阵**（枯萎阶段为 `boss_phase_sigils/wither.png`，r96 起不再固定花之摇篮）；
     - `drawChargeBar()` phase 6 分支使用用户提供的 `decay_progress_frame/fill.png`（`textures/gui/hud/`）绘制头顶读条条；
     - `onRenderEntityPre` 蓄力姿态判定加入 `getDecayAoeCharge() > 0`，读条期间双手交叉蓄力；
   - 结算 `releaseDecayAoe`：`floral` 伤害类型（挂 `minecraft:bypasses_cooldown` tag，无视原版 0.5s 受击无敌帧），
     仅对 `Mechanics.playersInDomain` 返回的生存/冒险玩家生效（创造/旁观已被统一过滤）。

### 设计说明

- 总周期 140t（7s）比激光的 100t（5s）略长，给玩家留出走位 + 救花的窗口；
- 8 点花灵伤害在枯萎阶段 boss 只有 10 HP 的背景下主要起**清场逼走位**作用，而非输出压力；
- 读条视觉与激光完全一致（同 `tickPetalBeamCharge` 的锚点/汇聚公式），只换颜色和汇聚终点（boss 中心而非面前目标点），
  保持"芙莉艾丝的蓄力都长一个样"的视觉统一性。

## 十三、r96 改动笔记

### 新增机制

1. **霜华阶段 — 钢铁月环**（cd 140t，3s 读条）
   - 完全复用虚空潮汐的环形/圆形 AOE 结构（`castFrostMoonRing`），贴图换为 `frost_moon_ring.png` / `frost_moon_circle.png`；
   - 粒子用 SNOWFLAKE，音效 `PLAYER_HURT_FREEZE`；
   - 基础伤害 8 点魔法（`dmgFrostMoonRing` 倍率）。

2. **焚尽阶段 — 光辉炎柱**（cd 240t = 12s）
   - 8 个随机落点（参照霜华随机冰刺法阵，boss 周围 16~24 格），半径 3 圆形预警（`flame_pillar_circle.png`），50t 后爆发；
   - 伤害：12 点花灵 + 6 点爆炸（`dmgFlamePillar` 倍率）；
   - 炎柱视觉：40 粒向上喷射的 FLAME 粒子柱 + EXPLOSION_EMITTER/EXPLOSION/LAVA 核心粒子 + 爆炸音效。

3. **云雨阶段 — 万变水波**（转阶段生成，持续存在）
   - 进入云雨阶段时在 boss 周围 8 格生成 4 个半径 3 的圆形水面（`WaterRippleEntity`，贴图 `water_ripple.png`）；
   - 排列方式随机：正点（N/S/E/W）或斜向（4 对角线）；
   - 玩家踏入：5 秒【浸泡】（受伤 +25%，新效果 `SOGGY`）+ 1 层雨之华；
   - 云雨全屏 AOE 释放后位置正点↔斜点互换。

4. **云雨阶段 — 全屏 AOE**（cd 480t = 24s）
   - 领域内全员 6 点花灵伤害 + 2 格击退（`dmgRainAoe` 倍率）；
   - 释放后重置万变水波位置。

### 修正

- **积雨云高度**：由"地面 +5"改为"最近玩家头顶 +3"（`RainCloudEntity.tick`），解决原高度太高玩家看不见雨云的问题；生成高度同步从 +6 改为 +3。
- **读条脚下法阵**：r96 起所有阶段读条（花瓣光束/枯萎 AOE/技能施法）脚下法阵不再固定花之摇篮，改为按当前阶段切换 `boss_phase_sigils/{phase}.png`（自然 nature / 焚尽 ember / 霜华 frost / 云雨 rain / 天空 sky / 虚空 void / 枯萎 wither / 时间 time）。

### 新增系统

- **技能队列**：boss 释放机制时（`skillCastTicks > 0` 或 15t 法阵窗口内），cd 到点的其他机制不立即释放，而是入队暂存，等当前机制释放完毕后按入队顺序依次结算。避免多机制同帧重叠导致预警混乱。
- **云雨/枯萎 AOE 粒子扩散波**：全屏 AOE / 枯萎 AOE 释放时，从 boss 中心向外扩散一波环形粒子冲击波（云雨为蓝色 SPLASH/FALLING_WATER，枯萎为紫黑 SOUL_FIRE_FLAME/WITCH），可视化 AOE 范围。

### 新增效果

- **浸泡（soggy）**：云雨阶段万变水波 debuff，受到的所有伤害 +25%，持续 5 秒。由 `CharmEvents.onLivingHurt` 钩子实现伤害倍增。

### 贴图清单（待用户提供）

| 贴图路径 | 用途 | 建议尺寸 |
|---|---|---|
| `textures/effect/frost_moon_ring.png` | 钢铁月环 环形预警 | 512×512（外半径 18，同 void_aoe_ring） |
| `textures/effect/frost_moon_circle.png` | 钢铁月环 圆形预警 | 256×256（半径 9，同 void_aoe_circle） |
| `textures/effect/flame_pillar_circle.png` | 光辉炎柱 预警圆 | 96×96 或 128×128（半径 3） |
| `textures/effect/water_ripple.png` | 万变水波 持久地面 | 96×96 或 128×128（半径 3） |
| `textures/effect/mille_florum.png` | 万花凋零终结技 魔法阵（kind=7） | 半径 24 格，建议 512×512 |

半径 3 格贴图尺寸说明：现有 `void_aoe_circle.png`（256px，半径 9）比例约 14.2 px/格（按直径计），半径 3（直径 6）约 85px，取 96 或 128（2 的幂更友好）。

