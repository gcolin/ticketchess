package com.github.gcolin.player;

import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class SchemaHandlerTest {

    @Test
    void collectsXmlPathsAndSampleValues() throws Exception {
        SchemaHandler handler = new SchemaHandler();

        SAXParserFactory.newInstance()
                .newSAXParser()
                .parse(new InputSource(new StringReader("<players><player><name>Alice</name></player></players>")), handler);

        Map<String, Set<String>> paths = handler.getPaths();
        Assertions.assertEquals(Set.of("Alice"), paths.get("players/player/name"));
        Assertions.assertTrue(paths.containsKey("players/player"));
        Assertions.assertTrue(paths.containsKey("players"));
    }
}
