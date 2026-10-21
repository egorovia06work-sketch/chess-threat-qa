-- Схема базы данных QA для задачи 2.25 (SQLite).
-- Тестовые данные: test_suite, test_case. Результаты: test_run, test_result, method_metric.

CREATE TABLE IF NOT EXISTS test_suite (
    id          INTEGER PRIMARY KEY,
    name        TEXT    NOT NULL UNIQUE,
    description TEXT,
    enabled     INTEGER NOT NULL DEFAULT 1          -- 0: набор не запускается по умолчанию
);

CREATE TABLE IF NOT EXISTS test_case (
    id       INTEGER PRIMARY KEY,
    suite_id INTEGER NOT NULL REFERENCES test_suite(id),
    king     TEXT    NOT NULL,
    rook     TEXT    NOT NULL,
    bishop   TEXT    NOT NULL,
    expected TEXT    NOT NULL CHECK (expected IN ('NONE', 'ROOK', 'BISHOP', 'BOTH', 'ERROR')),
    comment  TEXT
);

-- Один запуск тестов (gradle test)
CREATE TABLE IF NOT EXISTS test_run (
    id                   INTEGER PRIMARY KEY,
    started_at           TEXT    NOT NULL,           -- дата и время начала (ISO 8601)
    finished_at          TEXT,
    total                INTEGER,
    passed               INTEGER,
    failed               INTEGER,
    skipped              INTEGER,
    duration_ms          INTEGER,                    -- время выполнения
    instruction_coverage REAL,                       -- покрытие кода, % (JaCoCo)
    branch_coverage      REAL,
    line_coverage        REAL,
    complexity           INTEGER,                    -- суммарная цикломатическая сложность
    max_complexity       INTEGER,                    -- максимальная сложность метода
    java_version         TEXT
);

-- Результат каждого теста
CREATE TABLE IF NOT EXISTS test_result (
    id          INTEGER PRIMARY KEY,
    run_id      INTEGER NOT NULL REFERENCES test_run(id),
    suite       TEXT,                                -- тестовый набор
    test_name   TEXT    NOT NULL,
    status      TEXT    NOT NULL CHECK (status IN ('PASSED', 'FAILED', 'SKIPPED')),
    duration_ms INTEGER,
    message     TEXT
);

-- Метрики методов программы (JaCoCo): сложность и покрытие
CREATE TABLE IF NOT EXISTS method_metric (
    run_id        INTEGER NOT NULL REFERENCES test_run(id),
    class_name    TEXT    NOT NULL,
    method_name   TEXT    NOT NULL,
    complexity    INTEGER,
    line_coverage REAL
);

-- Статистика по тестовым наборам в каждом запуске
CREATE VIEW IF NOT EXISTS v_suite_stats AS
SELECT r.id                            AS run_id,
       r.started_at                    AS started_at,
       t.suite                         AS suite,
       COUNT(*)                        AS total,
       SUM(t.status = 'PASSED')        AS passed,
       SUM(t.status = 'FAILED')        AS failed,
       SUM(t.duration_ms)              AS duration_ms
FROM test_result t JOIN test_run r ON r.id = t.run_id
GROUP BY r.id, t.suite;
