package chess.qa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Подключение к базе данных QA. Адрес задаётся свойством qa.db.url. */
public final class Db {

    private Db() {}

    public static String url() {
        return System.getProperty("qa.db.url", "jdbc:sqlite:qa/qa.db");
    }

    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /** Выполняет SQL-скрипт; команды разделены «;» в конце строки. */
    public static void runScript(Connection c, Path script) throws IOException, SQLException {
        StringBuilder sql = new StringBuilder();
        try (Statement s = c.createStatement()) {
            for (String line : Files.readAllLines(script, StandardCharsets.UTF_8)) {
                String t = line.strip();
                if (t.startsWith("--") || t.isEmpty()) continue;
                sql.append(line).append('\n');
                if (t.endsWith(";")) {
                    s.execute(sql.toString());
                    sql.setLength(0);
                }
            }
        }
    }
}
