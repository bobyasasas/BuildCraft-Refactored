package buildcraft.lib.command;

import buildcraft.api.core.BCLog;
import buildcraft.lib.BCLib;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.VersionChecker.CheckResult;
import net.minecraftforge.fml.VersionChecker.Status;
import net.minecraftforge.versions.mcp.MCPVersion;

//public class CommandVersion extends CommandBase
public class CommandVersion extends BCSubCommandBase {
    CommandVersion() {
        super(
                "version",
                "command.buildcraft.buildcraft.version.help",
                0,
                (arg) ->
                {
                    Entity sender = arg.getSource().getEntity();
                    if (sender == null) {
                        return 0;
                    }
                    CheckResult result = VersionChecker.getResult(BCLib.MOD_CONTAINER.getModInfo());
                    if (result.status() == Status.FAILED) {
                        sender.sendSystemMessage(Component.translatable("command.buildcraft.version.failed"));
                        return 0;
                    }

                    Style style = Style.EMPTY;
                    if (result.status() == Status.OUTDATED) {
                        style.withColor(ChatFormatting.RED);
                    } else {
                        style.withColor(ChatFormatting.GREEN);
                    }

                    BCLog.logger.info("[lib.command.version] Result status = " + result.status());
                    BCLog.logger.info("[lib.command.version] Result url = " + result.url());
                    BCLog.logger.info("[lib.command.version] Result target = " + result.target());
                    BCLog.logger.info("[lib.command.version] Result changes = " + result.changes());

                    String currentVersion = BCLib.VERSION;
                    if (currentVersion.startsWith("$")) {
                        currentVersion = "?.??.??";
                        style.withColor(ChatFormatting.GRAY);
                    }

                    Object[] textArgs = { currentVersion, MCPVersion.getMCVersion(), result.target() };
                    sender.sendSystemMessage(Component.translatable("command.buildcraft.version", textArgs).setStyle(style));

                    if (currentVersion.contains("-pre")) {
                        sender.sendSystemMessage(Component.translatable("command.buildcraft.version.prerelease"));
                    }
                    return 0;
                }
        );
    }
}
