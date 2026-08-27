package com.github.gcolin.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FideLoader {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void load(IndexWriter writer, String mdb) throws IOException {
        logger.info("load zip " + new File(mdb).getAbsolutePath());
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(mdb))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            logger.info("load " + entry.getName());
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            FideHandler handler = new FideHandler(writer);
            parser.parse(zipInputStream, handler);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }
}
