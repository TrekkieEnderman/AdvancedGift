/*
 * Copyright (c) 2025 TrekkieEnderman
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift.locale;

import org.jetbrains.annotations.NotNull;

public enum Message {
    COMMAND_NO_PERMISSION("commandNoPermission"),
    COMMAND_FOR_PLAYER_ONLY("commandIsForPlayerOnly"),
    ARGUMENT_NOT_RECOGNIZED("argumentNotRecognized"),
    PLAYER_NOT_FOUND("playerNotFound"),
    CHECK_CONSOLE("checkServerConsole"),
    CONFIG_NOT_FOUND("configNotFound"),
    CONFIG_LOADED("configLoaded"),
    COMMAND_RELOAD_DESCRIPTION("reloadCommandDescription"),
    COMMAND_RELOAD_USAGE("reloadCommandUsage"),
    CONFIG_RELOADED("configReloaded"),
    CONFIG_NOT_RELOADED("configNotReloaded"),
    OUTDATED_CONFIG("configIsOutdated"),
    COMMAND_SPY_DESCRIPTION("giftSpyCommandDescription"),
    COMMAND_SPY_USAGE("giftSpyCommandUsage"),
    SPY_ENABLED("spyEnabled"),
    SPY_ALREADY_ENABLED("spyAlreadyEnabled"),
    SPY_DISABLED("spyDisabled"),
    SPY_ALREADY_DISABLED("spyAlreadyDisabled"),
    COMMAND_BLOCK_DESCRIPTION("giftBlockCommandDescription"),
    COMMAND_BLOCK_USAGE("giftBlockCommandUsage"),
    BLOCK_SELF("attemptedToBlockSelf"),
    BLOCK_OTHER("otherPlayerBlocked"),
    OTHER_BLOCKED_ALREADY("otherPlayerIsAlreadyBlocked"),
    COMMAND_UNBLOCK_DESCRIPTION("giftUnblockCommandDescription"),
    COMMAND_UNBLOCK_USAGE("giftUnblockCommandUsage"),
    UNBLOCK_OTHER("otherPlayerUnblocked"),
    OTHER_UNBLOCKED_ALREADY("otherPlayerIsAlreadyUnblocked"),
    BLOCK_LIST_DESCRIPTION("giftBlockListCommandDescription"),
    BLOCK_LIST_USAGE("giftBlockListCommandUsage"),
    BLOCK_LIST_SHOW("showBlockList"),
    BLOCK_LIST_EMPTY("blockListIsEmpty"),
    TIP_CLICK_TO_UNBLOCK("clickToUnblock"),
    BLOCK_LIST_CLEARED("giftBlockListCleared"),
    BLOCK_LIST_ALREADY_CLEARED("giftBlockListAlreadyCleared"),
    COMMAND_TOGGLE_DESCRIPTION("giftToggleCommandDescription"),
    COMMAND_TOGGLE_USAGE("giftToggleCommandUsage"),
    TOGGLED_OFF("giftToggledOff"),
    ALREADY_TOGGLED_OFF("giftAlreadyToggledOff"),
    TOGGLED_ON("giftToggledOn"),
    ALREADY_TOGGLED_ON("giftAlreadyToggledOn"),
    COMMAND_GIFT_DESCRIPTION("giftCommandDescription"),
    COMMAND_GIFT_USAGE("giftCommandUsage"),
    TARGET_NOT_ONLINE("targetIsNotOnline"),
    MULTIPLE_TARGET_FOUND("multipleTargetFound"),
    TIP_CLICK_TO_SEND_GIFT("clickToSendGift"),
    SEND_GIFT_SELF("attemptedToGiveSelf"),
    GIFT_EMPTY("attemptedToGiveAir"),
    INVALID_GIFT_AMOUNT("giftAmountIsInvalid"),
    INSUFFICIENT_GIFT_AMOUNT("giftAmountMoreThanAmountHave"),
    MESSAGE_REMOVED_NO_PERMISSION("messageRemovedNoPermission"),
    MESSAGE_REMOVED_INAPPROPRIATE("messageRemovedBadWordsProhibited"),
    GIFT_DENIED_INAPPROPRIATE_MESSAGE("giftDeniedBadWordsProhibited"),
    SENDER_IN_BLACKLISTED_WORLD("senderIsInBlacklistedWorld"),
    TARGET_IN_BLACKLISTED_WORLD("targetIsInBlacklistedWorld"),
    INTERWORLD_GIFT_PROHIBITED("interworldGiftIsProhibited"),
    GIFT_DENIED_GENERIC("giftDeniedGenericMessage"),
    TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY("targetCanNotReceiveGiftsCurrently"),
    TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY("targetIsNotAcceptingGiftsCurrently"),
    GIFT_COOLDOWN_NOT_OVER("giftCooldownHasNotEndedYet"),
    TARGET_INVENTORY_FULL("targetInventoryIsFull"),
    YOUR_INVENTORY_FULL("yourInventoryIsFull"),
    TARGET_INVENTORY_ALMOST_FULL("targetInventoryWasPartiallyFull"),
    YOUR_INVENTORY_ALMOST_FULL("yourInventoryWasPartiallyFull"),
    GIFT_SENT("giftSent"),
    MESSAGE_SENT("messageSent"),
    GIFT_RECEIVED("giftReceived"),
    MESSAGE_RECEIVED("messageReceived"),
    GIFT_LOGGED("giftLogged"),
    MESSAGE_LOGGED("messageLogged"),
    ENCHANTED_ITEM("enchantedItem"),
    PATTERNED_ITEM("patternedItem"),
    NAMED_ITEM("namedItem"),
    TRANSLATION_CREATED("translationCreated"),
    TRANSLATION_NOT_CREATED("translationNotCreated"),
    UNKNOWN_LOCALE("unknownLocale"),
    COMMAND_TRANSLATE_DESCRIPTION("translateCommandDescription"),
    COMMAND_TRANSLATE_USAGE("translateCommandUsage");


    private final String key;

    Message(final @NotNull String key) {
        this.key = key;
    }

    public String translate(Object... objects) {
        return Translation.translate(Translation.getServerLocale(), key, objects);
    }
}
