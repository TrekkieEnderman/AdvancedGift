/*
 * Copyright (c) 2025-2026 TrekkieEnderman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift.commands.concrete;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.commands.SimpleCommand;
import io.github.TrekkieEnderman.advancedgift.gift.ItemContent;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.util.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandGift extends SimpleCommand {

    public CommandGift(AdvancedGift plugin) {
        super(plugin, "gift", "advancedgift.gift.send");
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage(Message.COMMAND_GIFT_DESCRIPTION.translatePrefixed());
        sender.sendMessage(Message.COMMAND_GIFT_USAGE.translate()); // TODO any good way to hide the last argument if messages are disabled?
    }

    @Override
    public boolean run(@NotNull final Player sender, @NotNull final String label, @NotNull final String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        // Get target
        Player target = null;
        final PlayerInventory senderInventory = sender.getInventory();
        List<Player> matchList = Bukkit.matchPlayer(args[0]);
        if (!sender.hasPermission("advancedgift.bypass.vanish")) {
            matchList = matchList.stream()
                    .filter(player -> !PlayerUtils.isVanished(player))
                    .collect(Collectors.toList());
        }

        if (matchList.size() == 1 && matchList.getFirst().equals(sender)) {
            sender.sendMessage(Message.SEND_GIFT_SELF.translatePrefixed());
            return false;
        }

        matchList = matchList.stream().filter(player -> !player.equals(sender)).collect(Collectors.toList());
        if (matchList.size() == 1) {
            target = matchList.getFirst();
        } else if (matchList.size() > 1) {
            sender.sendMessage(Message.MULTIPLE_TARGET_FOUND.translatePrefixed());
            final ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();

            HoverEvent<Component> hoverEvent = Message.TIP_CLICK_TO_SEND_GIFT.translate().asHoverEvent();
            final String[] argsClone = args.clone(); //we want to reuse the exact command the player used, and just change the target name
            boolean first = true;
            for (Player player : matchList) {
                argsClone[0] = player.getName(); //replace the original 1st argument with new name
                final String commandString = "/gift " + String.join(" ", argsClone);
                ClickEvent clickEvent = net.kyori.adventure.text.event.ClickEvent.suggestCommand(commandString);
                Component entry = player.displayName().hoverEvent(hoverEvent).clickEvent(clickEvent);

                if (!first) builder.appendSpace();
                first = false;
                builder.append(entry);
            }
            sender.sendMessage(builder);
            return true;
        }

        if (target == null) {
            sender.sendMessage(Message.TARGET_NOT_ONLINE.translatePrefixed(Component.text(args[0])));
            return false;
        }

        // Get ItemStack
        final ItemStack giftItem = senderInventory.getItemInMainHand().clone();
        if (giftItem.getType() == Material.AIR) {
            sender.sendMessage(Message.GIFT_EMPTY.translatePrefixed());
            return false;
        }
        if (plugin.isPainting(sender)) {
            sender.sendMessage(Message.GIFT_DENIED_GENERIC.translatePrefixed());
            return true;
        }
        if (plugin.isPainting(target)) {
            sender.sendMessage(Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translatePrefixed());
            return true;
        }

        if (args.length == 1) {
            sendItem(sender, target, giftItem, giftItem.getAmount(), null);
            return true;
        }

        // Get and validate gift amount
        int giftAmount = getGiftAmount(sender, giftItem, args[1]);
        if (giftAmount < 1) {
            sender.sendMessage(Message.INVALID_GIFT_AMOUNT.translatePrefixed());
            return false;
        }
        if (!senderInventory.containsAtLeast(giftItem, giftAmount)) {
            sender.sendMessage(Message.INSUFFICIENT_GIFT_AMOUNT.translatePrefixed());
            return false;
        }

        if (args.length == 2) {
            sendItem(sender, target, giftItem, giftAmount, null);
            return true;
        }
        if (!sender.hasPermission("advancedgift.gift.message")) {
            sender.sendMessage(Message.MESSAGE_REMOVED_NO_PERMISSION.translatePrefixed());
            sendItem(sender, target, giftItem, giftAmount, null);
            return true;
        }

        // Get message
        final String[] giftMessage = Arrays.copyOfRange(args, 2, args.length);
        sendItem(sender, target, giftItem, giftAmount, String.join(" ", giftMessage));
        return true;
    }

    private int getGiftAmount(final Player sender, final ItemStack itemStack, final String amountInput) {
        if (amountInput.equalsIgnoreCase("hand")) {
            return itemStack.getAmount();
        }
        if (amountInput.equalsIgnoreCase("all")) {
            return PlayerUtils.getTotalAmountHas(sender, itemStack);
        }
        return NumberUtils.toInt(amountInput);
    }

    private void sendItem(final Player sender, final Player target, final ItemStack itemStack, final int amount, final @Nullable String message) {
        final ItemContent gift = new ItemContent(itemStack, amount);
        sender.getInventory().removeItem(gift.getItemStack());
        plugin.getGiftManager().sendGift(sender, target, gift, message);
    }
}