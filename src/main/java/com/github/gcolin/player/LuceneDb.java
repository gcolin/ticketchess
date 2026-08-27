package com.github.gcolin.player;

import com.github.gcolin.platform.Config;
import com.github.gcolin.player.Club;
import com.github.gcolin.player.ManualPlayerEntry;
import com.github.gcolin.player.Player;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneDb {

    private IndexSearcher searcher;
    private Analyzer analyzer;
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private File dir = new File("lucene/index");
    String mdbFile = "Data.mdb";
    String fideFile = "players_list_xml.zip";
    String manualPlayersFile = "lucene/manual-players.json";
    private Pattern decimal = Pattern.compile("\\d+");
    private Directory directory;
    private Config config;

    public void setConfig(Config config) {
        this.config = config;
    }

    public synchronized void downloadAndExtractFFeDb() throws IOException {
        String url = "https://www.echecs.asso.fr/Papi/PapiData.zip";
        File dbDir = new File(mdbFile).getAbsoluteFile().getParentFile();
        String zipPath = new File(dbDir, "PapiData.zip").getAbsolutePath();
        new File(zipPath).delete();
        logger.info("download {}", url);
        IOUtils.downloadFile(url, zipPath);
        logger.info("unzip {}", zipPath);
        IOUtils.unzip(zipPath, dbDir.getAbsolutePath());
        new File(zipPath).delete();
    }

    public synchronized void downloadFideDB() throws IOException {
        String url = "https://ratings.fide.com/download/players_list_xml.zip";
        File dbDir = new File(mdbFile).getAbsoluteFile().getParentFile();
        String zipPath = new File(dbDir, "players_list_xml.zip").getAbsolutePath();
        logger.info("download {}", url);
        IOUtils.downloadFile(url, zipPath);
    }

    public File getMdbFile() {
        return new File(mdbFile);
    }

    public File getFideFile() {
        return new File(fideFile);
    }

    public File getManualPlayersFile() {
        return new File(manualPlayersFile);
    }

    private void loadToLucene() throws IOException {
        if (!"true".equals(System.getProperty("test"))) {
            if (dir.exists()) {
                IOUtils.deleteDirectory(dir.toPath());
            }
            dir.mkdirs();
        }

        IndexWriterConfig config = new IndexWriterConfig(createAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setRAMBufferSizeMB(256); // important pour 600k

        boolean loadtest = "true".equals(System.getProperty("loadtest"));

        try (IndexWriter writer = new IndexWriter(directory, config)) {

            boolean hasFFE = !loadtest && Files.exists(Paths.get(mdbFile));
            boolean hasFide = !loadtest && Files.exists(Paths.get(fideFile));

            if (hasFFE) {
                logger.info("load FFE db");
                new FFELoader().load(writer, mdbFile);
            }

            if (hasFide) {
                logger.info("load Fide db");
                new FideLoader().load(writer, fideFile);
            }

            if (!hasFFE && !hasFide) {
                new TestLoader().load(writer);
            }

            mergeManualPlayers(writer);

            writer.commit();
            logger.info("Indexation terminée");
        }
    }

    private void mergeManualPlayers(IndexWriter writer) throws IOException {
        List<ManualPlayerEntry> manualPlayers = readManualPlayersInternal();
        if (manualPlayers.isEmpty()) {
            return;
        }

        List<ManualPlayerEntry> kept = new ArrayList<>();
        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            IndexSearcher indexSearcher = new IndexSearcher(reader);
            for (ManualPlayerEntry player : manualPlayers) {
                if (isImportedByFfeOrFide(player, indexSearcher)) {
                    logger.info("remove manual player {} because it is now imported", player.getKey());
                    continue;
                }
                writer.addDocument(manualPlayerToDocument(player));
                kept.add(player);
            }
        }

        if (kept.size() != manualPlayers.size()) {
            writeManualPlayersInternal(kept);
        }
    }

    private boolean isImportedByFfeOrFide(ManualPlayerEntry player, IndexSearcher indexSearcher) throws IOException {
        String nrffe = trimToNull(player.getNrffe());
        if (nrffe != null) {
            Query nrffeQuery = new TermQuery(new Term("nrffe", nrffe));
            TopDocs byNrffe = indexSearcher.search(nrffeQuery, 1);
            if (byNrffe.scoreDocs.length > 0) {
                return true;
            }
        }

        String fide = trimToNull(player.getFide());
        if (fide != null && !"0".equals(fide)) {
            Query fideQuery = new TermQuery(new Term("fide", fide));
            TopDocs byFide = indexSearcher.search(fideQuery, 1);
            return byFide.scoreDocs.length > 0;
        }
        return false;
    }

    private Document manualPlayerToDocument(ManualPlayerEntry player) {
        Document doc = new Document();
        doc.add(new StringField("type", "joueur", Field.Store.YES));

        String nrffe = trimToNull(player.getNrffe());
        String fide = trimToNull(player.getFide());
        String name = defaultString(trimToNull(player.getName()));
        String firstname = defaultString(trimToNull(player.getFirstname()));

        doc.add(new StringField("nrffe", defaultString(nrffe), Field.Store.YES));
        doc.add(new StringField("fide", fide == null ? "0" : fide, Field.Store.YES));

        doc.add(new TextField("nom", name, Field.Store.YES));
        doc.add(new TextField("prenom", firstname, Field.Store.YES));
        doc.add(new TextField(
                "fullName",
                (firstname + " " + name + " " + defaultString(nrffe) + " " + defaultString(fide)).trim(),
                Field.Store.NO));

        doc.add(new StringField("birth", defaultString(trimToNull(player.getBirth())), Field.Store.YES));
        doc.add(new StringField("cat", defaultString(trimToNull(player.getCategory()), "SenM"), Field.Store.YES));
        doc.add(new StringField("affType", defaultString(trimToNull(player.getAffType()), "A"), Field.Store.YES));
        doc.add(new StringField(
                "fideCode", defaultString(trimToNull(player.getFideCode()), defaultString(fide)), Field.Store.YES));
        if (player.getFideTitre() != null && !player.getFideTitre().isEmpty()) {
            doc.add(new StringField("fideTitre", defaultString(trimToNull(player.getFideTitre())), Field.Store.YES));
        }
        doc.add(new StringField(
                "federation", defaultString(trimToNull(player.getFederation()), "FRA"), Field.Store.YES));
        doc.add(new StringField("club", defaultString(trimToNull(player.getClub())), Field.Store.YES));
        doc.add(new StringField("eloStd", defaultString(trimToNull(player.getEloStd()), "1399F"), Field.Store.YES));
        doc.add(new StringField(
                "eloRapide", defaultString(trimToNull(player.getEloRapide()), "1399F"), Field.Store.YES));
        doc.add(new StringField("eloBlitz", defaultString(trimToNull(player.getEloBlitz()), "1399F"), Field.Store.YES));
        return doc;
    }

    private Analyzer createAnalyzer() {
        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new StandardTokenizer();
                TokenStream stream = new LowerCaseFilter(tokenizer);
                stream = new ASCIIFoldingFilter(stream);
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
        return analyzer;
    }

    public void init() {
        configurePaths();
    }

    private void configurePaths() {
        String configDir = config.getConfigDir();
        dir = new File(configDir + "/lucene/index");
        mdbFile = configDir + "/Data.mdb";
        fideFile = configDir + "/players_list_xml.zip";
        manualPlayersFile = configDir + "/lucene/manual-players.json";
    }

    private void ensureLoaded() throws IOException {
        if (searcher == null) {
            load(false);
        }
    }

    public synchronized void load(boolean refresh) throws IOException {

        close();
        logger.info("load LuceneDb from {}", dir.getAbsolutePath());

        if ("true".equals(System.getProperty("test"))) {
            directory = new ByteBuffersDirectory();
            loadToLucene();
        } else {
            directory = FSDirectory.open(Paths.get(dir.getAbsolutePath()));
            if (!dir.exists() || refresh) {
                loadToLucene();
            }
        }

        try {
            openForRead();
        } catch (IOException e) {
            logger.warn("cannot open lucene, rebuilding index", e);
            loadToLucene();
            openForRead();
        }
    }

    private void openForRead() throws IOException {
        DirectoryReader reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        analyzer = createAnalyzer();
    }

    public void close() throws IOException {
        if (searcher != null) {
            searcher.getIndexReader().close();
            searcher = null;
        }
        if (analyzer != null) {
            analyzer.close();
            analyzer = null;
        }
    }

    public synchronized List<Club> searchClub(String query, int nb) throws ParseException, IOException {
        ensureLoaded();
        QueryParser clubParser = new QueryParser("clubSearch", analyzer);
        clubParser.setDefaultOperator(QueryParser.Operator.AND);

        Query clubQuery = clubParser.parse(query);

        BooleanQuery.Builder clubBool = new BooleanQuery.Builder();

        clubBool.add(clubQuery, BooleanClause.Occur.MUST);
        Query finalClubQuery = clubBool.build();

        TopDocs clubHits = searcher.search(finalClubQuery, nb);

        List<Club> clubs = new ArrayList<>();

        for (ScoreDoc sd : clubHits.scoreDocs) {

            Document doc = searcher.storedFields().document(sd.doc);
            clubs.add(docToClub(doc, sd.score));
        }
        return clubs;
    }

    public synchronized Club searchClub(String ref) throws IOException {
        ensureLoaded();
        Query clubQuery = IntPoint.newExactQuery("clubRef", Integer.parseInt(ref));

        TopDocs hits = searcher.search(clubQuery, 1);

        if (hits.scoreDocs.length > 0) {
            Document doc = searcher.storedFields().document(hits.scoreDocs[0].doc);
            return docToClub(doc, hits.scoreDocs[0].score);
        }
        return null;
    }

    public synchronized List<Player> searchJoueursByClub(int ref) throws ParseException, IOException {
        ensureLoaded();
        Query nrffeQuery = IntPoint.newExactQuery("clubRef", ref);
        TopDocs hits = searcher.search(nrffeQuery, 200);
        List<Player> players = new ArrayList<>();
        if (hits.scoreDocs.length > 0) {
            for (ScoreDoc sd : hits.scoreDocs) {
                Document doc = searcher.storedFields().document(sd.doc);
                players.add(docToJoueur(doc, sd.score));
            }
        }
        return players;
    }

    public synchronized Player searchJoueur(String nrFFE) throws ParseException, IOException {
        ensureLoaded();
        Query nrffeQuery;
        if (decimal.matcher(nrFFE).matches()) {
            nrffeQuery = new TermQuery(new Term("fide", nrFFE));
        } else {
            nrffeQuery = new TermQuery(new Term("nrffe", nrFFE));
        }
        TopDocs hits = searcher.search(nrffeQuery, 1);

        if (hits.scoreDocs.length > 0) {
            Document doc = searcher.storedFields().document(hits.scoreDocs[0].doc);
            return docToJoueur(doc, hits.scoreDocs[0].score);
        }
        return null;
    }

    public synchronized Player searchJoueurByNrffe(String nrffe) throws IOException {
        ensureLoaded();
        String value = trimToNull(nrffe);
        if (value == null) {
            return null;
        }
        return searchJoueurByField("nrffe", value);
    }

    public synchronized Player searchJoueurByFide(String fide) throws IOException {
        ensureLoaded();
        String value = trimToNull(fide);
        if (value == null) {
            return null;
        }
        return searchJoueurByField("fide", value);
    }

    private Player searchJoueurByField(String field, String value) throws IOException {
        Query query = new TermQuery(new Term(field, value));
        TopDocs hits = searcher.search(query, 1);
        if (hits.scoreDocs.length > 0) {
            Document doc = searcher.storedFields().document(hits.scoreDocs[0].doc);
            return docToJoueur(doc, hits.scoreDocs[0].score);
        }
        return null;
    }

    public synchronized List<Player> searchJoueur(String query, int nb, Integer club)
            throws ParseException, IOException {
        ensureLoaded();
        QueryParser playerParser = new QueryParser("fullName", analyzer);
        playerParser.setDefaultOperator(QueryParser.Operator.AND);

        Query playerQuery = playerParser.parse(query);

        BooleanQuery.Builder playerBool = new BooleanQuery.Builder();

        playerBool.add(playerQuery, BooleanClause.Occur.MUST);
        if (club != null) {
            playerBool.add(IntPoint.newExactQuery("clubRef", club), BooleanClause.Occur.FILTER);
        }

        Query finalClubQuery = playerBool.build();

        TopDocs clubHits = searcher.search(finalClubQuery, nb);

        List<Player> clubs = new ArrayList<>();

        for (ScoreDoc sd : clubHits.scoreDocs) {

            Document doc = searcher.storedFields().document(sd.doc);
            clubs.add(docToJoueur(doc, sd.score));
        }
        return clubs;
    }

    private Player docToJoueur(Document doc, float score) {
        Player joueur = new Player();
        joueur.setNrffe(doc.get("nrffe"));
        joueur.setName(doc.get("nom"));
        joueur.setFirstname(doc.get("prenom"));
        if (joueur.getFirstname() == null) {
            joueur.setFirstname("");
        }
        joueur.setClubRef(doc.get("clubRef"));
        joueur.setRating(doc.get("eloStd"));
        joueur.setRapidRating(doc.get("eloRapide"));
        joueur.setBlitzRating(doc.get("eloBlitz"));
        joueur.setBirthDate(doc.get("birth"));
        joueur.setCategory(doc.get("cat"));
        joueur.setAffType(doc.get("affType"));
        joueur.setFideCode(doc.get("fideCode"));
        joueur.setFideTitre(doc.get("fideTitre"));
        joueur.setFederation(doc.get("federation"));
        joueur.setClub(doc.get("club"));
        joueur.setFide(doc.get("fide"));
        joueur.setRefffe(doc.get("refffe"));
        return joueur;
    }

    private Club docToClub(Document doc, float score) {
        Club club = new Club();
        club.setRef(doc.get("clubRef"));
        club.setNrffe(doc.get("nrffe"));
        club.setNom(doc.get("nom"));
        club.setCommune(doc.get("commune"));
        club.setActif(doc.get("actif"));
        club.setLigue(doc.get("ligue"));
        club.setScore(score);
        return club;
    }

    public synchronized List<ManualPlayerEntry> getManualPlayers() throws IOException {
        return readManualPlayersInternal();
    }

    public synchronized void addManualPlayer(ManualPlayerEntry player) throws IOException {
        validateManualPlayer(player);
        List<ManualPlayerEntry> players = readManualPlayersInternal();

        String key = player.getKey();
        players.removeIf(entry -> key.equals(entry.getKey()));
        players.add(player);

        writeManualPlayersInternal(players);
    }

    public synchronized boolean removeManualPlayer(String key) throws IOException {
        String normalizedKey = trimToNull(key);
        if (normalizedKey == null) {
            return false;
        }
        List<ManualPlayerEntry> players = readManualPlayersInternal();
        boolean removed = players.removeIf(entry -> normalizedKey.equals(entry.getKey()));
        if (removed) {
            writeManualPlayersInternal(players);
        }
        return removed;
    }

    private void validateManualPlayer(ManualPlayerEntry player) {
        if (trimToNull(player.getName()) == null) {
            throw new IllegalArgumentException("name is required");
        }
        if (trimToNull(player.getNrffe()) == null && trimToNull(player.getFide()) == null) {
            throw new IllegalArgumentException("nrffe or fide is required");
        }
        player.normalize();
    }

    private List<ManualPlayerEntry> readManualPlayersInternal() throws IOException {
        File file = getManualPlayersFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }

        if (file.length() == 0) {
            return new ArrayList<>();
        }

        Jsonb jsonb = JsonbBuilder.create();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            ManualPlayerEntry[] array = jsonb.fromJson(reader, ManualPlayerEntry[].class);
            if (array == null || array.length == 0) {
                return new ArrayList<>();
            }
            List<ManualPlayerEntry> entries = new ArrayList<>();
            for (ManualPlayerEntry entry : Arrays.asList(array)) {
                if (entry == null) {
                    continue;
                }
                entry.normalize();
                if (entry.getKey() != null && entry.getName() != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (RuntimeException e) {
            throw new IOException("cannot parse manual players JSON", e);
        } finally {
            closeJsonb(jsonb);
        }
    }

    private void writeManualPlayersInternal(List<ManualPlayerEntry> players) throws IOException {
        File file = getManualPlayersFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Map<String, ManualPlayerEntry> dedup = new LinkedHashMap<>();
        for (ManualPlayerEntry player : players) {
            player.normalize();
            if (player.getKey() != null) {
                dedup.put(player.getKey(), player);
            }
        }

        List<ManualPlayerEntry> normalized = new ArrayList<>(dedup.values());

        JsonbConfig jsonbConfig = new JsonbConfig().withFormatting(true);
        Jsonb jsonb = JsonbBuilder.create(jsonbConfig);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            jsonb.toJson(normalized, writer);
        } finally {
            closeJsonb(jsonb);
        }
    }

    private void closeJsonb(Jsonb jsonb) throws IOException {
        try {
            jsonb.close();
        } catch (Exception e) {
            throw new IOException("cannot close jsonb", e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
