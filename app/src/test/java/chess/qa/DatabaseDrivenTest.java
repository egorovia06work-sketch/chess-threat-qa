package chess.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import chess.Square;
import chess.Threat;
import chess.ThreatAnalyzer;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Тесты, данные для которых берутся из базы данных QA. */
@DisplayName("Тесты из базы данных")
class DatabaseDrivenTest {

    /** Проверка одной позиции: ожидаемый Threat или исключение (ERROR). */
    static void check(TestCaseRow t) {
        if (t.expected().equals("ERROR")) {
            assertThrows(IllegalArgumentException.class,
                () -> new ThreatAnalyzer(Square.parse(t.king()), Square.parse(t.rook()),
                                         Square.parse(t.bishop())),
                "ожидалась ошибка: " + t.comment());
            return;
        }
        ThreatAnalyzer a = new ThreatAnalyzer(
            Square.parse(t.king()), Square.parse(t.rook()), Square.parse(t.bishop()));
        assertEquals(Threat.valueOf(t.expected()), a.threat(), t.comment());
    }

    // ---- 1. @MethodSource: поток аргументов из параметрического SQL-запроса ----

    static Stream<Arguments> basicSuite() throws SQLException {
        return TestData.cases("basic").stream()
            .map(t -> Arguments.of(t.king(), t.rook(), t.bishop(), t.expected(), t.comment()));
    }

    @ParameterizedTest(name = "K={0} R={1} B={2} → {3} ({4})")
    @MethodSource("basicSuite")
    @DisplayName("Набор basic через @MethodSource")
    void basicFromDb(String k, String r, String b, String expected, String comment) {
        assertEquals(Threat.valueOf(expected),
            new ThreatAnalyzer(Square.parse(k), Square.parse(r), Square.parse(b)).threat(),
            comment);
    }

    // ---- 2. @TestFactory: группа наборов, каждый набор — контейнер тестов ----

    @TestFactory
    @DisplayName("Все наборы из таблицы test_suite")
    Stream<DynamicContainer> allSuites() throws SQLException {
        var suites = TestData.suites();
        assertFalse(suites.isEmpty(), "в базе нет тестовых наборов — выполните gradle initDb");
        return suites.stream().map(name -> {
            try {
                return DynamicContainer.dynamicContainer(name,
                    TestData.cases(name).stream().map(t ->
                        DynamicTest.dynamicTest(t.toString(), () -> check(t))));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
