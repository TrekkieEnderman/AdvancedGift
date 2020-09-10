package io.github.TrekkieEnderman.advancedgift.command;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandHandler implements TabExecutor {
    private final CommandGift gift;
    private final CommandGiftToggle giftToggle;
    private final CommandGiftBlock giftBlock;
    private final CommandAdmin admin;

    public CommandHandler(AdvancedGift plugin) {
        gift = new CommandGift(plugin);
        giftToggle = new CommandGiftToggle(plugin);
        giftBlock = new CommandGiftBlock(plugin);
        admin = new CommandAdmin(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (sender instanceof Player) {
            switch (cmd) {
                case "gift":
                    gift.onCommand(sender, args);
                case "giftblock":
                case "giftblocklist":
                    giftBlock.onCommand(sender, command, label, args);
                case "togglegift":
                    giftToggle.onCommand(sender, args);
                case "giftspy":
                    admin.onCommand(sender, command, args);
            }
        }
        switch (cmd) {
            case "agreload":
                admin.onCommand(sender, command, args);
            default:
                sendNotConsoleCommandMessage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        //TODO
        return null;
    }

    private void sendNotConsoleCommandMessage (CommandSender console) {
        console.sendMessage("This command can only be run by a player.");
    }
}
