package com.github.gcolin.player;

import java.io.BufferedWriter;
import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FideToCsvHandler extends DefaultHandler {

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

    BufferedWriter writer;

    public FideToCsvHandler(BufferedWriter writer) throws IOException {
        writer.write("fideCode");
        writer.write(";");
        writer.write("name");
        writer.write(";");
        writer.write("sex");
        writer.write(";");
        writer.write("birth");
        writer.write(";");
        writer.write("federation");
        writer.write(";");
        writer.write("fideTitre");
        writer.write(";");
        writer.write("eloStd");
        writer.write(";");
        writer.write("eloRapide");
        writer.write(";");
        writer.write("eloBlitz");
        writer.write('\n');
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
                    writer.write(fideCode);
                    writer.write(";");
                    writer.write(name);
                    writer.write(";");
                    writer.write(sex);
                    writer.write(";");
                    writer.write(birth);
                    writer.write(";");
                    writer.write(federation);
                    writer.write(";");
                    writer.write(fideTitre);
                    writer.write(";");
                    writer.write(eloStd);
                    writer.write(";");
                    writer.write(eloRapide);
                    writer.write(";");
                    writer.write(eloBlitz);
                    writer.write('\n');
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                break;
        }
    }
}
