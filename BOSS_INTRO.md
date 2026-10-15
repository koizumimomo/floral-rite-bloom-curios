# 世界之花·芙莉艾丝 —— 剧情介绍与 Boss 机制全览

> 本文档由 BOSS_TODO.md 改写（2026-10-05，r71），既是 mod 剧情介绍文案，也是 boss 机制的完整状态清单。
> 状态标记：`[✅ 已完成]` / `[⚠️ 部分完成]` / `[❌ 未完成]` / `🎨` 依赖用户提供贴图。

---

## 一、剧情：花灵的力量与世界之花的试炼

### 背景故事

玩家在探索世界时，从花草的低语中察觉到了一种古老的力量——**花灵之力**。
它是「世界之花」万千花草之灵的馈赠，在漫长的岁月中流散进尘世的花草之间。

玩家追随着这份力量的指引，从**世界之花**中截取了一缕本源，将它凝炼成七件**花灵饰品**随身佩戴：

| 阶段 | 花灵饰品 | 力量主题 |
|---|---|---|
| 自然 | 森语藤 | 生长与荆棘 |
| 焚尽 | 焚生藤 | 烈焰与爆裂 |
| 霜华 | 寒风铃 | 冰霜与凝视 |
| 云雨 | 天泪花 | 雷雨与潮湿 |
| 天空 | 天穹花 | 风刃与苍穹 |
| 虚空 | 星界花 | 裂隙与侵蚀 |
| 枯萎 | —— | 凋零与死亡 |

集齐七件花灵饰品（成就「万花齐放」）后，世界之花才会为玩家敞开**永恒花园**的大门。

### 挑战：向世界之花证明自己

**挑战 Boss，即是向世界之花证明——玩家是否真正拥有驾驭这份力量的资格。**

- **第一次击败**：世界之花的化身「万千花草之灵·芙莉艾丝·于死亡中绽放的花」认可了玩家，
  交出七件被封印的花灵饰品与祝福——**但她并未使出全力**。
  她的力量远不止七阶段的轮回，只是不忍对初窥门径的"花之继承者"痛下杀手。

- **第二次挑战**：当玩家**解封了丰饶**（丰饶饰品经「世界之花的意志」解封），
  或佩戴着**神环「万花万草之灵」**站到芙莉艾丝面前时，
  她将收起保留——三花全灭后不再咏唱万花凋零，而是**逆转时间的法则，
  进入隐藏的「额外阶段·时间」**，以全部实力迎接这场真正的试炼。

> 二周目判定（I1，已实现）：击败三花后检测领域内任意玩家持有「解封的丰饶」（NBT 解封标记）
> 或「万花万草之灵」，有则跳过万花凋零直接进入时间阶段。

### 战斗全流程

```
花草低语 → 七件花灵饰品 → 万花齐放 → 永恒花园
   → 世界之花催熟（骨粉）→ 召唤芙莉艾丝（开场三幕演出）
   → 七阶段轮回：自然 → 焚尽 → 霜华 → 云雨 → 天空 → 虚空 → 枯萎
      （每阶段结束封印玩家对应属性的花灵饰品）
   → 三朵死亡之花 → 全灭触发终局：
      · 初见 → 万花凋零咏唱（锁血 1HP，最后一秒可打断/可击杀）
      · 携解封丰饶/神环 → 额外阶段·时间（全力）
   → 胜利：芙莉艾丝亲手交出祝福（万花万草之灵）+ 全饰品解封复原
```

---

## 二、Boss 机制全览（含完成状态）

### A. 通用框架

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| A1 | ✅ | 机制调度器 | `PhaseMechanic` 接口 + `Mechanics.all()` 集中注册，cooldown 取模 + tier 阈值 |
| A2 | ✅ | 预警→结算框架 | `sendMarker`（头顶标记，按颜色分流：FROST_EYE/GARDEN_PETAL/LIGHTNING_MARK）/ `sendCircle`（地面预警圈），客户端本地渲染 |
| A3 | ✅ | 单人/多人模式 | r64：开场扫描一次 `introIsMultiplayer` 整场固定；摇篮/落雷按人数点名。多人专属机制用 `unlockedInMultiplayer()→false` 标记 |
| A4 | ✅ | 血量分段解锁 | `mechanicTier` 0/1/2，70%/40% 静默解锁（提示文本已按需求删除） |
| A5 | ✅ | 机制实体免疫 | `MechanicEntity` 基类，hurt=false + canBeAffected 拦截；r71 补 `PetalVolleyEntity`（Projectile 子类）三重拦截 |
| A6 | ✅ | 激光读条 | r66：6 锚点（头顶/身侧/脚下各两处）粒子收拢 24 tick → 花瓣光束；r67 读条 HUD 接用户贴图 |

### B. 自然阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| B1 | ✅ | 花之摇篮 | cd **240 tick（12s）**；地面法阵 3s → 落花砸击 +【花灵之助】buff；多人最多 3 个；空摇篮惩罚 8 点；**r71 法阵简化为单张静态贴地 quad（不旋转、不脉冲、不叠描边）** 🎨 |
| B2 | ✅ | 荆棘共生 | cd **300 tick（15s）**；多人专属；点名红圈+红标 4s → 以被点名者为中心 3 格内所有玩家平分 16 点魔法伤害（落单独吞全部） |
| B3 | ✅ | 末影龙式荆棘飞弹 | cd **200 tick（10s）**；boss 身后两侧 0.5 格处发射 2 颗荆棘飞弹（`SpikeVolleyEntity`），锁定玩家位置快照作为瞄准方向，命中 5 点物理伤害。**与 B2 互补共存** |
| B4 | ✅ | 花瓣光束（自然版） | cd **160 tick（8s）**；`startPetalBeamCharge` 6 锚点（头顶/身侧/脚下各两处）粒子收拢 24 tick → 3 点魔法伤害 + 5 秒中毒 II |

### C. 焚尽阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| C1 | ✅ | 点燃附近花草 | cd **30 tick（1.5s）**；boss 周围 12×3×12 方块内 log/leaf/植物/紫水晶簇上方铺 LIFE_FIRE 附层（r64 不再替换植物本体） |
| C2 | ✅ | 玩家脚下生焰 | cd **22 tick（1.1s）**；附近玩家脚下 4 格内随机 3 处 → 地面铺 LIFE_FIRE + FLAME 粒子 |
| C3 | ✅ | 灼热之种 | cd **240 tick（12s）**；红圈+红标 → 引信 **80 tick（4s）**后引爆。**r71：手动广播原版爆炸粒子（EXPLOSION_EMITTER×1 + EXPLOSION×4 + POOF×8 + FLAME×6）+ 原版爆炸音效**，仅对 1.5 格内玩家结算 8 点爆炸伤害。**不破坏地形、不放生之焰、避免 `sl.explode()` 引发卡顿** |
| C4 | ✅ | 烈焰击退 | cd **160 tick（8s）**；30 tick 蓄力 → 直径 6 格爆炸 + 击退 6 格 |

### D. 霜华阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| D1 | ✅ | 霜凝 | cd **400 tick（20s）**；1.5s（30 tick）蓄力 → 视线判定（画面内有 boss 才吃满）8 点冰伤+失明+黑暗 60 tick；**r66**：boss 头顶 Gaze 印记 + 玩家周围 10 粒环绕 + chou_hen.ogg 提示音；**r64 加强特效**：蓄力音量 0.6→1.4、6 颗霜眼散开、玩家头顶冰蓝 marker（持续 30 tick）、蓄力期间持续 FROST_EYE + SNOWFLAKE 冷气环；结算粒子 30 颗→100 颗 + 8 颗 FROST_EYE；白闪 0.6→0.85；frost_eye.png 已放到 `textures/particle/` |
| D2 | ✅ | 末影龙式冰刺飞弹 | cd **30 tick（1.5s）**；boss 身后两侧 0.5 格处发射 2 颗冰刺飞弹，锁定玩家位置快照作为瞄准方向，命中 6 点魔法伤害+3 秒缓慢 III |
| D3 | ✅ | 随机法阵冰刺 | cd **200 tick（10s）**，unlockedAt 1；boss 周围 16~24 格随机落 6~8 颗冰刺，40 tick（2 秒）后同时爆发。用 `Heightmap.MOTION_BLOCKING_NO_LEAVES` 取地表（与 boss Y 无关），向上找 6 格内最上方空气 |
| D4 | ✅ | 花瓣光束（霜华版） | cd **160 tick（8s）**；`startPetalBeamCharge` 6 锚点收拢 24 tick → 4 点魔法伤害 + 5 秒缓慢 III |

### E. 云雨阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| E1 | ✅ | 雨之华 | cd **60 tick（3s）**；领域内全员叠 1 层 `RAIN_BLOOM`（最高 12 层，每层 -5% 最大生命）；潮湿 ×1.5 落雷伤害（`CharmEvents` 钩子） |
| E2 | ✅ | 落雷 | cd **200 tick（10s）**；按人数点名 1/2/3；3 格内队友 → 伤害翻倍（12/6）；**r63/r71**：`LIGHTNING_MARK` 粒子 scale 1.2→0.5（缩小 60% 修复"粒子过大"反馈） |
| E3 | ✅ | 积雨云 | cd **480 tick（24s）**，unlockedAt 1；**r63/r64**：GeckoLib 模型（`rain_cloud.geo.json` + `rain_cloud.png`，scale=0.04）；恒定离地 5 格悬停 + 缓慢追踪玩家；8 格内每 20 tick 给玩家附加 1200 tick【潮湿】；**r64 跨阶段清理**：`beginTransition(oldPhase=3)` hook 离开云雨时全清 `RainCloudEntity` |
| E4 | ✅ | 雨阶段回血（终结） | 第 4 次传送后强制回血 **max HP × 15%** + 屏显"芙莉艾丝恢复了..."（由 `rainTeleports >= 4` 触发，传送阈值 = 0.2 × 当前血量比） |

### F. 天空阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| F1 | ✅ | 末影龙式轨道悬停 | 每 tick 推进圆周角 `tickCount * 0.04`，目标点 = 玩家 + (cos·7, 5, sin·7)，斜上方 45°环绕；速度 0.45；始终面朝玩家俯冲方向（yaw + pitch 都更新） |
| F2 | ✅ | 风刃 | cd **120 tick（6s）**，unlockedAt 1；随机方向双风刃（`WindBladeEntity`），6 点物理 + 击退 3 格；80 tick（4s）寿命 |
| F3 | ✅ | 盘旋冲击波 | cd **45 tick（2.25s）**；每 45 tick 对 16 格内玩家推力 0.9 + 击退 + 3 点物理伤害（仅触发方向的一次小冲击，非四向横扫） |

### G. 虚空阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| G1 | ✅ | 裂隙 | cd **40 tick（2s）**；密度减半（每 40 tick 单条）；2 格内吸引 + 传送到无裂隙的空地（半径 9~16 格）+ 10 点魔法伤害；落点花/草/紫水晶簇原地替换（r65） |
| G2 | ✅ | 虚空侵蚀 | 裂隙首次显形后（tickCount=21）在地面铺 1 块 VOID_EROSION 附层（**r64 修复"应该在地面附层"反馈**：用 `Heightmap.MOTION_BLOCKING_NO_LEAVES` 取地表列高，与生之焰同一路径）；踩踏每 20 tick 2 点魔法伤害 + 叠层（最高 20 层，每层 -1% 生命/攻击/移速，离开每秒 -1） |

### H. 枯萎阶段

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| H1 | ✅ | 死亡之花复活 | cd **80 tick（4s）**；每 80 tick 检查最近 60 tick（3 秒）内被击杀的花（slot + gameTime），若仍有花存活且数量 < 3 → 复活该 slot 的花；r66 修正：多花均分环绕间距、击杀后重新均分 |
| H2 | ✅ | 枯萎凋零氛围 | `tickCount % 20 == 0`：全屏 FX_GRAY 0.85×30（褪色）；`tickCount % 40 == 0 && anyFlowerAlive`：玩家脚下 2 颗 SOUL 粒子 |
| H3 | ✅ | 万花凋零 | **r63 实现 + r64 崩溃修复**。三花全灭 → 咏唱 **120 tick（6s）** 锁血 1HP + 黑洞实体 `MilleFlorumBlackHoleEntity` + `boss_finale.fsh` 后处理 shader pass + 屏幕文本"芙莉艾丝正在咏唱：万花凋零 / 距离强制灭杀还剩 N 秒"。最后一秒可打断：玩家击杀走 KILLED 路径；boss 在最后 1 秒（remaining ≤ 20 tick）停止咏唱 + 显示"芙莉艾丝真正认可了你" + 60 tick 收缩离场。**持解封丰饶/神环跳过（衔接 I1）。崩溃修复**：`die(this.damageSources().magic())`（1.20.1 没有 `genericKill()`/`outOfWorld()`）；`aiStep` 加 `isRemoved()`/`dead` 守卫避免 cast → exit → discard → 同一 tick 内重新启动的循环 |

### I. 额外阶段·时间（二周目全力战，phase 7）

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| I1 | ✅ | 触发条件 | 击败三花后检测「解封的丰饶」/`FRBState.holdingUnsealedBountiful(p)` 或「万花万草之灵」/`FRBState.hasUltimate(p)` → 跳过万花凋零，屏显"时间开始倒流" |
| I2 | ✅ | 阶段技能 ×4（r67） | 见下 |

**I2 四机制详情（r67 实现）：**

1. **预支生命**（cd **400 tick（20s）**）：吸取领域内所有玩家当前生命值的 **25%**，计算合计后全部作为回复量给予自身（吸血）。
   沙漏拖尾粒子从玩家汇聚到 boss + 回血爱心 + 提示「以汝之生命，贺花之永绽」。
   （原设计的"预支伤害"按用户要求取消。）

2. **时间停止**（cd 30s，持续 4s）：快照领域内全部可冻实体（生物/弹射物/掉落物）位置，
   每 tick 强制拽回快照点 + 清零速度——万物静止。
   豁免：boss 自身、机制实体（含死亡之花）、boss 自己的弹射物、创造/旁观玩家。
   **必接连招**：第 30 tick 从「自然光束 / 焚尽灼热之种 / 天空风刃」三选一释放——
   不重复轮换：第一次随机三选一 → 第二次从剩下两个随机 → 第三次必定最后一个未用机制
   → 三种全用后重置回随机三选一。
   时停内风刃瞄准玩家三连夹击；灼热之种 80 tick 引信（时停结束后正好要求走位拆弹）。

3. **时间倒流**（cd 12s）：领域内玩家回溯到 **3 秒前**的位置与视角（只回溯位置，不回溯生命/效果等状态）。
   位置历史：`TimeRewindTracker` 环形缓冲，每 5 tick 采样、保留 100 tick。
   传送门粒子双向演出 + 落地清除摔落伤害。

4. **旧技能轮换**（cd **200 tick（10s）**）：随机复用前阶段技能
   （自然光束/灼热之种/霜凝/积雨云/风刃），与四机制并行。
   时间阶段主题粒子 `TIME_PHASE_01~06` 6 张循环 + `TIME_TICK_RING` 刻度环（每 40 tick）。

### J. 杂项

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| J1 | ✅ | 发射位置多样化 | r68 花瓣光束读条演出：蓄力 24t 期间 boss 脚下缓缓展开花之摇篮法阵（75% 透明度 smoothstep 展开+缓旋），读条完法阵刚好展开 → 发射光束；自然荆棘（r64）改为从侧后 0.5 格发射；霜华冰刺同上 |
| J2 | ⚠️ | 贴图依赖 | 法阵/落花/风刃/积雨云/霜凝眼睛 ✅ 已接入；**r71 frost_eye.png 已复制到 `textures/particle/`（原 bug A 崩溃根因）** |

### K. 其他遗留

- ✅ 回主世界传送门结构（r68）：用户结构方块保存 `garden_gate.nbt`（frb 命名空间）；首次进入永恒花园时在落点北侧摆放一次（DimensionDataStorage 标记，已有则不生成），替换旧"落点悬空单门"；旧档残留单门自动清除；结构文件缺失时回退单门。世界之花改摆东侧「结构半宽+4（最小 8）」格，避免被结构覆盖
- ✅ B2 荆棘共生（见上）
- ✅ J1 花瓣光束发射演出（读条法阵展开）
- ✅ B3/D2 末影龙式荆棘/冰刺飞弹（r64）
- ✅ C3 生之焰附层排除花草/紫水晶簇（r64）
- ✅ D3 霜华随机法阵 16~24 格 + 高度图取地表（r64）
- ✅ E2 落雷粒子缩小 60%（r71）
- ✅ E3 积雨云跨阶段清理（r64）
- ✅ F1 天空末影龙环绕（r64）
- ✅ G2 虚空侵蚀地表附层（r64）
- ✅ H2 万花凋零 6 秒咏唱 + 屏幕文本 + 黑洞 shader（r63）
- ✅ H2 崩溃修复 `damageSources().magic()` + `isRemoved()` 守卫（r64）
- ✅ I1 二周目时间阶段判定（r63）
- ✅ I2 时间阶段四机制（r67）

### L. 待办（r68 新增，r71 扩充）

| 编号 | 状态 | 机制 | 说明 |
|---|---|---|---|
| L1 | ❌ | 阶段视觉区分 | 七阶段 + 时间阶段给 boss 加可辨识的视觉区分，方案可组合：① 阶段主题色——发光层/法阵/粒子主色随 phase 切换（自然绿 / 焚尽橙 / 霜华浅蓝 / 云雨青 / 天空白 / 虚空紫 / 枯萎暗紫 / 时间金）；② 模型配件——神环背环颜色、眼睛颜色、头顶花饰随阶段变化；③ 转场特效——进入新阶段瞬间主题色闪光 + 独立音效提示；④ 名牌/血条颜色随阶段变化。🎨 具体配色与配件方案待用户确认后实施 |
| L2 | ❌ | 场地特效：方案 D「星云天幕 + 能量边界」 | **r71 已敲定方案 D**：① 上空主题粒子云（每阶段不同 — 自然飘散花瓣 / 焚尽火星 / 霜华飘雪 / 云雨阴雨 / 天空星河 / 虚空升腾粒子 / 枯萎灰烬 / 时间沙漏螺旋）；② 圆形竞技场能量边界（24 → 12 格半径随 phase 收缩，触碰 2 点花灵伤害 + 击退）；③ 转阶段瞬间触发 boss_finale 简化漩涡 0.3s + FX_FLASH + FX_TINT。实现要点：`BossFxPacket.FX_ARENA_BORDER=256` + `FX_AMBIENT_TINT=512`；新建 `ArenaBorderEntity extends MechanicEntity`；`arena ambient tint` 每阶段 boss.aiStep 每 10 tick 续 5 tick 寿命（strength 0.2）；预估 6-8 小时工作量 |
| L3 | ✅ r72 | 限伤代码硬化（反 getHealth/反 setHealth/反 kill） | 用户反馈：① 防住玩家通过 setHealth/getHealth/虚空伤害/data 修改/直接 kill 绕过锁血（参考启示录·亚波伦 + 梦幻终焉·生命妨害反 getHealth）；② 给 boss 加「动态减伤」——短时间内受击会逐步降低伤害（参考启示录·末日效果 + 星辰箭矢·凋零聚晶）；③ 自然阶段自然生命恢复频率 ×2；④ 仅当 `difficulty=peaceful` 时允许 boss 死亡逃离战斗。**r72 实施**：`kill()` 重写为空 + `setHealth` 仅接受合法转阶段路径 + `isInvulnerableTo` 拦截 BYPASSES_INVULNERABILITY + 60 tick 滑窗 >40 触发 ×0.6 减伤 + 自然回血 10→5 tick |
| L4 | ✅ r72 | 阶段「回到过去」效果（10% 无视伤害） | 时间阶段专属效果：受击时 10% 概率无视本次伤害；粒子回滚 + 0.5 tick 短暂无敌窗口；机制来源 — 梦幻终焉·终焉秩序维系者（user 提到），需要解析其代码后仿写。**r72 实施**：`timePhaseActive` 分支 + `random.nextFloat() < 0.10F` + REVERSE_PORTAL ×18 粒子（haze 视角，附魔光效）|
| L5 | ❌ | 时间静止参照梦幻终焉重写 | 解析梦幻终焉 mod 的 `TimeStopSpell` + `TimeStopSavedData` + `LevelMixin/ParticleManagerMixin` 架构，仿写实现：① per-dimension SavedData 标记时停状态（仅当前维度）；② 时停期间暂停粒子更新与附魔光效渲染（梦幻终焉的关键 Mixin 效果）；③ 保留现有 48 格领域扫描 + 实体冻结拉回快照；④ 加 client-side 粒子暂停钩子。**前置**：需在 `build.gradle` 声明 Mixin 插件 + 创建 `mixins.json`（当前未声明 Mixin） |
| L6 | ✅ r72 解析 | 解析梦幻终焉·终焉秩序维系者 boss | 解析该 mod 的 boss 完整代码（包括 phase 切换、机制、特效、shader、shader）作为后续二周目 boss 设计参考。仅解析，不做实施。**r72 已写入 M.1 节**：UomWither 双阶段 + 终结技三态 + TimeStopSkillGoal + 反 getHealth 旁路（special_getHealth 走 SynchedEntityData）+ 经验归零（setSkipDropExperience）+ 死亡回溯坐标（UomDeathPosX/Y/Z）+ 全套网络包。**L5 实施时直接对照 M.1/M.2 节** |
| L7 | ✅ r72 解包 | 解包 boss 的两个娘化模型 | 用户在 mod参考 中放入两个娘化模型文件，需要解包为可用的 .bbmodel / .json 资源供后续替换 boss 视觉。仅解包，不做替换。**r72 已写入 M.3 节**：实际解包到 `L7_unpacked_models/`，发现用户提供的资源**只是贴图**（apollyon.png + halo_the_end.png 等共 4 张 PNG），不是 3D 模型文件。完整 3D 模型需从对应 mod jar 内提取 GeckoLib geo.json + 动画 + 贴图三件套 |
| L8 | ✅ r72 | 模组配置：分阶段调整 CD/伤害 | 配置架构改为「boss 配置 → 8 个阶段选项卡（自然/焚尽/霜华/云雨/天空/虚空/枯萎/时间）→ 进入选项卡后展开当前阶段所有机制调整」；CD 上下限 ±50%；伤害倍率 10%~1000%。**r72 实施**：`FRBConfig.java` 重写 24 个 cd × 24 个 dmg 倍率 + 8 个 phase push/pop + 工具方法 scaleCd/scaleDmg + 14 个 dmgXxx getter 已 wire-up 到 10 个伤害点（FriliasEntity ×8 / SpikeVolleyEntity ×2 / Mechanics ×2） |
| L9 | ✅ r72 | 世界之花意志消耗 | 当前世界之花意志（unsealing 花灵饰品）在 `BountifulRites.unseal()` 中不消耗；改为 unsealing 丰饶饰品时被消耗 1 个；服务器端检测保证多人只消耗一次。**r72 实施**：`RitualManager.finishUnseal` 按主手→副手→背包顺序扫描 + 服务端原子 `shrink(1)` + lang `frb.unseal.need_will` 中英文双补 |
| L10 | ✅ r72 | JEI 自动识别配方 | 写一个通用 JEI 适配模板，自动扫描 mod 注册的所有 Recipe<?> 注册到 JEI（避免手写每个配方）；允许 kubejs 用户通过 `RecipeEventJS`/`CustomRecipeJS` 添加自定义配方被自动识别。**r72 实施**：`FRBRecipeRegistry.java` 中央注册表 + `RitualLike/GrowthLike` 接口 + `CopyOnWriteArrayList` 线程安全 + `RitualJeiRecipe implements RitualLike` / `GrowthJeiRecipe implements GrowthLike`（让 builtin 既能入注册表也能直接 addRecipes）+ `FRBJeiPlugin.registerRecipes` 改为 `addRecipes(FRBRecipeTypes.RITUAL, FRBRecipeRegistry.getRituals())` |

---

## 三、执行注意事项（交接备忘）

1. **构建**：`$env:GRADLE_USER_HOME="D:\新建文件夹 (3)\gradle-home"; .\gradlew build --console=plain`，
   关沙盒运行（前台 10 分钟超时内可完成；后台任务在会话切换时可能丢失）。
2. **发布**：`Remove-Item` 旧 jar + `Copy-Item` 新 jar（关沙盒）可直接写入
   `D:\mc\.minecraft\versions\1.20.1-Forge_47.4.10\mods\`（r68 实测成功，旧"沙盒硬拦"已失效）。
3. **必须完整 `gradlew build`**（不能只 compileJava），否则用户实测全是旧行为。
4. 含中文的文件一律用 Write/Edit 工具，禁止 PowerShell 写文件（乱码）。
5. lang 格式串只用 `%s` / `%1$s` / `%%`；**Minecraft MobEffect 显示名 lang key 必须用 `effect.<mod_namespace>.<effect_name>`**（r71 修复：原 `effect.frb.*` 全部改为 `effect.floral_rite_bloom_curios.*`）。
6. FX 体系：`BossFxPacket` 位标志（SHAKE|FLASH|SHOCKWAVE|GRAY|TINT|MARKER|CIRCLE|FINALE=128）+ `FRBBossFxClient` shader。
7. boss 死亡解封走 `FriliasEntity.remove()`（KILLED→releaseFight(true)），改 remove/die 别破坏。
8. 机制实体必须免疫一切效果（A5 老坑）。**注意：`extends Projectile` 的子类（SpikeVolleyEntity/WindBladeEntity/PetalVolleyEntity）必须显式重写 `hurt()`+`isInvulnerable()`+`isInvulnerableTo()`**（r71 修复 PetalVolleyEntity 漏写 → 红闪特效）。
9. boss 访问方块/实体前检查区块加载（`level.isLoaded`/`areaLoaded`）。
10. 锁血/转阶段在 `setHealth` override + `hurt()`，加阈值检查别干扰锁血分支。
11. **粒子 SpriteSet 必须从 `textures/particle/` 目录读取**（r71 修复 Bug A：`textures/entity/` 下的 frost_eye.png 不被 SpriteSet 索引 → NPE）。
12. **粒子爆炸避免直接调 `Level.explode()`**（r71 修复 Bug B：内部 ray-trace 大量粒子导致单 tick 卡顿）—— 改用 `sl.sendParticles(EXPLOSION_EMITTER/EXPLOSION/POOF/FLAME)` + `playSound(GENERIC_EXPLODE)` + `victim.hurt(damageSources().explosion(...))` 手动广播。
13. **多人机制判定改为开场一次扫描**：用 `introIsMultiplayer` 字段在首次 aiStep 锁定，取代每次 cd 都 `fightersInRange` 扫描（r64 修复"偶发卡顿"反馈）。
14. **Minecraft 1.20.1 DamageSources 没有 `genericKill()` / `outOfWorld()`**：boss 自杀/终局死亡用 `damageSources().magic()`（r64 修复 Bug 8 崩溃）。

---

## M、第三方 mod 参考解析（r72 起新增章节）

> 仅解析，不做实施。供二周目 boss 设计、限伤方案对照、视觉/特效参考使用。

### M.1 梦幻终焉·终焉秩序维系者 boss（`UomWither.class`，57700 字节）

**类继承**：继承 `BaseEntity extends Mob`（典型 Wither 体型）；注册 ID = `entity.fantasy_ending.ultimate_order_manager`。

**阶段系统**（双阶段 + 终结技三态）：

| 状态 | 字段 | 行为 |
|---|---|---|
| 第一阶段 | `isSecondPhase=false` | 普攻 + 时间静止 + 弹幕（`MobEffects.weakness/summonEffect`） |
| 转场动画 | `phaseSwapAnim` / `phaseChangeTime` | `HENSHIN` GeckoLib 动画过渡 |
| 第二阶段 | `isSecondPhase=true` | 终结技 `finalSkillAttack` + `finalSkillComing` |
| 终结 | `ENABLE_FINAL_SKILL` + `trulyKill()` | `causeDeathDsDamage` 真实伤害源 + `finallyDeathTime` |

**时间静止**（参考我们 L5 的实现目标）：
- 字段 `timestopGoalTime`（cd）+ `setTsGoalTime`（写）
- 行为类 `TimeStopSkillGoal`（`com.mega.uom.common.entity.goals.TimeStopSkillGoal`）
- 接口 `com.mega.endinglib.api.entity.TimeStopEntity`（标记 boss 进入时停状态）
- 期间调用 `setSkipDropExperience()`（`AccessorLivingEntity`）— 经验归零
- 真实伤害源 `FE_SOURCE` / `DS_SOURCE`（`ModDamageSources`）

**抗性护甲**（对照启示录·亚波伦的限伤）：

```java
// ① peaceful 检查（仅和平模式可被杀死）
if (Difficulty.PEACEFUL == level.getDifficulty()) return false;
// ② 反 getHealth：special_getHealth 走 LivingEntityAccessor.HEALTH 直接读 SynchedEntityData
//    → 不走 getHealth() 方法的 ASM hook
// ③ 致命来源检查：instanceof DamageTypes.BYPASSES_INVULNERABILITY 直接拒绝
// ④ damageSources().magic() 兜底（与 r64 我们的 magic 修复同源）
// ⑤ SafeClass.isApollyon() — 亚波伦直系 boss 检测 → 走 boss 自己的 actuallyHurt 而非父类
```

**网络包同步**（独立通道）：
- `UomPhaseChangePacket`（阶段切换）
- `UomDodgeParticlePacket(IFF)`（闪避粒子）
- `UomRadialFlashRenderPacket`（径向闪光）
- `DreamShadowBeamPacket(Vector3f, Vector3f, DDI)`（光束）
- `WaveCustomRendererPacket` / `StarOfDavidRendererPacket`（视觉装饰）
- `AwayParticleRendererPacket(DDDDDDDI)`（远离粒子）
- `RandomTextDancingRendererPacket`（随机文字跳舞）
- `FantasyZeroFlashRenderer.disable/enable`（shader 闪烁）

**BossEvent**：`UUIDServerBossEvent`，`PURPLE` + `PROGRESS`（紫色进度条），多 boss 同步。

**死亡机制**（关键）：
- 死亡时记录位置 `UomDeathPosX/Y/Z`（死亡回溯用）
- `setRemoved0(reason)` 重写：原版 `Entity.setRemoved0` 在栈轨迹出现 `com.mega.uom` 时调 `mixinCancel()` → 不进真正死逻辑
- 反 kill：被 `forceKill` / `softKill` / `softGetHealthZero` 等 mod 操控时回滚血量

**启示**（对我们 r72 的价值）：
1. `Difficulty.PEACEFUL` 是逃脱点 — 我们的 r72 已实现
2. `special_getHealth` 走 SynchedEntityData 而非 getHealth() — 反 ASM hook 的标准做法
3. `AccessorLivingEntity.setSkipDropExperience()` — 反经验吸取
4. `mixinCancel()` 在 setRemoved 重写里拦截栈轨迹 — 反被操控 kill 的进阶做法

### M.2 梦幻终焉·时间静止架构（已存在 `mod参考/_disabled/fantasy_ending-1.20.1-2.7.20-all.jar`）

**核心组件**（从 jar 类列表摘出）：
- `com.mega.endinglib.api.entity.TimeStopEntity` — 接口标记
- `com.mega.endinglib.api.TimeStopHelper` — 静态助手（推断）
- `com.mega.endinglib.coremod.forge.LaunchPluginServiceBuilder` — coremod 服务构建
- `com.mega.endinglib.coremod.forge.IClassProcessor` — Mixin 类处理器
- `com.mega.endinglib.util.mixin.ApplyCheckMixinConfigPlugin` — **缺失**，导致 jar 不可加载
- `com.mega.endinglib.util.entity.armor.ArmorUtils` — 缺失，同样原因
- `com.mega.endinglib.mixin.accessor.AccessorLivingEntity` — `setSkipDropExperience` 方法

**Mixin 钩子**：
- `com.mega.uom.mixin.time.LevelMixin` — `Level.tick()` 钩子（暂停实体更新）
- `com.mega.uom.mixin.time.ClientLevelMixin` — 客户端 Level 钩子（暂停粒子）
- `com.mega.uom.mixin.time.ServerLevelMixin` — 服务端 Level 钩子（暂停实体）
- `com.mega.uom.mixin.time.ServerMixin` — 服务器钩子
- `com.mega.uom.mixin.time.ParticleManagerMixin` — 粒子管理器钩子
- `com.mega.uom.mixin.time.EmptyGlyphAccessor` — 附魔光效钩子（暂停字符动画）
- `com.mega.uom.mixin.LivingEntityAccessor` / `LivingEntityMixin` — 实体血量 + `special_getHealth` 旁路
- `com.mega.uom.mixin.MobEffectInstanceAccessor` — 效果持续时间旁路
- `com.mega.uom.mixin.MinecraftMixin` — 主线程 tick 钩子

**SaveData**：
- `com.mega.uom.common.data.level.TimeStopSavedData` — per-dimension 时停状态
- `com.mega.uom.common.data.level.CompleteSaveData` — 总 SavedData 集合

**启示**（L5 我们要仿写的最小集合）：
- ✅ ① per-dimension SavedData：标记当前维度是否处于时停（仅当前维度生效）
- ✅ ② `LevelMixin`（服务端）跳过非玩家实体的 `tick()` / `animateTick()`
- ✅ ③ `ClientLevelMixin`（客户端）跳过粒子 tick
- ✅ ④ `ParticleManagerMixin` 暂停粒子发射
- ✅ ⑤ `EmptyGlyphAccessor` 暂停附魔光效字符动画（可选，我们只需要最简单的子集）
- ⚠️ ⑥ 还需要 `LivingEntityMixin` 注入玩家视觉冻结（位移拉回快照）— 我们 r67 已实现，可复用

### M.3 GoetyRevelation boss 美术资源（已解包到 `L7_unpacked_models/`）

**两个 zip 资源包**（来自 `D:\mc\.minecraft\versions\1.20.1-Forge_47.4.10\resourcepacks\`）：

#### `GoetyRevelation art style.zip`
- 仅 **1 个 boss 关键贴图**：`assets/revelationfix/textures/entity/player/halo_the_end.png`（4 KB）— 启示录·终末之环 boss 头部光环
- 其他大量 `assets/goety/textures/item/*`（22 个）都是物品图标，与 boss 视觉无关

#### `GR Old Textures.zip`
- `assets/goety_revelation/textures/entity/apollyon/apollyon.png`（5.5 KB）— 亚波伦 boss 主体
- `assets/goety_revelation/textures/entity/apollyon/apollyon_second.png`（5.7 KB）— 亚波伦第二阶段
- `assets/goety_revelation/textures/entity/player/halo.png` + `halo_second.png` + `broken_halo.png`（776 字节）— 玩家头上光环
- `assets/goety_revelation/textures/item/halo.png` + `broken_halo.png` + `revelation.png` — 物品图标

**重要发现**：用户提到的"两个娘化模型"实际只是 PNG 贴图，**不是** `.geo.json` / `.obj` / 3D 模型文件。无法直接替换 FriliasEntity 的 3D 渲染。要真正替换 boss 模型，需要从对应 mod jar 内提取 GeckoLib 模型（如 `assets/fantasy_ending/geo/entity/uom_chan.geo.json` 246 KB）+ 动画 + 贴图，三件套齐全才可换装。

**已构建清单**（L7 实施步骤，未替换 boss）：
- 提取：`L7_unpacked_models/goety_revelation_art_style/` + `L7_unpacked_models/gr_old_textures/`
- 真实可用的 boss 贴图：`apollyon.png` + `apollyon_second.png` + `halo_the_end.png`
- 用途：可作为芙莉芙芙视觉变体的色彩参考（紫黑主体 + 金/银光环），但需先建模/雕刻才能套用

---

## 四、版本演进历史

| 版本 | 主要交付 |
|---|---|
| r59-r60 | 早期阶段机制 |
| r61 | 七阶段轮回 + 永封印/解封 + 成就系统 |
| r62 | 11 个新机制（花之摇篮/灼热之种/烈焰击退/霜凝/雨之华/落雷/积雨云/风刃/裂隙/死亡之花复活）+ 4 个新效果（雨之华/潮湿/虚空侵蚀/花灵之助）+ 8 个新粒子（用户贴图接入） |
| r63 | H2 万花凋零终结技（120 tick 咏唱 + 黑洞 shader）+ I1 额外阶段·时间 + 末影龙式抬手姿势 + 6 张时间阶段粒子（time_phase_01~06/time_hourglass/clock_face/clock_hands/tick_ring）+ frost_eye 替换 SOUL_FIRE_FLAME |
| r64 | 10 项修复：维度超平坦 + amethyst 入噪声列表 + 荆棘/冰刺飞弹（SpikeVolleyEntity）+ 虚空侵蚀地表附层 + 霜凝特效增强 + 积雨云 GeckoLib + 落雷粒子缩小 + 天空末影龙环绕 + 万花凋零崩溃修复（die→magic + isRemoved/dead 守卫）+ 多人机制开场扫描 |
| r65-r66 | 荆棘共生 B2 + 读条演出 + 激光 6 锚点 |
| r67 | 时间阶段 I2 四机制（预支生命/时间停止/时间倒流/旧技能轮换） |
| r68 | A7 血雨场地特效 + J1 花瓣光束读条法阵展开 + 传送门结构修复 + B2 多人专属 |
| r69-r70 | 本地迭代（粒子减半 / vanilla 爆炸尝试 / 燃烧种无生之焰） |
| **r71** | **5 项 bug 修复**：崩溃（frost_eye.png 缺失） + 灼热之种卡顿（sl.explode→手动粒子） + PetalVolleyEntity 红闪（三重无敌覆盖） + FlowerCradleSigil 双贴图（简化为单静态 quad） + buff 显示名 lang key（effect.frb → effect.floral_rite_bloom_curios） |
| **r72** | **7 项任务一次落地**：① boss 限伤硬化（peaceful escape + kill/setHealth/BYPASSES_INVULNERABILITY 拦截 + 60 tick 滑窗 ×0.6 动态减伤 + 自然阶段回血 10→5 tick）；② 时间阶段「回到过去」10% 无视伤害（haze 视角 REVERSE_PORTAL ×18）；③ 分阶段配置（8 个 phase 选项卡 push/pop，cd 倍率 0.5×~2.0×，dmg 倍率 10%~1000%，dmgXxx 三层倍率 wire-up 接入 10 个伤害点）；④ 世界之花意志（unseal 丰饶时按主手→副手→背包原子性扣 1 个 + `frb.unseal.need_will` 中英文）；⑤ `FRBRecipeRegistry` 中央注册表 + JEI 自动识别（`RitualLike/GrowthLike` 接口 + CopyOnWriteArrayList + `RitualJeiRecipe/GrowthJeiRecipe implements RitualLike/GrowthLike` 直接注册）；⑥ 10 处 wire-up（FriliasEntity×8 / SpikeVolleyEntity×2 / Mechanics×2）；⑦ **启动失败 fix**：梦幻终焉 2.7.20-all jar 内缺失 `com.mega.endinglib.util.mixin.ApplyCheckMixinConfigPlugin` 类（发行 bug）导致 `FantasyEndingMixinPlugin.<clinit>` 失败 → `mixins.uom.json` 不可用 → ModLauncher 抛 RuntimeException，已将该 jar 移到 `mod参考/_disabled/`（我们自己的 r72 jar 正常加载） |

