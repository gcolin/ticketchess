package com.github.gcolin.platform;

import com.github.gcolin.auth.RoleCode;
import com.github.gcolin.event.EventCollectionOptionType;
import com.github.gcolin.event.EventOptionType;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);
    private static final Pattern CHECK_CONSTRAINT_ENUM_VALUE =
            Pattern.compile("'([A-Z][A-Z0-9_]*)'");

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
        backfillMembershipSeasonIdsAfterBootstrap();
    }

    private void backfillMembershipSeasonIdsAfterBootstrap() {
        try (Connection connection = dataSource.getConnection()) {
            backfillMembershipSeasonIds(connection);
        } catch (SQLException e) {
            logger.warn("Failed to backfill membership season ids after bootstrap", e);
        }
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
            applyMembershipLicenseTypeColumn(connection);
            applyMembershipSeasonColumns(connection);
            applyMembershipPaymentColumn(connection);
            migratePermissionsToRoles(connection);
            if (isH2()) {
                applyH2EnumColumnPatch(connection, "eventoption", EventOptionType.class);
                applyH2EnumColumnPatch(connection, "eventcollectionoption", EventCollectionOptionType.class);
            } else {
                applyPostgresEnumCheckConstraintPatch(connection, "eventoption", EventOptionType.class);
                applyPostgresEnumCheckConstraintPatch(
                        connection, "eventcollectionoption", EventCollectionOptionType.class);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to apply schema patches", e);
        }
    }

    private boolean isH2() {
        return "h2".equalsIgnoreCase(config.getProperties().getProperty("db.type", "h2"));
    }

    private void applyH2EnumColumnPatch(Connection connection, String table, Class<? extends Enum<?>> enumClass)
            throws SQLException {
        String tableKey = table.toUpperCase();
        if (!tableExists(connection, tableKey) || !columnExists(connection, tableKey, "OPTION_TYPE")) {
            return;
        }
        if (!isH2EnumColumn(connection, tableKey, "OPTION_TYPE")) {
            return;
        }

        String enumValues = Arrays.stream(enumClass.getEnumConstants())
                .map(value -> "'" + value.name() + "'")
                .collect(Collectors.joining(", "));
        String sql = "ALTER TABLE " + table + " ALTER COLUMN option_type ENUM(" + enumValues + ") NOT NULL";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
        tableColumnsCache.remove(tableKey);
        logger.info("Updated {}.option_type enum to match {}", table, enumClass.getSimpleName());
    }

    private void applyPostgresEnumCheckConstraintPatch(
            Connection connection, String table, Class<? extends Enum<?>> enumClass) throws SQLException {
        if (!postgresTableHasColumn(connection, table, "option_type")) {
            return;
        }

        String constraintName = table + "_option_type_check";
        Set<String> expectedValues = enumValues(enumClass);
        Set<String> currentValues = readPostgresCheckConstraintEnumValues(connection, constraintName);
        if (currentValues.equals(expectedValues)) {
            logger.debug("{}.option_type check constraint already up to date", table);
            return;
        }

        String enumValues = expectedValues.stream()
                .map(value -> "'" + value.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));

        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraintName);
            st.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName + " CHECK (option_type IN ("
                    + enumValues
                    + "))");
        }
        logger.info("Updated {}.option_type check constraint to match {}", table, enumClass.getSimpleName());
    }

    private Set<String> readPostgresCheckConstraintEnumValues(Connection connection, String constraintName)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?")) {
            ps.setString(1, constraintName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Set.of();
                }
                return parseEnumNamesFromCheckConstraint(rs.getString(1));
            }
        }
    }

    static Set<String> parseEnumNamesFromCheckConstraint(String constraintDef) {
        if (constraintDef == null || constraintDef.isBlank()) {
            return Set.of();
        }
        Set<String> values = new TreeSet<>();
        Matcher matcher = CHECK_CONSTRAINT_ENUM_VALUE.matcher(constraintDef);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static Set<String> enumValues(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean postgresTableHasColumn(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
            ps.setString(1, table.toLowerCase(Locale.ROOT));
            ps.setString(2, column.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private boolean isH2EnumColumn(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return "ENUM".equalsIgnoreCase(rs.getString(1));
            }
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

    private void applyMembershipLicenseTypeColumn(Connection connection) throws SQLException {
        if (isH2()) {
            if (!tableExists(connection, "MEMBERSHIP") || columnExists(connection, "MEMBERSHIP", "LICENSE_TYPE")) {
                return;
            }
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE membership ADD COLUMN license_type VARCHAR(10)");
            }
            tableColumnsCache.remove("MEMBERSHIP");
        } else {
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE membership ADD COLUMN IF NOT EXISTS license_type VARCHAR(10)");
            }
        }
        logger.info("Ensured membership.license_type column exists");
    }

    private void applyMembershipSeasonColumns(Connection connection) throws SQLException {
        if (isH2()) {
            if (tableExists(connection, "MEMBERSHIP") && !columnExists(connection, "MEMBERSHIP", "SEASON_ID")) {
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE membership ADD COLUMN season_id INTEGER");
                }
                tableColumnsCache.remove("MEMBERSHIP");
            }
            if (tableExists(connection, "MEMBERSHIP_OPTION") && !columnExists(connection, "MEMBERSHIP_OPTION", "SEASON_ID")) {
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE membership_option ADD COLUMN season_id INTEGER");
                }
                tableColumnsCache.remove("MEMBERSHIP_OPTION");
            }
        } else {
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE membership ADD COLUMN IF NOT EXISTS season_id INTEGER");
                st.execute("ALTER TABLE membership_option ADD COLUMN IF NOT EXISTS season_id INTEGER");
            }
        }
        backfillMembershipSeasonIds(connection);
        logger.info("Ensured membership and membership_option season_id columns exist");
    }

    private void applyMembershipPaymentColumn(Connection connection) throws SQLException {
        if (isH2()) {
            if (!tableExists(connection, "MEMBERSHIP") || columnExists(connection, "MEMBERSHIP", "PAYMENT_ID")) {
                return;
            }
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE membership ADD COLUMN payment_id BIGINT");
            }
            tableColumnsCache.remove("MEMBERSHIP");
        } else {
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE membership ADD COLUMN IF NOT EXISTS payment_id BIGINT");
            }
        }
        logger.info("Ensured membership.payment_id column exists");
    }

    private static final int MEMBERSHIP_PRE_SEASON_MONTHS = 5;

    private void backfillMembershipSeasonIds(Connection connection) throws SQLException {
        if (!tableExists(connection, "CLUB_SEASON")) {
            return;
        }
        List<SeasonDates> seasons = loadClubSeasons(connection);
        if (seasons.isEmpty()) {
            return;
        }
        Integer fallbackSeasonId = resolveCurrentSeasonId(connection, seasons);
        if (tableExists(connection, "MEMBERSHIP") && columnExists(connection, "MEMBERSHIP", "SEASON_ID")) {
            int updated = backfillTableSeasonId(connection, "membership", seasons, MEMBERSHIP_PRE_SEASON_MONTHS, fallbackSeasonId);
            if (updated > 0) {
                logger.info("Backfilled season_id on {} membership row(s)", updated);
            }
        }
        if (tableExists(connection, "MEMBERSHIP_OPTION") && columnExists(connection, "MEMBERSHIP_OPTION", "SEASON_ID")) {
            int updated = backfillTableSeasonId(connection, "membership_option", seasons, 0, fallbackSeasonId);
            if (updated > 0) {
                logger.info("Backfilled season_id on {} membership_option row(s)", updated);
            }
        }
    }

    private Integer resolveCurrentSeasonId(Connection connection, List<SeasonDates> seasons) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM club_season WHERE is_current = true ORDER BY start_date DESC LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return seasons.isEmpty() ? null : seasons.get(0).id();
    }

    private List<SeasonDates> loadClubSeasons(Connection connection) throws SQLException {
        List<SeasonDates> seasons = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, start_date, end_date FROM club_season ORDER BY start_date DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate startDate = rs.getDate("start_date").toLocalDate();
                LocalDate endDate = rs.getDate("end_date").toLocalDate();
                seasons.add(new SeasonDates(rs.getInt("id"), startDate, endDate));
            }
        }
        return seasons;
    }

    private int backfillTableSeasonId(
            Connection connection,
            String table,
            List<SeasonDates> seasons,
            int preSeasonMonths,
            Integer fallbackSeasonId)
            throws SQLException {
        int updated = 0;
        String selectSql = "SELECT id, created_at, updated_at, season_id FROM " + table;
        try (PreparedStatement select = connection.prepareStatement(selectSql);
                ResultSet rows = select.executeQuery();
                PreparedStatement update =
                        connection.prepareStatement("UPDATE " + table + " SET season_id = ? WHERE id = ?")) {
            while (rows.next()) {
                LocalDateTime timestamp = coalesceTimestamp(rows.getTimestamp("created_at"), rows.getTimestamp("updated_at"));
                Integer seasonId = resolveSeasonId(seasons, timestamp, preSeasonMonths);
                if (seasonId == null) {
                    seasonId = fallbackSeasonId;
                }
                if (seasonId == null) {
                    continue;
                }
                int currentSeasonId = rows.getInt("season_id");
                if (!rows.wasNull() && currentSeasonId == seasonId) {
                    continue;
                }
                update.setInt(1, seasonId);
                update.setInt(2, rows.getInt("id"));
                updated += update.executeUpdate();
            }
        }
        return updated;
    }

    private static LocalDateTime coalesceTimestamp(Timestamp createdAt, Timestamp updatedAt) {
        Timestamp value = createdAt != null ? createdAt : updatedAt;
        return value == null ? null : value.toLocalDateTime();
    }

    private static Integer resolveSeasonId(List<SeasonDates> seasons, LocalDateTime timestamp, int preSeasonMonths) {
        if (timestamp == null) {
            return null;
        }
        LocalDate date = timestamp.toLocalDate();
        for (SeasonDates season : seasons) {
            if (!date.isBefore(season.startDate()) && !date.isAfter(season.endDate())) {
                return season.id();
            }
        }
        if (preSeasonMonths > 0) {
            for (SeasonDates season : seasons) {
                LocalDate windowStart = season.startDate().minusMonths(preSeasonMonths);
                if (!date.isBefore(windowStart) && date.isBefore(season.startDate())) {
                    return season.id();
                }
            }
        }
        return null;
    }

    private record SeasonDates(int id, LocalDate startDate, LocalDate endDate) {}

    private void migratePermissionsToRoles(Connection connection) throws SQLException {
        if (!tableExists(connection, "USER_AUTHORIZATION")) {
            return;
        }

        prepareUserAuthorizationRoleColumnForMigration(connection);

        boolean hasPermissionColumn = columnExists(connection, "USER_AUTHORIZATION", "PERMISSION");
        boolean hasRoleColumn = columnExists(connection, "USER_AUTHORIZATION", "ROLE");
        String valueColumn = hasPermissionColumn ? "permission" : (hasRoleColumn ? "role" : null);
        if (valueColumn == null) {
            return;
        }

        Map<String, String> mapping = Map.ofEntries(
                Map.entry("USER_IMPERSONATE", "ADMIN"),
                Map.entry("ADMIN_PANEL", "ADMIN"),
                Map.entry("ADMIN_USER", "ADMIN"),
                Map.entry("PAYMENT_READ", "TRESORIER"),
                Map.entry("PAYMENT_WRITE", "TRESORIER"),
                Map.entry("EVENT_CREATE", "EVENT_ADMIN"),
                Map.entry("EVENT_EDIT", "EVENT_ADMIN"),
                Map.entry("EVENT_DELETE", "EVENT_ADMIN"),
                Map.entry("MAIL_SEND", "EVENT_ADMIN"),
                Map.entry("EVENT_READ", "ARBITRE"));

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String sql = "UPDATE user_authorization SET " + valueColumn + " = ? WHERE " + valueColumn + " = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, entry.getValue());
                ps.setString(2, entry.getKey());
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    logger.info(
                            "Migrated {} user_authorization grant(s) from {} to role {}",
                            updated,
                            entry.getKey(),
                            entry.getValue());
                }
            }
        }

        Set<String> validRoles = Set.of("ADMIN", "TRESORIER", "ARBITRE", "EVENT_ADMIN");
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM user_authorization WHERE " + valueColumn + " NOT IN (?, ?, ?, ?)")) {
            int index = 1;
            for (String role : validRoles) {
                ps.setString(index++, role);
            }
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                logger.info("Removed {} user_authorization grant(s) with unrecognized values", deleted);
            }
        }

        deduplicateUserAuthorizations(connection, valueColumn);

        if (hasPermissionColumn && !hasRoleColumn) {
            if (isH2()) {
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE user_authorization ALTER COLUMN permission RENAME TO role");
                }
            } else {
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE user_authorization RENAME COLUMN permission TO role");
                }
            }
            logger.info("Renamed user_authorization.permission column to role");
        }

        finalizeUserAuthorizationRoleColumn(connection);
    }

    private void prepareUserAuthorizationRoleColumnForMigration(Connection connection) throws SQLException {
        if (isH2()) {
            for (String column : List.of("PERMISSION", "ROLE")) {
                if (!columnExists(connection, "USER_AUTHORIZATION", column)) {
                    continue;
                }
                if (!isH2EnumColumn(connection, "USER_AUTHORIZATION", column)) {
                    continue;
                }
                String sqlColumn = column.toLowerCase(Locale.ROOT);
                try (Statement st = connection.createStatement()) {
                    st.execute(
                            "ALTER TABLE user_authorization ALTER COLUMN " + sqlColumn + " VARCHAR(64) NOT NULL");
                }
                tableColumnsCache.remove("USER_AUTHORIZATION");
                logger.info("Converted user_authorization.{} from legacy ENUM to VARCHAR(64)", sqlColumn);
                return;
            }
            return;
        }

        for (String column : List.of("permission", "role")) {
            if (!postgresTableHasColumn(connection, "user_authorization", column)) {
                continue;
            }
            String constraintName = "user_authorization_" + column + "_check";
            Set<String> currentValues = readPostgresCheckConstraintEnumValues(connection, constraintName);
            if (currentValues.isEmpty()) {
                continue;
            }
            Set<String> expectedValues = enumValues(RoleCode.class);
            if (currentValues.equals(expectedValues)) {
                return;
            }
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE user_authorization DROP CONSTRAINT IF EXISTS " + constraintName);
            }
            logger.info("Dropped legacy {} check constraint before role migration", constraintName);
            return;
        }
    }

    private void finalizeUserAuthorizationRoleColumn(Connection connection) throws SQLException {
        if (!columnExists(connection, "USER_AUTHORIZATION", "ROLE") || isH2()) {
            return;
        }
        if (!postgresTableHasColumn(connection, "user_authorization", "role")) {
            return;
        }
        String constraintName = "user_authorization_role_check";
        Set<String> expectedValues = enumValues(RoleCode.class);
        Set<String> currentValues = readPostgresCheckConstraintEnumValues(connection, constraintName);
        if (currentValues.equals(expectedValues)) {
            return;
        }
        String enumValues = expectedValues.stream()
                .map(value -> "'" + value.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE user_authorization DROP CONSTRAINT IF EXISTS " + constraintName);
            st.execute("ALTER TABLE user_authorization ADD CONSTRAINT " + constraintName + " CHECK (role IN ("
                    + enumValues
                    + "))");
        }
        logger.info("Updated user_authorization.role check constraint to match RoleCode");
    }

    private void deduplicateUserAuthorizations(Connection connection, String valueColumn) throws SQLException {
        String sql =
                "DELETE FROM user_authorization ua WHERE ua.id NOT IN ("
                        + "SELECT MIN(u2.id) FROM user_authorization u2 "
                        + "GROUP BY u2.email, u2."
                        + valueColumn
                        + ", u2.scope_type, u2.scope_id)";
        try (Statement st = connection.createStatement()) {
            int deleted = st.executeUpdate(sql);
            if (deleted > 0) {
                logger.info("Deduplicated {} user_authorization grant(s)", deleted);
            }
        }
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
