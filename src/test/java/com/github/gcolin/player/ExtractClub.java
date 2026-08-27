package com.github.gcolin.player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

public class ExtractClub {

	public static void main(String[] args) throws Exception {
		try (Database db = DatabaseBuilder.open(new File("Data.mdb"))) {
            Table joueurs = db.getTable("JOUEUR");
            StringBuilder str = new StringBuilder("nom;prenom;cat;nrffe;nele;aff\n");
            for (Row row : joueurs) {
            	//int clubref = row.getInt("ClubRef");
            	//if(clubref == 274) {
            		str.append(row.getString("Nom")).append(";");
            		str.append(row.getString("Prenom")).append(";");
            		str.append(row.getString("Cat")).append(";");
            		str.append(row.getString("NrFFE")).append(";");
            		str.append(row.getLocalDateTime("NeLe")).append(";");
            		str.append(row.getString("AffType")).append("\n");
            	//}
            }
            Files.write(Paths.get("club.csv"), str.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }

	}

}
