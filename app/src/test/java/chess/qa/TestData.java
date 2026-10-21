package chess.qa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Чтение тестовых наборов из базы данных. */
public final class TestData {

    private TestData() {}

    /** Набор broken (заведомо ложный) включается свойством -DincludeBroken=true. */
    static boolean includeBroken() {
        return Boolean.parseBoolean(System.getProperty("qa.includeBroken", "false"));
    }

    /** Имена наборов, которые нужно запустить. */
    public static List<String> suites() throws SQLException {
        String sql = "SELECT name FROM test_suite WHERE enabled = 1 OR ? ORDER BY id";
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, includeBroken());
            List<String> names = new ArrayList<>();
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) names.add(r.getString(1));
            }
            return names;
        }
    }

    /** Параметрический запрос: все тесты набора suite. */
    public static List<TestCaseRow> cases(String suite) throws SQLException {
        String sql = """
            SELECT c.id, s.name, c.king, c.rook, c.bishop, c.expected, c.comment
            FROM test_case c JOIN test_suite s ON s.id = c.suite_id
            WHERE s.name = ?
            ORDER BY c.id""";
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, suite);
            List<TestCaseRow> rows = new ArrayList<>();
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    rows.add(new TestCaseRow(r.getInt(1), r.getString(2), r.getString(3),
                        r.getString(4), r.getString(5), r.getString(6), r.getString(7)));
                }
            }
            return rows;
        }
    }
}
