package chess.qa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Слушатель JUnit Platform: сохраняет результаты каждого запуска в базу данных
 * (таблицы test_run и test_result). Подключается через
 * META-INF/services/org.junit.platform.launcher.TestExecutionListener.
 */
public class ResultRecorder implements TestExecutionListener {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection db;
    private TestPlan plan;
    private long runId = -1;
    private long runStart;
    private int passed, failed, skipped;
    private final Map<String, Long> started = new ConcurrentHashMap<>();

    private boolean enabled() {
        return Boolean.parseBoolean(System.getProperty("qa.record", "false"));
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (!enabled()) return;
        plan = testPlan;
        runStart = System.currentTimeMillis();
        try {
            db = Db.connect();
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO test_run (started_at, java_version) VALUES (?, ?)")) {
                ps.setString(1, LocalDateTime.now().format(TS));
                ps.setString(2, System.getProperty("java.version"));
                ps.executeUpdate();
            }
            try (Statement s = db.createStatement();
                 ResultSet k = s.executeQuery("SELECT last_insert_rowid()")) {
                k.next();
                runId = k.getLong(1);
            }
        } catch (Exception e) {
            System.err.println("ResultRecorder: база недоступна (" + e.getMessage() + ")");
            db = null;
        }
    }

    @Override
    public void executionStarted(TestIdentifier id) {
        if (db != null && id.isTest()) started.put(id.getUniqueId(), System.nanoTime());
    }

    @Override
    public void executionSkipped(TestIdentifier id, String reason) {
        if (db != null && id.isTest()) {
            skipped++;
            save(id, "SKIPPED", 0, reason);
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (db == null || !id.isTest()) return;
        long ms = (System.nanoTime() - started.getOrDefault(id.getUniqueId(), System.nanoTime())) / 1_000_000;
        boolean ok = result.getStatus() == TestExecutionResult.Status.SUCCESSFUL;
        if (ok) passed++; else failed++;
        String msg = result.getThrowable().map(Throwable::getMessage).orElse(null);
        save(id, ok ? "PASSED" : "FAILED", ms, msg);
    }

    /** Тестовый набор — имя ближайшего контейнера (набор из БД или класс тестов). */
    private String suiteOf(TestIdentifier id) {
        return plan.getParent(id).map(TestIdentifier::getDisplayName).orElse("?");
    }

    private void save(TestIdentifier id, String status, long ms, String msg) {
        try (PreparedStatement ps = db.prepareStatement("""
                INSERT INTO test_result (run_id, suite, test_name, status, duration_ms, message)
                VALUES (?, ?, ?, ?, ?, ?)""")) {
            ps.setLong(1, runId);
            ps.setString(2, suiteOf(id));
            ps.setString(3, id.getDisplayName());
            ps.setString(4, status);
            ps.setLong(5, ms);
            ps.setString(6, msg);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("ResultRecorder: " + e.getMessage());
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (db == null) return;
        try (PreparedStatement ps = db.prepareStatement("""
                UPDATE test_run SET finished_at = ?, total = ?, passed = ?, failed = ?,
                       skipped = ?, duration_ms = ? WHERE id = ?""")) {
            ps.setString(1, LocalDateTime.now().format(TS));
            ps.setInt(2, passed + failed + skipped);
            ps.setInt(3, passed);
            ps.setInt(4, failed);
            ps.setInt(5, skipped);
            ps.setLong(6, System.currentTimeMillis() - runStart);
            ps.setLong(7, runId);
            ps.executeUpdate();
            System.out.printf("Запуск #%d сохранён в БД: тестов %d, успешно %d, провалено %d%n",
                              runId, passed + failed + skipped, passed, failed);
            db.close();
        } catch (Exception e) {
            System.err.println("ResultRecorder: " + e.getMessage());
        }
    }
}
