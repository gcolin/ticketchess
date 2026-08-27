package com.github.gcolin.platform;

import com.github.gcolin.player.IPlayer;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashSet;
import java.util.Set;
import com.github.gcolin.event.Event;
import com.github.gcolin.player.Player;

public class ServiceUtils {

    private static Set<String> freeForTitle = new HashSet<>();

    static {
        freeForTitle.add("g");
        freeForTitle.add("m");
        freeForTitle.add("mf");
    }

    public static long calculatePrice(IPlayer player, com.github.gcolin.event.Event event) {
        if (player == null) {
            throw new WebApplicationException("Player not found", 400);
        }
        String title = player.getFideTitre();
        if (title != null && freeForTitle.contains(title)) {
            return 0L;
        }
        long priceCents;
        if (player.isYoung()) {
            priceCents = event.getYoungPriceCents();
        } else {
            priceCents = event.getPriceCents();
        }
        return priceCents;
    }

    public static double toEuros(long amountCents) {
        return amountCents / 100d;
    }

    public static Integer parseInt(String nb) {
        if (nb == null || nb.isEmpty()) {
            return null;
        }
        return Integer.parseInt(nb);
    }

    public static String readable(long size) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        double d = size;

        while (d >= 1024 && i < units.length - 1) {
            d /= 1024;
            i++;
        }

        return String.format("%.2f %s", d, units[i]);
    }
}
