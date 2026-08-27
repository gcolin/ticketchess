package com.github.gcolin.event;

import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.player.Club;
import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.registration.PlayerSubscriptionStatus;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PapiService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private LuceneDb luceneDb;

    public void setLuceneDb(LuceneDb luceneDb) {
        this.luceneDb = luceneDb;
    }

    public File generatePapiFile(Event event, List<DisplayPlayer> players) throws IOException {
        File tempFile = File.createTempFile(event.getName().replaceAll("[^a-zA-Z0-9]", "_"), ".papi");
        logger.info("create temporary file in {}", tempFile.getAbsolutePath());
        URL template = this.getClass().getClassLoader().getResource("template-3.3.8.papi");
        if (template == null) {
            throw new IOException("cannot find template-3.3.8.papi");
        }
        try (OutputStream out = new FileOutputStream(tempFile)) {
            try (InputStream in = template.openStream()) {
                in.transferTo(out);
            }
        }

        try (Database db = DatabaseBuilder.open(tempFile)) {
            updateInfoTable(db, event);
            updateJoueursTable(db, players, event);
        }

        return tempFile;
    }

    private void updateJoueursTable(Database db, List<DisplayPlayer> players, Event event) throws IOException {
        Table table = db.getTable("JOUEUR");

        int index = 2;
        for (DisplayPlayer player : players) {
            Map<String, Object> row = new HashMap<>();
            row.put("Ref", index++);

            String refffe = player.getRefffe();
            if (refffe == null || refffe.isBlank()) {
                refffe = lookupRefffe(player);
            }
            putRefFfe(row, refffe);
            if (player.getNrffe() != null
                    && !player.getNrffe().isEmpty()
                    && player.getNrffe().length() == 6) {
                row.put("NrFFE", player.getNrffe());
            }

            putNameFields(row, player);

            row.put("Sexe", extractSexe(player.getCategory()));
            row.put("NeLe", formatBirthDate(player.getBirthDate()));
            row.put("Cat", extractCat(player.getCategory()));
            row.put("Elo", extractRating(player.getRating()));
            row.put("Rapide", extractRating(player.getRapidRating()));
            row.put("Blitz", extractRating(player.getBlitzRating()));
            if (player.getFederation() == null || player.getFederation().isEmpty()) {
                row.put("Federation", "FRA");
            } else {
                row.put("Federation", truncate(player.getFederation(), 3));
            }
            row.put("Fide", extractFideFlag(player.getRating()));
            row.put("RapideFide", extractFideFlag(player.getRapidRating()));
            row.put("BlitzFide", extractFideFlag(player.getBlitzRating()));
            putFideCode(row, player);
            if (player.getFideTitre() != null && !player.getFideTitre().isBlank()) {
                row.put("FideTitre", truncate(player.getFideTitre().trim(), 2));
            }

            row.put("ClubRef", parseClubRef(player.getClubRef()));
            if (player.getClubRef() != null && !player.getClubRef().isEmpty()) {
                Club club = luceneDb.searchClub(player.getClubRef());
                if (club != null) {
                    row.put("Club", truncate(club.getNom(), 80));
                    row.put("Ligue", truncate(club.getLigue(), 3));
                }
            }

            row.put("Fixe", 0);
            row.put("Pointe", player.getAttendanceAt() != null);
            double price = ServiceUtils.toEuros(ServiceUtils.calculatePrice(player, event));
            if (player.getStatus() == PlayerSubscriptionStatus.PAID) {
                row.put("InscriptionRegle", price);
                row.put("InscriptionDu", 0);
            } else {
                row.put("InscriptionRegle", 0);
                row.put("InscriptionDu", price);
            }
            if (player.getAffType() == null || player.getAffType().isBlank()) {
                row.put("AffType", "N");
            } else {
                row.put("AffType", truncate(player.getAffType().trim(), 1));
            }

            for (int roundNum = 1; roundNum <= 24; roundNum++) {
                String roundStr = String.format("%02d", roundNum);
                row.put("Rd" + roundStr + "Cl", "R");
                row.put("Rd" + roundStr + "Res", 0);
            }

            table.addRowFromMap(row);
        }
    }

    private void putNameFields(Map<String, Object> row, DisplayPlayer player) {
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
            nom = player.getName();
            prenom = player.getFirstname();
        }
        row.put("Nom", truncate(nom, 20));
        row.put("Prenom", truncate(prenom, 20));
    }

    private void putRefFfe(Map<String, Object> row, String refffe) {
        if (refffe == null || refffe.isBlank()) {
            return;
        }
        try {
            int val = Integer.parseInt(refffe.trim());
            if (val != 0) {
                row.put("RefFFE", val);
            }
        } catch (NumberFormatException ignored) {
            // RefFFE is optional; ignore invalid values
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
            logger.debug("cannot lookup refffe for {}", key, e);
            return null;
        }
    }

    private void putFideCode(Map<String, Object> row, DisplayPlayer player) {
        String fideCode = player.getFideCode();
        if (fideCode == null || fideCode.isBlank() || "0".equals(fideCode.trim())) {
            fideCode = player.getFide();
        }
        if (fideCode != null && !fideCode.isBlank() && !"0".equals(fideCode.trim())) {
            row.put("FideCode", truncate(fideCode.trim(), 10));
        }
    }

    private int parseClubRef(String clubRef) {
        if (clubRef == null || clubRef.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(clubRef);
    }

    private void updateInfoTable(Database db, Event event) throws IOException {
        Table infoTable = db.getTable("INFO");
        DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDateTime endDate = event.getEndDate() != null ? event.getEndDate() : event.getStartDate();

        for (Row row : infoTable) {
            String name = String.valueOf(row.get("Variable"));
            switch (name) {
                case "Nom": {
                    row.put("Value", truncate(event.getName(), 50));
                    infoTable.updateRow(row);
                    break;
                }
                case "DateDebut": {
                    row.put("Value", event.getStartDate().format(DATE_FORMAT));
                    infoTable.updateRow(row);
                    break;
                }
                case "DateFin": {
                    row.put("Value", endDate.format(DATE_FORMAT));
                    infoTable.updateRow(row);
                    break;
                }
                case "NbrRondes": {
                    String rounds = getOptionValue(event, EventOptionType.ROUNDS);
                    if (rounds != null) {
                        row.put("Value", rounds);
                        infoTable.updateRow(row);
                    }
                    break;
                }
                case "Cadence": {
                    String cadence = getOptionValue(event, EventOptionType.CADENCE);
                    if (cadence != null) {
                        row.put("Value", truncate(cadence, 50));
                        infoTable.updateRow(row);
                    }
                    break;
                }
                case "Pairing": {
                    String pairing = getOptionValue(event, EventOptionType.PAIRING);
                    if (pairing != null) {
                        row.put("Value", truncate(pairing, 50));
                        infoTable.updateRow(row);
                    }
                    break;
                }
                case "ClassElo": {
                    String classElo = toClassElo(event.getEventType());
                    if (classElo != null) {
                        row.put("Value", classElo);
                        infoTable.updateRow(row);
                    }
                    break;
                }
                default:
            }
        }
    }

    private String getOptionValue(Event event, EventOptionType type) {
        if (event.getEventOptions() == null) {
            return null;
        }
        EventOption option = event.getEventOptions().get(type);
        if (option == null || option.getValue() == null || option.getValue().isBlank()) {
            return null;
        }
        return option.getValue();
    }

    private String toClassElo(EventType eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case STANDARD -> "Elo";
            case RAPID -> "Rapide";
            case BLITZ -> "Blitz";
        };
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private Integer extractRating(String rating) {
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

    private String extractFideFlag(String rating) {
        if (rating == null || rating.isEmpty()) {
            return "N";
        }
        if (rating.length() > 1 && Character.isLetter(rating.charAt(rating.length() - 1))) {
            return String.valueOf(rating.charAt(rating.length() - 1));
        }
        return "N";
    }

    private LocalDateTime formatBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(birthDate);
        } catch (Exception e) {
            try {
                return LocalDateTime.of(Integer.parseInt(birthDate), 1, 1, 0, 0);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private String extractSexe(String category) {
        if (category != null && category.endsWith("F")) {
            return "F";
        }
        return "M";
    }

    private String extractCat(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }
        char last = category.charAt(category.length() - 1);
        if (last == 'M' || last == 'F') {
            return truncate(category.substring(0, category.length() - 1), 4);
        }
        return truncate(category, 4);
    }
}
