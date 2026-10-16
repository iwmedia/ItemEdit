package emanondev.itemedit.command.itemedit;

import emanondev.itemedit.Util;
import emanondev.itemedit.command.ItemEditCommand;
import emanondev.itemedit.command.SubCmd;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Gradient extends SubCmd {

    private static final int TOLERANCE = 2;

    public Gradient(ItemEditCommand cmd) {
        super("gradient", cmd, true, true);
    }

    @Override
    public void onCommand(CommandSender sender, String alias, String[] args) {
        Player player = (Player) sender;
        ItemMeta meta = this.getItemInHand(player).getItemMeta();
        if (meta == null || (!meta.hasDisplayName() && !meta.hasLore())) {
            Util.sendMessage(player, this.getLanguageString("no-text", null, sender));
            return;
        }

        String hover = this.getLanguageString("hover", null, sender);
        if (meta.hasDisplayName()) {
            sendLine(player, this.getLanguageString("name-prefix", null, sender), toMiniMessage(meta.getDisplayName()), hover);
        }
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (int i = 0; i < lore.size(); i++) {
                sendLine(player, this.getLanguageString("lore-prefix", null, sender, "%line%", String.valueOf(i + 1)), toMiniMessage(lore.get(i)), hover);
            }
        }
    }

    private void sendLine(Player player, String prefix, String miniMessage, String hover) {
        Util.sendMessage(player, new ComponentBuilder(prefix + miniMessage)
                .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, miniMessage))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()))
                .create());
    }

    private String toMiniMessage(String legacy) {
        List<Integer> codePoints = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        Color color = null;
        for (int index = 0; index < legacy.length(); index++) {
            char curChar = legacy.charAt(index);
            if (curChar == ChatColor.COLOR_CHAR && index + 1 < legacy.length()) {
                char code = Character.toLowerCase(legacy.charAt(index + 1));
                if (code == 'x' && index + 13 < legacy.length()) {
                    // Spigot serializes RGB as §x§R§R§G§G§B§B
                    StringBuilder hex = new StringBuilder("#");
                    for (int j = index + 3; j <= index + 13; j += 2) {
                        hex.append(legacy.charAt(j));
                    }
                    color = ChatColor.of(hex.toString()).getColor();
                    index += 13;
                    continue;
                }
                ChatColor chatColor = ChatColor.getByChar(code);
                if (chatColor == ChatColor.RESET) {
                    color = null;
                } else if (chatColor != null && chatColor.getColor() != null) {
                    color = chatColor.getColor();
                }
                index++;
                continue;
            }
            int codePoint = legacy.codePointAt(index);
            index += Character.charCount(codePoint) - 1;
            codePoints.add(codePoint);
            colors.add(color);
        }

        StringBuilder result = new StringBuilder();
        int start = 0;
        while (start < codePoints.size()) {
            int end = start;
            while (end + 1 < codePoints.size() && fits(colors, start, end + 1)) {
                end++;
            }
            Color from = colors.get(start);
            Color to = colors.get(end);
            if (end - start == 1 && !Objects.equals(from, to)) {
                end = start;
                to = from;
            }

            if (from == null) {
                result.append(start > 0 ? "<reset>" : "");
                appendText(result, codePoints, start, end);
            } else if (from.equals(to)) {
                result.append('<').append(toHex(from)).append('>');
                appendText(result, codePoints, start, end);
            } else {
                result.append("<gradient:").append(toHex(from)).append(':').append(toHex(to)).append('>');
                appendText(result, codePoints, start, end);
                result.append("</gradient>");
            }
            start = end + 1;
        }
        return result.toString();
    }

    private boolean fits(List<Color> colors, int start, int end) {
        Color from = colors.get(start);
        Color to = colors.get(end);
        if (from == null || to == null) {
            for (int index = start; index <= end; index++) {
                if (colors.get(index) != null) {
                    return false;
                }
            }
            return true;
        }
        for (int k = start + 1; k < end; k++) {
            Color actual = colors.get(k);
            if (actual == null) {
                return false;
            }
            double t = (double) (k - start) / (end - start);
            if (Math.abs(actual.getRed() - lerp(from.getRed(), to.getRed(), t)) > TOLERANCE
                    || Math.abs(actual.getGreen() - lerp(from.getGreen(), to.getGreen(), t)) > TOLERANCE
                    || Math.abs(actual.getBlue() - lerp(from.getBlue(), to.getBlue(), t)) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private long lerp(int from, int to, double t) {
        return Math.round(from + t * (to - from));
    }

    private void appendText(StringBuilder result, List<Integer> codePoints, int start, int end) {
        for (int k = start; k <= end; k++) {
            int codePoint = codePoints.get(k);
            if (codePoint == '<' || codePoint == '\\') {
                result.append('\\');
            }
            result.appendCodePoint(codePoint);
        }
    }

    private String toHex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }

    @Override
    public List<String> onComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
