package chess;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ThreatAnalyzer: математическая модель задачи 2.25")
class ThreatAnalyzerTest {

    private static ThreatAnalyzer of(String k, String r, String b) {
        return new ThreatAnalyzer(Square.parse(k), Square.parse(r), Square.parse(b));
    }

    @Nested
    @DisplayName("Основные случаи (тесты 1–4 из отчёта)")
    class Basic {
        @Test @DisplayName("1. ладья на одной вертикали: e1, e8, a1")
        void rook() {
            ThreatAnalyzer a = of("e1", "e8", "a1");
            assertTrue(a.rookThreat());
            assertFalse(a.bishopThreat());
            assertEquals(Threat.ROOK, a.threat());
        }

        @Test @DisplayName("2. слон на диагонали: e1, a8, h4")
        void bishop() {
            ThreatAnalyzer a = of("e1", "a8", "h4");
            assertFalse(a.rookThreat());
            assertTrue(a.bishopThreat());
            assertEquals(Threat.BISHOP, a.threat());
        }

        @Test @DisplayName("3. двойной шах: e1, e8, h4")
        void both() {
            assertEquals(Threat.BOTH, of("e1", "e8", "h4").threat());
        }

        @Test @DisplayName("4. угрозы нет: e1, b8, h8")
        void none() {
            assertEquals(Threat.NONE, of("e1", "b8", "h8").threat());
        }
    }

    @Nested
    @DisplayName("Перекрытие линии атаки (тесты 5–7)")
    class Blocking {
        @Test @DisplayName("5. слон e4 закрывает ладью e8 от короля e1")
        void bishopBlocksRook() {
            ThreatAnalyzer a = of("e1", "e8", "e4");
            assertFalse(a.rookThreat(), "ладья перекрыта");
            assertEquals(Threat.NONE, a.threat());
        }

        @Test @DisplayName("6. ладья c3 закрывает слона a5 от короля e1")
        void rookBlocksBishop() {
            ThreatAnalyzer a = of("e1", "c3", "a5");
            assertFalse(a.bishopThreat(), "слон перекрыт");
            assertEquals(Threat.NONE, a.threat());
        }

        @Test @DisplayName("7. перекрытие по вертикали a: a1, a8, a4")
        void verticalBlock() {
            assertEquals(Threat.NONE, of("a1", "a8", "a4").threat());
        }

        @Test @DisplayName("слон за ладьёй не перекрывает её: e1, e4, e8 → ладья")
        void bishopBehindRook() {
            assertEquals(Threat.ROOK, of("e1", "e4", "e8").threat());
        }
    }

    @Nested
    @DisplayName("Предикат Между(A, C, P)")
    class Between {
        @ParameterizedTest(name = "Между({0}, {1}, {2}) = {3}")
        @DisplayName("Между(A, C, P) — 9 наборов")
        @CsvSource({
            "e1, e8, e4, true",   "e1, e8, e1, false",  "e1, e8, e8, false",
            "e1, e8, d4, false",  "a1, h8, d4, true",   "a1, h8, d5, false",
            "h1, a8, e4, true",   "a1, h1, c1, true",   "a1, b2, a1, false"})
        void between(String a, String c, String p, boolean expected) {
            assertEquals(expected,
                ThreatAnalyzer.isBetween(Square.parse(a), Square.parse(c), Square.parse(p)));
        }
    }

    @ParameterizedTest(name = "[{index}] K={0} R={1} B={2} → {3}")
    @CsvFileSource(resources = "/positions.csv", numLinesToSkip = 1)
    @DisplayName("Набор позиций из файла positions.csv")
    void positionsFromFile(String k, String r, String b, Threat expected) {
        assertEquals(expected, of(k, r, b).threat());
    }

    @Test
    @DisplayName("фигуры на одной клетке → исключение")
    void sameSquare() {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> of("e1", "e1", "a5"));
        assertTrue(e.getMessage().contains("одной клетке"));
    }

    @Test
    @DisplayName("describe() содержит клетку атакующей фигуры")
    void describe() {
        assertEquals("Королю угрожает ладья с e8.", of("e1", "e8", "a1").describe());
        assertEquals("Королю угрожает слон с h4.", of("e1", "a8", "h4").describe());
        assertEquals("Королю угрожают ладья с e8 и слон с h4 (двойной шах).",
                     of("e1", "e8", "h4").describe());
        assertEquals("Угрозы королю нет.", of("e1", "b8", "h8").describe());
    }
}
