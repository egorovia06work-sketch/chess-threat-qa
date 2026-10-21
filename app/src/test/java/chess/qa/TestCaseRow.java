package chess.qa;

/** Строка таблицы test_case. */
public record TestCaseRow(int id, String suite, String king, String rook, String bishop,
                          String expected, String comment) {

    @Override
    public String toString() {
        return "#" + id + " K=" + king + " R=" + rook + " B=" + bishop + " → " + expected
               + " (" + comment + ")";
    }
}
