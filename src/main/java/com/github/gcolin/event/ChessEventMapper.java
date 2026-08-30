package com.github.gcolin.event;

import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.player.Club;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscription;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChessEventMapper {

    private final LuceneDb luceneDb;

    public ChessEventMapper(LuceneDb luceneDb) {
        this.luceneDb = luceneDb;
    }

    public Map<String, Object> mapTournament(
            Event event, List<DisplayPlayer> players, Map<Integer, PlayerSubscription> subscriptionsBySubId) {
        Map<String, Object> tournament = new LinkedHashMap<>();
        tournament.put("name", event.getName());
        tournament.put("type", 1);
        tournament.put("rounds", parseRounds(event));
        tournament.put("pairing", mapPairing(getOptionValue(event, EventOptionType.PAIRING)));
        tournament.put("time_control", nullToEmpty(getOptionValue(event, EventOptionType.CADENCE)));
        tournament.put("location", "");
        tournament.put("arbiter", "");
        tournament.put("start", toEpochSeconds(event.getStartDate()));
        tournament.put("end", toEpochSeconds(event.getEndDate() != null ? event.getEndDate() : event.getStartDate()));
        tournament.put("tie_break_1", 0);
        tournament.put("tie_break_2", 0);
        tournament.put("tie_break_3", 0);
        tournament.put("rating", mapRatingType(event.getEventType()));

        List<Map<String, Object>> playerList = new ArrayList<>();
        for (DisplayPlayer player : players) {
            PlayerSubscription sub = subscriptionsBySubId.get(player.getSubId());
            playerList.add(mapPlayer(event, player, sub));
        }
        tournament.put("players", playerList);
        return tournament;
    }

    public Map<String, Object> mapPlayer(Event event, DisplayPlayer player, PlayerSubscription sub) {
        Map<String, Object> row = new LinkedHashMap<>();
        String[] names = splitName(player);
        row.put("last_name", names[0]);
        row.put("first_name", names[1]);
        row.put("gender", mapGender(player.getCategory()));
        row.put("birth", toBirthEpoch(player.getBirthDate()));

        String federation = player.getFederation();
        row.put("federation", federation == null || federation.isBlank() ? "FRA" : federation.trim());

        row.put("fide_id", parseFideId(player));
        row.put("ffe_id", parseFfeId(player));
        row.put("ffe_license", mapFfeLicence(player.getAffType()));
        row.put("ffe_license_number", mapLicenceNumber(player.getNrffe()));
        row.put("ffe_league", mapLeague(player));
        row.put("ffe_club_id", parseClubRef(player.getClubRef()));
        row.put("ffe_club", mapClubName(player));
        row.put("category", mapCategory(player.getCategory()));

        row.put("standard_rating", extractRating(player.getRating()));
        row.put("standard_rating_type", mapRatingTypeFlag(player.getRating()));
        row.put("rapid_rating", extractRating(player.getRapidRating()));
        row.put("rapid_rating_type", mapRatingTypeFlag(player.getRapidRating()));
        row.put("blitz_rating", extractRating(player.getBlitzRating()));
        row.put("blitz_rating_type", mapRatingTypeFlag(player.getBlitzRating()));
        row.put("title", mapFideTitle(player.getFideTitre()));

        row.put("email", sub == null || sub.getCreationUser() == null ? "" : sub.getCreationUser());
        row.put("phone", "");

        double initialFee = ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, event));
        double paidBefore = 0;
        if (player.getStatus() == PlayerSubscriptionStatus.PAID) {
            if (sub != null && sub.getAmountCents() != null) {
                paidBefore = ServiceUtils.toEuros(sub.getAmountCents());
            } else {
                paidBefore = initialFee;
            }
        }
        double discount = 0;
        row.put("initial_fee", initialFee);
        row.put("discount", discount);
        row.put("paid_before", paidBefore);
        row.put("paid_site", 0.0);
        row.put("fee", initialFee - discount);
        row.put("paid", paidBefore);

        row.put("check_in", player.getAttendanceAt() != null);
        row.put("board", 0);
        row.put("skipped_rounds", Map.of());
        return row;
    }

    static int mapPairing(String pairing) {
        if (pairing == null || pairing.isBlank()) {
            return 1;
        }
        return switch (pairing.trim()) {
            case "Haley" -> 2;
            case "HaleySoft" -> 3;
            case "SAD" -> 4;
            case "Nicois" -> 5;
            case "Berger" -> 6;
            default -> 1;
        };
    }

    static int mapRatingType(EventType eventType) {
        if (eventType == null) {
            return 1;
        }
        return switch (eventType) {
            case RAPID -> 2;
            case BLITZ -> 3;
            default -> 1;
        };
    }

    static int parseRounds(Event event) {
        String rounds = getOptionValue(event, EventOptionType.ROUNDS);
        if (rounds == null || rounds.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(rounds.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static Long toEpochSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static String getOptionValue(Event event, EventOptionType type) {
        if (event.getEventOptions() == null) {
            return null;
        }
        EventOption option = event.getEventOptions().get(type);
        if (option == null || option.getValue() == null || option.getValue().isBlank()) {
            return null;
        }
        return option.getValue();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String[] splitName(DisplayPlayer player) {
        String nom;
        String prenom;
        if (player.getFirstname() == null || player.getFirstname().isEmpty()) {
            String fullName = player.getName() == null ? "" : player.getName();
            int split = fullName.lastIndexOf(',');
            if (split >= 0) {
                nom = fullName.substring(0, split).trim();
                prenom = fullName.substring(split + 1).trim();
            } else {
                nom = fullName.trim();
                prenom = "";
            }
        } else {
            nom = player.getName() == null ? "" : player.getName();
            prenom = player.getFirstname();
        }
        return new String[] {nom, prenom};
    }

    static int mapGender(String category) {
        if (category != null && category.endsWith("F")) {
            return 1;
        }
        if (category != null && category.endsWith("M")) {
            return 2;
        }
        return 0;
    }

    static Long toBirthEpoch(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return 0L;
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(birthDate);
            return parsed.atZone(ZoneId.systemDefault()).toEpochSecond();
        } catch (Exception ignored) {
            try {
                LocalDate yearOnly = LocalDate.of(Integer.parseInt(birthDate.trim()), 1, 1);
                return yearOnly.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            } catch (Exception ignored2) {
                return 0L;
            }
        }
    }

    private int parseFideId(DisplayPlayer player) {
        String fideCode = player.getFideCode();
        if (fideCode == null || fideCode.isBlank() || "0".equals(fideCode.trim())) {
            fideCode = player.getFide();
        }
        if (fideCode == null || fideCode.isBlank() || "0".equals(fideCode.trim())) {
            return 0;
        }
        try {
            return Integer.parseInt(fideCode.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseFfeId(DisplayPlayer player) {
        String refffe = player.getRefffe();
        if (refffe == null || refffe.isBlank()) {
            refffe = lookupRefffe(player);
        }
        if (refffe == null || refffe.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(refffe.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String lookupRefffe(DisplayPlayer player) {
        String key = player.getNrffeId();
        if (key == null || key.isBlank()) {
            key = player.getNrffe();
        }
        if (key == null || key.isBlank() || key.startsWith("@")) {
            key = player.getFide();
        }
        if (key == null || key.isBlank() || "0".equals(key)) {
            return null;
        }
        try {
            var lucenePlayer = luceneDb.searchJoueur(key);
            return lucenePlayer == null ? null : lucenePlayer.getRefffe();
        } catch (Exception e) {
            return null;
        }
    }

    static int mapFfeLicence(String affType) {
        if (affType == null || affType.isBlank()) {
            return 0;
        }
        return switch (affType.trim().toUpperCase()) {
            case "N" -> 1;
            case "B" -> 2;
            case "A" -> 3;
            default -> 0;
        };
    }

    static String mapLicenceNumber(String nrffe) {
        if (nrffe != null && nrffe.length() == 6) {
            return nrffe;
        }
        return "";
    }

    private String mapLeague(DisplayPlayer player) {
        if (player.getClubRef() == null || player.getClubRef().isBlank()) {
            return "";
        }
        try {
            Club club = luceneDb.searchClub(player.getClubRef());
            return club == null || club.getLigue() == null ? "" : club.getLigue();
        } catch (Exception e) {
            return "";
        }
    }

    static int parseClubRef(String clubRef) {
        if (clubRef == null || clubRef.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(clubRef.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String mapClubName(DisplayPlayer player) {
        if (player.getClub() != null && !player.getClub().isBlank()) {
            return player.getClub();
        }
        if (player.getClubRef() == null || player.getClubRef().isBlank()) {
            return "";
        }
        try {
            Club club = luceneDb.searchClub(player.getClubRef());
            return club == null || club.getNom() == null ? "" : club.getNom();
        } catch (Exception e) {
            return "";
        }
    }

    static int mapCategory(String category) {
        if (category == null || category.isBlank()) {
            return 0;
        }
        String cat = category;
        if (cat.endsWith("M") || cat.endsWith("F")) {
            cat = cat.substring(0, cat.length() - 1);
        }
        return switch (cat) {
            case "Ppo" -> 1;
            case "Pou" -> 2;
            case "Pup" -> 3;
            case "Ben" -> 4;
            case "Min" -> 5;
            case "Cad" -> 6;
            case "Jun" -> 7;
            case "Sen" -> 8;
            case "Sep" -> 9;
            case "Vet" -> 10;
            default -> 0;
        };
    }

    static int extractRating(String rating) {
        if (rating == null || rating.isEmpty()) {
            return 0;
        }
        try {
            if (rating.length() > 1 && Character.isLetter(rating.charAt(rating.length() - 1))) {
                return Integer.parseInt(rating.substring(0, rating.length() - 1));
            }
            return Integer.parseInt(rating);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int mapRatingTypeFlag(String rating) {
        if (rating == null || rating.isEmpty()) {
            return 1;
        }
        if (rating.length() > 1 && Character.isLetter(rating.charAt(rating.length() - 1))) {
            return switch (Character.toUpperCase(rating.charAt(rating.length() - 1))) {
                case 'E' -> 1;
                case 'N' -> 2;
                case 'F' -> 3;
                default -> 1;
            };
        }
        return 1;
    }

    static int mapFideTitle(String fideTitre) {
        if (fideTitre == null || fideTitre.isBlank()) {
            return 0;
        }
        return switch (fideTitre.trim().toUpperCase()) {
            case "WFM" -> 1;
            case "FM" -> 2;
            case "WIM" -> 3;
            case "IM" -> 4;
            case "WGM" -> 5;
            case "GM" -> 6;
            default -> 0;
        };
    }
}
