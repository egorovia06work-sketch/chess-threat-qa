package chess;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("App: аргументы командной строки")
class AppTest {

    private record Result(int code, String out, String err) {}

    private static Result run(String input, String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int code = App.run(args,
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(code, out.toString(StandardCharsets.UTF_8).trim(),
                                err.toString(StandardCharsets.UTF_8).trim());
    }

    @Test @DisplayName("позиционные аргументы: e1 e8 h4")
    void positional() {
        Result r = run("", "e1", "e8", "h4");
        assertEquals(App.OK, r.code());
        assertEquals("Королю угрожают ладья с e8 и слон с h4 (двойной шах).", r.out());
    }

    @Test @DisplayName("именованные ключи в любом порядке: -b h4 -r a8 -k e1")
    void named() {
        Result r = run("", "-b", "h4", "-r", "a8", "-k", "e1");
        assertEquals(App.OK, r.code());
        assertEquals("Королю угрожает слон с h4.", r.out());
    }

    @Test @DisplayName("интерактивный ввод без аргументов")
    void interactive() {
        Result r = run("e1\ne8\na1\n");
        assertEquals(App.OK, r.code());
        assertTrue(r.out().endsWith("Королю угрожает ладья с e8."));
    }

    @Test @DisplayName("справка -h")
    void help() {
        Result r = run("", "-h");
        assertEquals(App.OK, r.code());
        assertTrue(r.out().startsWith("Использование"));
    }

    @Test @DisplayName("не хватает аргументов → код 2")
    void missing() {
        Result r = run("", "e1", "e8");
        assertEquals(App.BAD_ARGS, r.code());
        assertEquals("Ошибка: нужно указать все три клетки.", r.err());
    }

    @Test @DisplayName("неизвестный ключ и лишний аргумент → код 2")
    void badArgs() {
        assertEquals(App.BAD_ARGS, run("", "-x", "e1").code());
        assertEquals(App.BAD_ARGS, run("", "e1", "e8", "h4", "a2").code());
        assertEquals(App.BAD_ARGS, run("", "-k").code());
    }

    @Test @DisplayName("некорректная позиция → код 1")
    void badPosition() {
        Result r1 = run("", "z9", "a1", "b2");
        assertEquals(App.BAD_POSITION, r1.code());
        assertEquals("Ошибка: клетка задаётся буквой a-h и цифрой 1-8.", r1.out());
        Result r2 = run("", "e1", "e1", "a5");
        assertEquals(App.BAD_POSITION, r2.code());
        assertEquals("Ошибка: фигуры не могут стоять на одной клетке.", r2.out());
    }
}
