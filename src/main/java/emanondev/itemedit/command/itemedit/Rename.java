package emanondev.itemedit.command.itemedit;

import emanondev.itemedit.ItemEdit;
import emanondev.itemedit.Util;
import emanondev.itemedit.command.ItemEditCommand;
import emanondev.itemedit.command.SubCmd;
import emanondev.itemedit.utility.CompleteUtility;
import emanondev.itemedit.utility.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Rename extends SubCmd {

    private final Map<UUID, String> copies = new HashMap<>();
    private int lengthLimit;

    public Rename(@NotNull ItemEditCommand cmd) {
        super("rename", cmd, true, true);
        this.lengthLimit = this.getPlugin().getConfig().getInt("blocked.rename-length-limit", 120);
    }

    @Override
    public void reload() {
        super.reload();
        this.lengthLimit = this.getPlugin().getConfig().getInt("blocked.rename-length-limit", 120);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        Player p = (Player) sender;
        ItemStack item = this.getItemInHand(p);
        if (!Util.isAllowedRenameItem(sender, item.getType())) {
            return;
        }

        ItemMeta itemMeta = ItemUtils.getMeta(item);
        if (args.length == 1) {
            itemMeta.setDisplayName(" ");
            itemMeta.setItemName(" ");
            item.setItemMeta(itemMeta);
            this.updateView(p);
            return;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("-clear")) {
            itemMeta.setDisplayName(null);
            itemMeta.setItemName(null);
            item.setItemMeta(itemMeta);
            //TODO feedback
            this.updateView(p);
            return;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("-copy")) {
            this.copies.put(p.getUniqueId(), itemMeta.getDisplayName());
            //TODO feedback
            this.updateView(p);
            return;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("-paste")) {
            itemMeta.setDisplayName(this.copies.get(p.getUniqueId()));
            itemMeta.setItemName(this.copies.get(p.getUniqueId()));
            item.setItemMeta(itemMeta);
            //TODO feedback
            this.updateView(p);
            return;
        }

        StringBuilder bname = new StringBuilder(args[1]);
        for (int i = 2; i < args.length; i++) {
            bname.append(" ").append(args[i]);
        }

        String name = Util.formatText(p, bname.toString(), this.getPermission());
        if (Util.hasBannedWords(p, name)) {
            //TODO feedback
            return;
        }

        if (!this.allowedLengthLimit(p, ChatColor.stripColor(name))) {
            Util.sendMessage(p, ItemEdit.get().getLanguageConfig(p).loadMessage("blocked-by-rename-length-limit",
                    "", null, true, "%limit%", String.valueOf(this.lengthLimit)));
            return;
        }

        itemMeta.setDisplayName(name);
        itemMeta.setItemName(name);
        item.setItemMeta(itemMeta);
        //TODO feedback
        this.updateView(p);
    }

    @Override
    public List<String> onComplete(@NotNull CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        if (args.length != 2) {
            return Collections.emptyList();
        }
        ItemStack item = this.getItemInHand((Player) sender);

        List<String> result = new ArrayList<>();

        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = ItemUtils.getMeta(item);
            if (meta.hasDisplayName()) {
                result.addAll(Arrays.asList(
                        meta.getDisplayName().replace('§', '&'),
                        "-clear", "-copy"
                ));
            }
        }

        result.add("-paste");

        return CompleteUtility.complete(args[1], result);
    }

    private boolean allowedLengthLimit(Player who, String text) {
        if (this.lengthLimit < 0 || who.hasPermission("itemedit.bypass.rename_length_limit")) {
            return true;
        }
        return text.length() <= this.lengthLimit;
    }
}
