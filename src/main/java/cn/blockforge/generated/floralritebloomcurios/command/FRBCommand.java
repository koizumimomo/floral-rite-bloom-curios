package cn.blockforge.generated.floralritebloomcurios.command;

import cn.blockforge.generated.floralritebloomcurios.abilities.AstralBlink;
import cn.blockforge.generated.floralritebloomcurios.abilities.TerminusWreath;
import cn.blockforge.generated.floralritebloomcurios.charm.CharmType;
import cn.blockforge.generated.floralritebloomcurios.entity.FriliasEntity;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBEntities;
import cn.blockforge.generated.floralritebloomcurios.registry.FRBItems;
import cn.blockforge.generated.floralritebloomcurios.util.FRBState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * /floralrite (frb) progress | ritual | giveall | wreath | blink | boss
 * 七花收集进度也可通过 Curios 自带界面查看；饰品死亡不掉落。
 */
public final class FRBCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(withChildren(Commands.literal("floralrite")));
        dispatcher.register(withChildren(Commands.literal("frb")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withChildren(
            LiteralArgumentBuilder<CommandSourceStack> root) {
        return root
                .then(Commands.literal("progress")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(FRBCommand::progress))
                        .executes(FRBCommand::progressSelf))
                .then(Commands.literal("giveall")
                        .requires(s -> s.hasPermission(2))
                        .executes(FRBCommand::giveAll))
                .then(Commands.literal("wreath")
                        .executes(FRBCommand::wreath))
                .then(Commands.literal("blink")
                        .executes(FRBCommand::blink))
                .then(Commands.literal("boss")
                        .requires(s -> s.hasPermission(2))
                        .executes(FRBCommand::summonBoss))
                .then(Commands.literal("ritual")
                        .then(Commands.argument("charm", StringArgumentType.word())
                                .executes(FRBCommand::showRitual)));
    }

    private static int progress(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        sendProgress(ctx, player);
        return 1;
    }

    private static int progressSelf(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            sendProgress(ctx, player);
        } else {
            ctx.getSource().sendFailure(Component.translatable("frb.cmd.player_only"));
        }
        return 1;
    }

    private static void sendProgress(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        int n = FRBState.collectedCount(player);
        StringBuilder sb = new StringBuilder();
        for (CharmType type : CharmType.ALL) {
            sb.append(FRBState.collected(player, type) ? "■" : "□").append(' ');
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("frb.cmd.progress",
                String.valueOf(n), Component.literal(sb.toString().trim())), false);
    }

    private static int giveAll(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("frb.cmd.player_only"));
            return 0;
        }
        for (CharmType type : CharmType.ALL) {
            ItemStack stack = new ItemStack(FRBItems.charmOf(type));
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            FRBState.markCollected(player, type);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("frb.cmd.given")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int wreath(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            TerminusWreath.activate(player);
        }
        return 1;
    }

    private static int blink(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            AstralBlink.blink(player);
        }
        return 1;
    }

    private static int summonBoss(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        var pos = ctx.getSource().getPosition();
        FriliasEntity boss = new FriliasEntity(FRBEntities.FRILIAS.get(), level);
        boss.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 60, 1.5D, 2.0D, 1.5D, 0.1D);
        ctx.getSource().sendSuccess(() -> Component.translatable("frb.cmd.boss"), true);
        return 1;
    }

    private static int showRitual(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "charm");
        for (CharmType type : CharmType.ALL) {
            if (type.id.equals(id)) {
                ctx.getSource().sendSuccess(() -> Component.translatable("frb.ritual.req." + type.id)
                        .withStyle(ChatFormatting.GRAY), false);
                return 1;
            }
        }
        ctx.getSource().sendFailure(Component.translatable("frb.cmd.unknown_charm", id));
        return 0;
    }

    private FRBCommand() {}
}
