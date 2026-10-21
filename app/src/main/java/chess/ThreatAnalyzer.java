package chess;

/**
 * Математическая модель задачи 2.25 (А. Г. Юркин): угрожают ли белому королю
 * чёрная ладья и чёрный слон, с учётом перекрытия линии атаки.
 */
public final class ThreatAnalyzer {

    private final Square king;
    private final Square rook;
    private final Square bishop;

    public ThreatAnalyzer(Square king, Square rook, Square bishop) {
        if (king.equals(rook) || king.equals(bishop) || rook.equals(bishop)) {
            throw new IllegalArgumentException("Фигуры не могут стоять на одной клетке.");
        }
        this.king = king;
        this.rook = rook;
        this.bishop = bishop;
    }

    /** Лежит ли клетка p строго между a и c (a и c на одной линии). */
    public static boolean isBetween(Square a, Square c, Square p) {
        int dx = c.x() - a.x(), dy = c.y() - a.y();
        int sx = Integer.signum(dx), sy = Integer.signum(dy);
        int n = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 1; i < n; i++) {
            if (a.x() + i * sx == p.x() && a.y() + i * sy == p.y()) {
                return true;
            }
        }
        return false;
    }

    /** TR = ((xk = xr) ∨ (yk = yr)) ∧ ¬Между(R, K, B). */
    public boolean rookThreat() {
        boolean line = king.x() == rook.x() || king.y() == rook.y();
        return line && !isBetween(rook, king, bishop);
    }

    /** TB = (|xk − xb| = |yk − yb|) ∧ ¬Между(B, K, R). */
    public boolean bishopThreat() {
        boolean diag = Math.abs(king.x() - bishop.x()) == Math.abs(king.y() - bishop.y());
        return diag && !isBetween(bishop, king, rook);
    }

    public Threat threat() {
        boolean tr = rookThreat(), tb = bishopThreat();
        if (tr && tb) return Threat.BOTH;
        if (tr) return Threat.ROOK;
        if (tb) return Threat.BISHOP;
        return Threat.NONE;
    }

    public String describe() {
        return switch (threat()) {
            case BOTH -> "Королю угрожают ладья с " + rook + " и слон с " + bishop + " (двойной шах).";
            case ROOK -> "Королю угрожает ладья с " + rook + ".";
            case BISHOP -> "Королю угрожает слон с " + bishop + ".";
            case NONE -> "Угрозы королю нет.";
        };
    }
}
