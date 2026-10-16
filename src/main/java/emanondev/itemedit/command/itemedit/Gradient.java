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
            boolean colored = colors.get(start) != null;
            while (end + 1 < codePoints.size() && (colors.get(end + 1) != null) == colored) {
                end++;
            }

            if (!colored) {
                result.append(start > 0 ? "<reset>" : "");
                appendText(result, codePoints, start, end);
            } else {
                // the first three characters of any fitting run share one stop segment, ruling out longer runs early
                if (end - start >= 2 && !nearlyCollinear(colors.get(start), colors.get(start + 1), colors.get(start + 2))) {
                    end = start + 1;
                }
                // MiniMessage spreads the stops evenly over the whole tag, so a shorter run may fail where a longer one fits
                Color[] stops;
                while ((stops = fitStops(colors, start, end)) == null) {
                    end--;
                }
                if (stops.length == 1) {
                    result.append('<').append(toHex(stops[0])).append('>');
                    appendText(result, codePoints, start, end);
                } else {
                    result.append("<gradient");
                    for (Color stop : stops) {
                        result.append(':').append(toHex(stop));
                    }
                    result.append('>');
                    appendText(result, codePoints, start, end);
                    result.append("</gradient>");
                }
            }
            start = end + 1;
        }
        return result.toString();
    }

    private Color[] fitStops(List<Color> colors, int start, int end) {
        int length = end - start + 1;
        // at least two characters per stop segment, otherwise any sequence of distinct colors would pass as a gradient
        for (int count = 1; count <= (length - 1) / 2 + 1; count++) {
            Color[] stops = new Color[count];
            stops[0] = colors.get(start);
            if (fits(colors, start, length, stops)) {
                return stops;
            }
        }
        return null;
    }

    private boolean fits(List<Color> colors, int start, int length, Color[] stops) {
        for (int k = 0; k < length; k++) {
            double position = position(k, stops.length, length);
            int low = (int) Math.floor(position);
            int high = (int) Math.ceil(position);
            if (stops[high] == null) {
                // derived lazily from the last character before the stop, so a mismatch aborts early (runs on the main thread)
                int anchor = high * (length - 1) / (stops.length - 1);
                stops[high] = extrapolate(stops[low], colors.get(start + anchor), position(anchor, stops.length, length) - low);
            }
            double t = position - low;
            Color from = stops[low];
            Color to = stops[high];
            Color actual = colors.get(start + k);
            if (Math.abs(actual.getRed() - lerp(from.getRed(), to.getRed(), t)) > TOLERANCE
                    || Math.abs(actual.getGreen() - lerp(from.getGreen(), to.getGreen(), t)) > TOLERANCE
                    || Math.abs(actual.getBlue() - lerp(from.getBlue(), to.getBlue(), t)) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private boolean nearlyCollinear(Color a, Color b, Color c) {
        int limit = 1 + 3 * TOLERANCE;
        return Math.abs(a.getRed() - 2 * b.getRed() + c.getRed()) <= limit
                && Math.abs(a.getGreen() - 2 * b.getGreen() + c.getGreen()) <= limit
                && Math.abs(a.getBlue() - 2 * b.getBlue() + c.getBlue()) <= limit;
    }

    private double position(int index, int stops, int length) {
        return stops == 1 ? 0 : (double) index * (stops - 1) / (length - 1);
    }

    private Color extrapolate(Color from, Color through, double t) {
        return new Color(extrapolate(from.getRed(), through.getRed(), t),
                extrapolate(from.getGreen(), through.getGreen(), t),
                extrapolate(from.getBlue(), through.getBlue(), t));
    }

    private int extrapolate(int from, int through, double t) {
        return (int) Math.max(0, Math.min(255, Math.round(from + (through - from) / t)));
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
