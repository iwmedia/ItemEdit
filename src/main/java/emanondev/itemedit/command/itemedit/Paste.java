package emanondev.itemedit.command.itemedit;

import emanondev.itemedit.Util;
import emanondev.itemedit.command.ItemEditCommand;
import emanondev.itemedit.command.SubCmd;
import emanondev.itemedit.utility.ItemUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Paste extends SubCmd {

    public Paste(ItemEditCommand cmd) {
        super("paste", cmd, true, true);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        Player p = (Player) sender;
        ItemStack item = this.getItemInHand(p);
        Rename rename = this.getCommand().getSubCmd(Rename.class);
        Lore lore = this.getCommand().getSubCmd(Lore.class);
        Enchant enchant = this.getCommand().getSubCmd(Enchant.class);
        if (args.length != 1 || rename == null || lore == null || enchant == null) {
            this.onFail(p, alias);
            return;
        }

        UUID uuid = p.getUniqueId();
        String copiedName = rename.getCopies().get(uuid);
        List<String> copiedLore = lore.getCopies().get(uuid);
        Map<Enchantment, Integer> copiedEnchants = enchant.getCopies().get(uuid);
        boolean hasName = copiedName != null && !copiedName.isEmpty();
        boolean hasLore = copiedLore != null && !copiedLore.isEmpty();
        boolean hasEnchants = copiedEnchants != null && !copiedEnchants.isEmpty();
        if (!hasName && !hasLore && !hasEnchants) {
            Util.sendMessage(p, this.getLanguageString("no-copy", null, p));
            return;
        }
        if (hasName && !Util.isAllowedRenameItem(p, item.getType())) {
            return;
        }
        if (hasLore && !Util.isAllowedChangeLore(p, item.getType())) {
            return;
        }

        ItemMeta meta = ItemUtils.getMeta(item);
        if (hasName) {
            meta.setDisplayName(copiedName);
            meta.setItemName(copiedName);
        }
        if (hasLore) {
            meta.setLore(copiedLore);
        }
        if (hasEnchants) {
            // applied on the meta, item.addUnsafeEnchantments would be overwritten by the following setItemMeta
            copiedEnchants.forEach((ench, lv) -> meta.addEnchant(ench, lv, true));
        }
        item.setItemMeta(meta);
        Util.sendMessage(p, this.getLanguageString("feedback", null, p));
        this.updateView(p);
    }

    @Override
    public List<String> onComplete(@NotNull CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
