package com.github.gcolin.player;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.Player;
import com.github.gcolin.platform.ModelUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.lucene.queryparser.classic.ParseException;

@Path("club")
public class ClubApi {

    @Inject
    private LuceneDb luceneDb;

    @GET
    @Path("{ref}/composition")
    @Produces(MediaType.TEXT_PLAIN)
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    public String composition(@PathParam("ref") int ref) {
        List<Player> players;
        try {
            players = luceneDb.searchJoueursByClub(ref);
        } catch (ParseException | IOException e) {
            throw new WebApplicationException(e);
        }
        long nbM = players.stream()
                .filter(p -> p.getCategory() != null
                        && p.getCategory().endsWith("M")
                        && !p.getAffType().equals("N"))
                .count();
        long nbF = players.stream()
                .filter(p -> p.getCategory() != null
                        && p.getCategory().endsWith("F")
                        && !p.getAffType().equals("N"))
                .count();

        long licA = players.stream()
                .filter(p -> p.getAffType() != null
                        && p.getAffType().equals("A")
                        && !p.getAffType().equals("N"))
                .count();
        long licB = players.stream()
                .filter(p -> p.getAffType() != null
                        && p.getAffType().equals("B")
                        && !p.getAffType().equals("N"))
                .count();
        Map<String, Integer> licAMap = new TreeMap<>();
        Map<String, Integer> licBMap = new TreeMap<>();
        for (Player p : players) {
            if (p.getAffType() != null && !p.getAffType().equals("N")) {
                Map<String, Integer> m;
                if (p.getAffType().equals("A")) {
                    m = licAMap;
                } else {
                    m = licBMap;
                }
                Integer nb = m.get(p.getCategory());
                if (nb == null) {
                    nb = 0;
                }
                m.put(p.getCategory(), nb + 1);
            }
        }
        int sumNewplayer = 0;
        int sumNewplayerA = 0;
        int sumNewplayerB = 0;
        int sumNewplayerBa = 0;
        for (Player p : players) {
            if (p.getAffType() != null
                    && !p.getAffType().equals("N")
                    && p.getCategory().endsWith("F")) {

                if (p.getLicence().contains("Z")) {
                    sumNewplayer++;
                    System.out.println(">" + p.getFirstname() + " " + p.getName());
                }
                if (p.getAffType().equals("A")) {
                    sumNewplayerA++;
                    System.out.println(p.getFirstname() + " " + p.getName());
                } else {
                    sumNewplayerB++;
                    if (!ModelUtils.isYoung(p.getCategory())) {
                        sumNewplayerBa++;
                    }
                }
            }
        }
        System.out.println("sumNewplayer: " + sumNewplayer);
        System.out.println("sumNewplayerA: " + sumNewplayerA);
        System.out.println("sumNewplayerB: " + sumNewplayerB);
        System.out.println("sumNewplayerBa: " + sumNewplayerBa);
        System.out.println(players.stream().map(p -> p.getAffType()).collect(Collectors.toSet()));
        return "M: "
                + nbM
                + ", F: "
                + nbF
                + " total:"
                + (nbM + nbF)
                + " licA:"
                + licA
                + " licB: "
                + licB
                + "\n "
                + licAMap
                + "\n "
                + licBMap;
    }
}
