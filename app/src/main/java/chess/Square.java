package chess;

/**
 * Клетка шахматной доски: x — вертикаль (a..h → 1..8), y — горизонталь (1..8).
 */
public record Square(int x, int y) {

    public Square {
        if (x < 1 || x > 8 || y < 1 || y > 8) {
            throw new IllegalArgumentException("Клетка вне доски: (" + x + ", " + y + ")");
        }
    }

    /** Разбор шахматной нотации ("e4", "E4"). */
    public static Square parse(String s) {
        if (s == null || s.length() != 2) {
            throw new IllegalArgumentException("Клетка задаётся буквой a-h и цифрой 1-8: " + s);
        }
        char f = Character.toLowerCase(s.charAt(0));
        char r = s.charAt(1);
        if (f < 'a' || f > 'h' || r < '1' || r > '8') {
            throw new IllegalArgumentException("Клетка задаётся буквой a-h и цифрой 1-8: " + s);
        }
        return new Square(f - 'a' + 1, r - '0');
    }

    /** Обратный перевод: (5, 1) → "e1". */
    public String notation() {
        return "" + (char) ('a' + x - 1) + (char) ('0' + y);
    }

    @Override
    public String toString() {
        return notation();
    }
}
