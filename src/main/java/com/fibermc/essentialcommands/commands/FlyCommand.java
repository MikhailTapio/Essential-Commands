package com.fibermc.essentialcommands.commands;

import com.fibermc.essentialcommands.ECAbilitySources;
import com.fibermc.essentialcommands.ECText;
import com.fibermc.essentialcommands.access.ServerPlayerEntityAccess;
import com.fibermc.essentialcommands.util.TextUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ladysnake.pal.Pal;
import io.github.ladysnake.pal.VanillaAbilities;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

import static com.fibermc.essentialcommands.EssentialCommands.CONFIG;

public class FlyCommand implements Command<ServerCommandSource> {

    public FlyCommand() {
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity senderPlayer = source.getPlayer();

        ServerPlayerEntity targetPlayer;
        try {
            targetPlayer = EntityArgumentType.getPlayer(context, "target_player");
        } catch (IllegalArgumentException e) {
            targetPlayer = senderPlayer;
        }

        boolean permanent;
        try {
            permanent = BoolArgumentType.getBool(context, "permanent");
        } catch (IllegalArgumentException e) {
            permanent = false;
        }

        exec(source, targetPlayer, permanent);
        return 0;
    }

    public static void exec(ServerCommandSource source, ServerPlayerEntity target, boolean permanent) {
        PlayerAbilities playerAbilities = target.getAbilities();

        Consumer<PlayerAbilities> setAbilitiesOverride = (abilities) -> {
            abilities.allowFlying = !abilities.allowFlying;
            if (!abilities.allowFlying) {
                abilities.flying = false;
            }
            target.sendAbilitiesUpdate();
        };

        if (permanent) {
            setAbilitiesOverride.accept(playerAbilities);
        } else {
            try {
                boolean isFlightAllowed = VanillaAbilities.ALLOW_FLYING.getTracker(target).isGrantedBy(ECAbilitySources.FLY_COMMAND);
                if (isFlightAllowed) {
                    Pal.revokeAbility(target, VanillaAbilities.ALLOW_FLYING, ECAbilitySources.FLY_COMMAND);
                } else {
                    Pal.grantAbility(target, VanillaAbilities.ALLOW_FLYING, ECAbilitySources.FLY_COMMAND);
                }
                target.sendAbilitiesUpdate();
            } catch (NoClassDefFoundError ign) {
                setAbilitiesOverride.accept(playerAbilities);
            }
        }

        ((ServerPlayerEntityAccess) target).getEcPlayerData().updatePersistFlight(permanent);

        // Label boolean values in suggestions, or switch to single state value (present or it's not)

        source.sendFeedback(
            TextUtil.concat(
                ECText.getInstance().getText("cmd.fly.feedback.1").setStyle(CONFIG.FORMATTING_DEFAULT.getValue()),
                new LiteralText(playerAbilities.allowFlying ? "enabled" : "disabled").setStyle(CONFIG.FORMATTING_ACCENT.getValue()),
                ECText.getInstance().getText("cmd.fly.feedback.2").setStyle(CONFIG.FORMATTING_DEFAULT.getValue()),
                target.getDisplayName(),
                new LiteralText(".").setStyle(CONFIG.FORMATTING_DEFAULT.getValue())
            ),
            CONFIG.BROADCAST_TO_OPS.getValue()
        );
    }
}