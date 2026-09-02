package com.github.gcolin.player;

import com.github.gcolin.platform.ModelUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FideHandler extends DefaultHandler {

    String fideCode;
    String name;
    String sex;
    String birth;
    String federation;
    String fideTitre;
    String eloStd;
    String eloRapide;
    String eloBlitz;

    private StringBuilder buffer = new StringBuilder();

    IndexWriter writer;
    Map<String, String> titreConvert = new HashMap<>();

    public FideHandler(IndexWriter writer) {
        titreConvert.put("IM", "m");
        titreConvert.put("WIM", "mf");
        titreConvert.put("FM", "f");
        titreConvert.put("WFM", "ff");
        titreConvert.put("WGM", "gf");
        titreConvert.put("GM", "g");
        this.writer = writer;
    }

    public void clear() {
        fideCode = "";
        name = "";
        sex = "";
        birth = "";
        federation = "";
        fideTitre = "";
        eloStd = "";
        eloRapide = "";
        eloBlitz = "";
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        buffer.setLength(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        buffer.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "birthday":
                birth = buffer.toString().trim();
                break;
            case "blitz_rating":
                eloBlitz = buffer.toString().trim();
                break;
            case "rapid_rating":
                eloRapide = buffer.toString().trim();
                break;
            case "rating":
                eloStd = buffer.toString().trim();
                break;
            case "country":
                federation = buffer.toString().trim();
                break;
            case "fideid":
                fideCode = buffer.toString().trim();
                break;
            case "title":
                fideTitre = buffer.toString().trim();
                break;
            case "name":
                name = buffer.toString().trim();
                break;
            case "sex":
                sex = buffer.toString().trim();
                break;
            case "player":
                try {
                    boolean male = "M".equals(sex);
                    if (!"FRA".equals(federation)) {

                        Document doc = new Document();
                        doc.add(new StringField("type", "joueur", Field.Store.YES));
                        doc.add(new StringField("nrffe", "", Field.Store.YES));
                        doc.add(new StringField("fide", fideCode, Field.Store.YES));

                        doc.add(new TextField("nom", name, Field.Store.YES));
                        doc.add(new TextField("fullName", name + " " + fideCode, Field.Store.NO));
                        doc.add(new StringField("birth", birth, Field.Store.YES));
                        String category;
                        if (birth.isEmpty()) {
                            if (male) {
                                category = "SenM";
                            } else {
                                category = "SenF";
                            }
                        } else {
                            category = ModelUtils.getCategory(LocalDate.now(), Integer.parseInt(birth), male);
                        }
                        doc.add(new StringField("cat", category, Field.Store.YES));
                        doc.add(new StringField("fideCode", fideCode, Field.Store.YES));
                        String titreValue = fideTitre;
                        if (!titreValue.isEmpty()) {
                            String value = titreConvert.get(titreValue);
                            if (value != null) {
                                doc.add(new StringField("fideTitre", value, Field.Store.YES));
                            }
                        }
                        doc.add(new StringField("federation", federation, Field.Store.YES));
                        doc.add(new StringField("eloStd", formatFideRating(eloStd), Field.Store.YES));
                        doc.add(new StringField("eloRapide", formatFideRating(eloRapide), Field.Store.YES));
                        doc.add(new StringField("eloBlitz", formatFideRating(eloBlitz), Field.Store.YES));
                        doc.add(new StringField("affType", "A", Field.Store.YES));
                        writer.addDocument(doc);
                    }
                    clear();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                break;
        }
    }

    static String formatFideRating(String rating) {
        if (rating == null || rating.isBlank() || "0".equals(rating.trim())) {
            return "1399F";
        }
        return rating.trim() + "F";
    }
}
