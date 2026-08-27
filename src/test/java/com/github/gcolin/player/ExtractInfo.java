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

public class ExtractInfo {
//"src/main/resources/template-3.3.8.papi"
	public static void main(String[] args) throws Exception {
		String path = "C:\\Users\\modul\\Downloads\\3_me_Rapide_de_Perros_Guirec.papi";
		try (Database db = DatabaseBuilder.open(new File(path))) {
            Table joueurs = db.getTable("INFO");
            StringBuilder str = new StringBuilder("name;value\n");
            for (Row row : joueurs) {
        		str.append(row.getString("Variable")).append(";");
        		str.append(row.getString("Value")).append("\n");
            }
            System.out.println(str.toString());
            //Files.write(Paths.get("club.csv"), str.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }

	}

}
