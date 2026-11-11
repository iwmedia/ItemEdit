package emanondev.itemedit.command.itemedit;

import emanondev.itemedit.Util;
import emanondev.itemedit.command.ItemEditCommand;
import emanondev.itemedit.command.SubCmd;
import emanondev.itemedit.utility.CompleteUtility;
import emanondev.itemedit.utility.ItemUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Copy extends SubCmd {

    public Copy(ItemEditCommand cmd) {
        super("copy", cmd, true, true);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        Player p = (Player) sender;
        ItemStack item = this.getItemInHand(p);
        Rename rename = this.getCommand().getSubCmd(Rename.class);
        Lore lore = this.getCommand().getSubCmd(Lore.class);
        Enchant enchant = this.getCommand().getSubCmd(Enchant.class);
        if (args.length != 2 || !args[1].equalsIgnoreCase("all")
                || rename == null || lore == null || enchant == null) {
            this.onFail(p, alias);
            return;
        }

        ItemMeta meta = ItemUtils.getMeta(item);
        if (!meta.hasDisplayName() && !meta.hasLore() && !meta.hasEnchants()) {
            Util.sendMessage(p, this.getLanguageString("nothing", null, p));
            return;
        }

        UUID uuid = p.getUniqueId();
        rename.getCopies().put(uuid, meta.getDisplayName());
        lore.getCopies().put(uuid, meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>());
        enchant.getCopies().put(uuid, item.getEnchantments());
        Util.sendMessage(p, this.getLanguageString("feedback", null, p));
    }

    @Override
    public List<String> onComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CompleteUtility.complete(args[1], Collections.singletonList("all"));
        }
        return Collections.emptyList();
    }
}
