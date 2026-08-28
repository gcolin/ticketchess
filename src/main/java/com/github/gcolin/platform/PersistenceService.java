package com.github.gcolin.platform;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Config config;
    private EntityManagerFactory emf;
    private HikariDataSource dataSource;
    private final Map<String, Set<String>> tableColumnsCache = new HashMap<>();

    public PersistenceService(Config config) {
        this.config = config;
    }

    public void init() {
        initDatasource();
        applySchemaPatches();
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.nonJtaDataSource", dataSource);
        props.put("hibernate.hbm2ddl.auto", "update");

        emf = Persistence.createEntityManagerFactory("myPU", props);
        backfillMembershipTimestamps();
        initH2FromPostgresDumpIfNeeded();
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

    public void shutdown() {
        if (emf != null) emf.close();
        if (dataSource != null) dataSource.close();
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void initDatasource() {
        Properties properties = config.getProperties();
        String defaultDatabaseName = "ticketckess";
        String dbHost = properties.getProperty("db.host");
        String dbName = properties.getProperty("db.name", defaultDatabaseName);
        String dbUser = properties.getProperty("db.user");
        String dbPass = properties.getProperty("db.pass");
        String dbType = properties.getProperty("db.type", "h2");

        dataSource = new HikariDataSource();

        if ("postgres".equals(dbType)) {
            require(dbHost, "DB_HOST is required for postgres");
            require(dbUser, "DB_USER is required for postgres");
            require(dbPass, "DB_PASS is required for postgres");

            String databaseUrl = "jdbc:postgresql://" + dbHost
                    + "/"
                    + dbName
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
            dataSource.setDriverClassName("org.postgresql.Driver");
            logger.info("PostgreSQL using file-based database with authentication from environment variables");
            dataSource.setJdbcUrl(databaseUrl);
            dataSource.setUsername(dbUser);
            dataSource.setPassword(dbPass);
            dataSource.setMinimumIdle(0);
            dataSource.setMaximumPoolSize(4);
        } else {
            boolean h2Persistent = Boolean.parseBoolean(envOrProperty("DB_H2_PERSISTENT"))
                    || Boolean.parseBoolean(properties.getProperty("db.h2.persistent", "false"));
            String h2PathEnv = firstNonBlank(envOrProperty("DB_H2_PATH"), properties.getProperty("db.h2.path"));
            String databaseUrl;

            if (h2Persistent) {
                String configDir = System.getenv("CONFIG_DIR");
                String baseDir = (h2PathEnv != null && !h2PathEnv.isBlank())
                        ? h2PathEnv
                        : (configDir == null || configDir.isBlank() ? "./h2" : configDir + "/h2");

                Path dbDir = Paths.get(baseDir);
                try {
                    Files.createDirectories(dbDir);
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot create H2 directory " + dbDir, e);
                }

                Path dbFile = dbDir.resolve(dbName);
                String dbFilePath = dbFile.toAbsolutePath().toString().replace('\\', '/');
                databaseUrl = "jdbc:h2:file:" + dbFilePath;
                logger.info("h2 using persistent database on disk at {}", dbFilePath);
            } else {
                databaseUrl = "jdbc:h2:mem:" + dbName;
                logger.info(
                        "h2 using in-memory database (set db.h2.persistent=true or DB_H2_PERSISTENT=true for disk persistence)");
            }

            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setJdbcUrl(databaseUrl);
            dataSource.setMinimumIdle(1);
            dataSource.setMaximumPoolSize(2);
        }

        dataSource.setLeakDetectionThreshold(0);
    }

    private static String envOrProperty(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return System.getProperty(key);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void applySchemaPatches() {
        try (Connection connection = dataSource.getConnection()) {
            applyClubSeasonCurrentColumn(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to apply schema patches", e);
        }
    }

    private void backfillMembershipTimestamps() {
        try (Connection connection = dataSource.getConnection()) {
            backfillTimedEntityColumns(connection, "MEMBERSHIP");
            backfillTimedEntityColumns(connection, "MEMBERSHIP_OPTION");
        } catch (SQLException e) {
            logger.warn("Failed to backfill membership timestamps", e);
        }
    }

    private void backfillTimedEntityColumns(Connection connection, String table) throws SQLException {
        if (!tableExists(connection, table)
                || !columnExists(connection, table, "CREATED_AT")
                || !columnExists(connection, table, "UPDATED_AT")) {
            return;
        }
        String sql = "UPDATE " + table.toLowerCase()
                + " SET created_at = COALESCE(created_at, updated_at, CURRENT_TIMESTAMP),"
                + " updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)"
                + " WHERE created_at IS NULL OR updated_at IS NULL";
        try (Statement st = connection.createStatement()) {
            int updated = st.executeUpdate(sql);
            if (updated > 0) {
                logger.info("Backfilled timestamps on {} {} row(s)", updated, table.toLowerCase());
            }
        }
    }

    private void applyClubSeasonCurrentColumn(Connection connection) throws SQLException {
        if (!tableExists(connection, "CLUB_SEASON") || columnExists(connection, "CLUB_SEASON", "IS_CURRENT")) {
            return;
        }
        try (Statement st = connection.createStatement()) {
            st.execute(
                    "ALTER TABLE club_season ADD COLUMN IF NOT EXISTS is_current boolean NOT NULL DEFAULT false");
        }
        tableColumnsCache.remove("CLUB_SEASON");
        logger.info("Added club_season.is_current column with default false for existing rows");
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        return getTableColumns(connection, table).contains(column.toUpperCase());
    }

    private void initH2FromPostgresDumpIfNeeded() {
        Properties properties = config.getProperties();
        String dbType = properties.getProperty("db.type", "h2");
        boolean loadtest = "true".equals(System.getProperty("loadtest"));
        if (!"h2".equalsIgnoreCase(dbType) || loadtest) {
            return;
        }
        boolean enabled = Boolean.parseBoolean(properties.getProperty("db.h2.loadPostgresDump", "false"));
        if (!enabled) {
            return;
        }

        String configDir = System.getenv("CONFIG_DIR");
        String defaultPath = (configDir == null || configDir.isBlank()) ? "./src/docker/db.sql" : configDir + "/db.sql";
        Path dumpPath = Paths.get(properties.getProperty("db.h2.postgresDumpFile", defaultPath));
        if (!Files.exists(dumpPath)) {
            logger.warn("H2 bootstrap skipped: PostgreSQL dump file not found at {}", dumpPath);
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            if (!Boolean.parseBoolean(properties.getProperty("db.h2.reloadPostgresDump", "false"))
                    && !isLikelyEmpty(connection)) {
                logger.info("H2 bootstrap skipped: database already contains data");
                return;
            }
            int inserted = importPostgresCopySections(connection, dumpPath);
            logger.info("H2 bootstrap done from {} with {} inserted rows", dumpPath, inserted);
        } catch (Exception e) {
            logger.error("cannot initialize H2 from PostgreSQL dump", e);
        }
    }

    private boolean isLikelyEmpty(Connection connection) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) FROM event");
                var rs = st.executeQuery()) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    private int importPostgresCopySections(Connection connection, Path dumpPath) throws IOException, SQLException {
        int insertedRows = 0;
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement st = connection.createStatement()) {
            st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try (BufferedReader br = Files.newBufferedReader(dumpPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("COPY public.")) {
                        continue;
                    }

                    CopyHeader header = parseCopyHeader(line);
                    if (header == null) {
                        continue;
                    }
                    header = filterColumnsForTable(connection, header);
                    if (header == null) {
                        while ((line = br.readLine()) != null) {
                            if ("\\.".equals(line)) {
                                break;
                            }
                        }
                        continue;
                    }

                    String sql = buildInsertSql(header.tableName, header.columns);
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        while ((line = br.readLine()) != null) {
                            if ("\\.".equals(line)) {
                                break;
                            }

                            String[] values = line.split("\\t", -1);
                            for (int i = 0; i < header.columns.length; i++) {
                                int sourceIndex = header.sourceColumnIndices[i];
                                if (sourceIndex >= values.length || "\\N".equals(values[sourceIndex])) {
                                    ps.setObject(i + 1, null);
                                } else {
                                    ps.setString(i + 1, values[sourceIndex]);
                                }
                            }
                            ps.addBatch();
                        }
                        int[] counts = ps.executeBatch();
                        for (int count : counts) {
                            if (count > 0) {
                                insertedRows += count;
                            }
                        }
                    }
                }
            }
            synchronizeIdentityColumns(connection);
            connection.commit();
            st.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
        return insertedRows;
    }

    private void synchronizeIdentityColumns(Connection connection) throws SQLException {
        String identitiesSql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND COLUMN_NAME = 'ID' AND IS_IDENTITY = 'YES'";

        try (PreparedStatement identityTables = connection.prepareStatement(identitiesSql);
                ResultSet tables = identityTables.executeQuery()) {
            while (tables.next()) {
                String table = tables.getString(1);
                long nextId;
                try (Statement st = connection.createStatement();
                        ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM " + table)) {
                    rs.next();
                    nextId = rs.getLong(1);
                }

                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + nextId);
                }
            }
        }
    }

    private Set<String> getTableColumns(Connection connection, String table) throws SQLException {
        String cacheKey = table.toUpperCase();
        Set<String> cached = tableColumnsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Set<String> columns = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?")) {
            ps.setString(1, cacheKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString(1).toUpperCase());
                }
            }
        }
        tableColumnsCache.put(cacheKey, columns);
        return columns;
    }

    private CopyHeader filterColumnsForTable(Connection connection, CopyHeader header) throws SQLException {
        Set<String> tableColumns = getTableColumns(connection, header.tableName);
        if (tableColumns.isEmpty()) {
            logger.warn("Skipping COPY public.{}: table not found in H2 schema", header.tableName);
            return null;
        }

        List<String> columns = new ArrayList<>();
        List<Integer> sourceIndices = new ArrayList<>();
        for (int i = 0; i < header.columns.length; i++) {
            if (tableColumns.contains(header.columns[i].toUpperCase())) {
                columns.add(header.columns[i]);
                sourceIndices.add(i);
            }
        }
        if (columns.isEmpty()) {
            logger.warn("Skipping COPY public.{}: no matching columns", header.tableName);
            return null;
        }
        if (columns.size() < header.columns.length) {
            List<String> skipped = new ArrayList<>();
            for (String column : header.columns) {
                if (!tableColumns.contains(column.toUpperCase())) {
                    skipped.add(column);
                }
            }
            logger.debug("Skipping legacy columns for {}: {}", header.tableName, skipped);
        }
        return new CopyHeader(
                header.tableName,
                columns.toArray(new String[0]),
                sourceIndices.stream().mapToInt(Integer::intValue).toArray());
    }

    private CopyHeader parseCopyHeader(String line) {
        int startTable = "COPY public.".length();
        int endTable = line.indexOf(" (", startTable);
        if (endTable < 0) {
            return null;
        }
        int startColumns = endTable + 2;
        int endColumns = line.indexOf(") FROM stdin;", startColumns);
        if (endColumns < 0) {
            return null;
        }

        String table = line.substring(startTable, endTable).trim();
        String columnsRaw = line.substring(startColumns, endColumns);
        String[] columns = columnsRaw.split(",");
        for (int i = 0; i < columns.length; i++) {
            columns[i] = columns[i].trim();
        }
        return new CopyHeader(table, columns, indexRange(columns.length));
    }

    private static int[] indexRange(int length) {
        int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }
        return indices;
    }

    private String buildInsertSql(String table, String[] columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(columns[i]);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }

    private static class CopyHeader {
        private final String tableName;
        private final String[] columns;
        private final int[] sourceColumnIndices;

        private CopyHeader(String tableName, String[] columns, int[] sourceColumnIndices) {
            this.tableName = tableName;
            this.columns = columns;
            this.sourceColumnIndices = sourceColumnIndices;
        }
    }
}
