package chess.qa;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Создаёт схему и загружает тестовые данные: gradle initDb.
 * Аргументы: schema.sql seed.sql. История запусков (test_run) сохраняется.
 */
public final class DbInit {

    public static void main(String[] args) throws Exception {
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            Db.runScript(c, Path.of(args[0]));
            Db.runScript(c, Path.of(args[1]));
            c.commit();
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("""
                     SELECT s.name, s.enabled, COUNT(c.id)
                     FROM test_suite s LEFT JOIN test_case c ON c.suite_id = s.id
                     GROUP BY s.id ORDER BY s.id""")) {
                System.out.println("База данных: " + Db.url());
                while (r.next()) {
                    System.out.printf("  набор %-9s %2d тестов%s%n", r.getString(1), r.getInt(3),
                                      r.getInt(2) == 1 ? "" : "  (отключён)");
                }
            }
        }
    }
}
