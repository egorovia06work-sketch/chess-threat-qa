package chess.qa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** История запусков из базы данных: gradle qaHistory. */
public final class QaHistory {

    public static void main(String[] args) throws Exception {
        try (Connection c = Db.connect(); Statement s = c.createStatement()) {
            System.out.println("Запуски тестов (test_run):");
            System.out.printf("%3s  %-19s %5s %5s %5s %7s %7s %7s %5s%n",
                "#", "дата", "всего", "успех", "провал", "мс", "строки", "ветви", "слож");
            try (ResultSet r = s.executeQuery("""
                    SELECT id, started_at, total, passed, failed, duration_ms,
                           line_coverage, branch_coverage, complexity
                    FROM test_run ORDER BY id""")) {
                while (r.next()) {
                    System.out.printf("%3d  %-19s %5d %5d %6d %7d %6.1f%% %6.1f%% %5d%n",
                        r.getInt(1), r.getString(2), r.getInt(3), r.getInt(4), r.getInt(5),
                        r.getLong(6), r.getDouble(7), r.getDouble(8), r.getInt(9));
                }
            }
            System.out.println();
            System.out.println("Последний запуск по тестовым наборам (v_suite_stats):");
            try (ResultSet r = s.executeQuery("""
                    SELECT suite, total, passed, failed, duration_ms FROM v_suite_stats
                    WHERE run_id = (SELECT MAX(id) FROM test_run) ORDER BY suite""")) {
                while (r.next()) {
                    System.out.printf("  %-45s всего %3d, успех %3d, провал %2d, %4d мс%n",
                        r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getLong(5));
                }
            }
        }
    }
}
