package com.github.gcolin.player;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SchemaHandler extends DefaultHandler {

    private final Map<String, Set<String>> paths = new TreeMap<>();

    private StringBuilder current = new StringBuilder();
    private StringBuilder buffer = new StringBuilder();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (current.isEmpty()) {
            current.append(qName);
        } else {
            current.append('/').append(qName);
        }
        buffer.setLength(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        buffer.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String p = current.toString();
        Set<String> list = paths.get(p);
        if (list == null) {
            list = new TreeSet<String>();
            paths.put(p, list);
        }
        if (list.size() < 10) {
            list.add(buffer.toString().trim());
        }

        int index = current.lastIndexOf("/");
        if (index == -1) {
            index = 0;
        }
        current.setLength(index);
    }

    public Map<String, Set<String>> getPaths() {
        return paths;
    }
}
