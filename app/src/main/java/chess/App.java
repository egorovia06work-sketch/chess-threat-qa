package chess;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Консольная программа. Запуск:
 *   app e1 e8 h4             — король, ладья, слон
 *   app -k e1 -r e8 -b h4    — именованные ключи
 *   app -h                   — справка
 *   app                      — интерактивный ввод
 */
public final class App {

    static final int OK = 0, BAD_POSITION = 1, BAD_ARGS = 2;

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, System.err));
    }

    static void usage(PrintStream out) {
        out.println("Использование: app [КОРОЛЬ ЛАДЬЯ СЛОН]");
        out.println("               app -k КОРОЛЬ -r ЛАДЬЯ -b СЛОН");
        out.println("  -h, --help     справка");
        out.println("Клетки в шахматной нотации: e1, a8, h4.");
        out.println("Без аргументов позиции вводятся с клавиатуры.");
    }

    /** Вся логика main, вынесенная для модульного тестирования. */
    static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        String[] pos = new String[3];   // король, ладья, слон
        int positional = 0;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "-h", "--help" -> { usage(out); return OK; }
                case "-k", "-r", "-b" -> {
                    if (i + 1 >= args.length) {
                        err.println("Ошибка: после " + a + " нужна клетка.");
                        return BAD_ARGS;
                    }
                    pos[a.equals("-k") ? 0 : a.equals("-r") ? 1 : 2] = args[++i];
                }
                default -> {
                    if (a.startsWith("-")) {
                        err.println("Ошибка: неизвестный ключ " + a);
                        return BAD_ARGS;
                    }
                    if (positional >= 3) {
                        err.println("Ошибка: лишний аргумент " + a);
                        return BAD_ARGS;
                    }
                    pos[positional++] = a;
                }
            }
        }

        if (args.length == 0) {
            Scanner sc = new Scanner(in);
            String[] prompts = {"белого короля (например, e1)",
                                "чёрной ладьи (например, a8)",
                                "чёрного слона (например, h4)"};
            for (int i = 0; i < 3; i++) {
                out.print("Введите позицию " + prompts[i] + ": ");
                pos[i] = sc.hasNext() ? sc.next() : "";
            }
        } else if (pos[0] == null || pos[1] == null || pos[2] == null) {
            err.println("Ошибка: нужно указать все три клетки.");
            return BAD_ARGS;
        }

        try {
            Square k = Square.parse(pos[0]), r = Square.parse(pos[1]), b = Square.parse(pos[2]);
            out.println(new ThreatAnalyzer(k, r, b).describe());
            return OK;
        } catch (IllegalArgumentException e) {
            out.println("Ошибка: " + firstLower(e.getMessage().replaceFirst(":.*$", ".")));
            return BAD_POSITION;
        }
    }

    private static String firstLower(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
