package chess;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Square: разбор шахматной нотации")
class SquareTest {

    @ParameterizedTest(name = "{0} → ({1}, {2})")
    @CsvSource({"a1, 1, 1", "e1, 5, 1", "h8, 8, 8", "E4, 5, 4", "d7, 4, 7"})
    @DisplayName("корректные клетки переводятся в координаты")
    void parsesValid(String s, int x, int y) {
        Square sq = Square.parse(s);
        assertAll(
            () -> assertEquals(x, sq.x(), "вертикаль"),
            () -> assertEquals(y, sq.y(), "горизонталь"));
    }

    @ParameterizedTest(name = "«{0}» отклоняется")
    @ValueSource(strings = {"z9", "i1", "a0", "a9", "e", "e10", "11", "ee"})
    @NullAndEmptySource
    @DisplayName("некорректная нотация вызывает исключение")
    void rejectsInvalid(String s) {
        assertThrows(IllegalArgumentException.class, () -> Square.parse(s));
    }

    @Test
    @DisplayName("координаты вне доски вызывают исключение")
    void rejectsOutOfBoard() {
        assertThrows(IllegalArgumentException.class, () -> new Square(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Square(3, 9));
    }

    @Test
    @DisplayName("обратный перевод: notation(parse(s)) = s")
    void roundTrip() {
        for (char f = 'a'; f <= 'h'; f++)
            for (char r = '1'; r <= '8'; r++) {
                String s = "" + f + r;
                assertEquals(s, Square.parse(s).notation());
            }
    }

    @Test
    @DisplayName("клетки с одинаковыми координатами равны")
    void equality() {
        assertEquals(Square.parse("e4"), new Square(5, 4));
        assertNotEquals(Square.parse("e4"), Square.parse("e5"));
    }
}
