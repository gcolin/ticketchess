package com.github.gcolin.player;

import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLoader {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void load(IndexWriter writer) throws IOException {
        logger.info("no database file. Add default player for tests");
        {
            Document doc = new Document();

            String nrffe = "X82897";
            String nom = "COLIN";
            String prenom = "Gael";
            String eloStd = "1400F";
            String eloRapide = "1400F";
            String eloBlitz = "1400F";
            LocalDateTime birthDate = LocalDateTime.of(1987, 2, 4, 0, 0);

            String fullName = prenom + " " + nom + " " + nrffe;

            doc.add(new StringField("type", "joueur", Field.Store.YES));

            doc.add(new StringField("fide", "0", Field.Store.YES));
            doc.add(new StringField("nrffe", nrffe, Field.Store.YES));
            doc.add(new StringField("refffe", "424242", Field.Store.YES));

            doc.add(new TextField("nom", nom, Field.Store.YES));
            doc.add(new TextField("prenom", prenom, Field.Store.YES));
            doc.add(new TextField("fullName", fullName, Field.Store.NO));
            doc.add(new StringField("birth", birthDate.toString(), Field.Store.YES));
            doc.add(new StringField("cat", "SenM", Field.Store.YES));

            doc.add(new StringField("federation", "FRA", Field.Store.YES));

            doc.add(new StringField("affType", "A", Field.Store.YES));

            doc.add(new StringField("eloStd", eloStd, Field.Store.YES));
            doc.add(new StringField("eloRapide", eloRapide, Field.Store.YES));
            doc.add(new StringField("eloBlitz", eloBlitz, Field.Store.YES));

            writer.addDocument(doc);
        }
        {
            Document doc = new Document();

            String nom = "Breton, Martin";
            String eloStd = "1774F";
            String eloRapide = "1773F";
            String eloBlitz = "1399F";

            doc.add(new StringField("type", "joueur", Field.Store.YES));

            doc.add(new StringField("fide", "343428916", Field.Store.YES));
            doc.add(new StringField("nrffe", "", Field.Store.YES));

            doc.add(new TextField("nom", nom, Field.Store.YES));
            doc.add(new TextField("fullName", nom + " 343428916", Field.Store.NO));
            doc.add(new StringField("birth", "2009", Field.Store.YES));
            doc.add(new StringField("cat", "CadM", Field.Store.YES));

            doc.add(new StringField("federation", "ENG", Field.Store.YES));

            doc.add(new StringField("affType", "A", Field.Store.YES));

            doc.add(new StringField("eloStd", eloStd, Field.Store.YES));
            doc.add(new StringField("eloRapide", eloRapide, Field.Store.YES));
            doc.add(new StringField("eloBlitz", eloBlitz, Field.Store.YES));

            writer.addDocument(doc);
        }
    }
}
