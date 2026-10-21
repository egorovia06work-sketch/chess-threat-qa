# chess-threat-qa — модульное тестирование с данными из базы данных

Задача 2.25 (А. Г. Юркин): белый король, чёрная ладья и слон — есть ли угроза королю и от кого.

* тестовые наборы хранятся в базе данных SQLite (`qa/schema.sql`, `qa/seed.sql`);
* тесты JUnit 5 получают данные параметрическим SQL-запросом (`@MethodSource`, `@TestFactory`);
* результаты каждого запуска и метрики кода (покрытие, цикломатическая сложность — JaCoCo)
  сохраняются в ту же базу данных.

## Запуск (нужна JDK 17+)

```bash
./gradlew initDb                       # создать базу и загрузить тестовые наборы
./gradlew test                         # тесты + отчёт JaCoCo + запись результатов в БД
./gradlew test -PincludeBroken=true    # добавить набор с заведомо ложными ожиданиями
./gradlew qaHistory                    # история запусков из БД
```

Windows: `gradlew.bat ...`, перед запуском `chcp 65001`.

## Отчёты

| Что | Где |
|---|---|
| отчёт о тестах (HTML) | `app/build/reports/tests/test/index.html` |
| покрытие и сложность (HTML) | `app/build/reports/jacoco/test/html/index.html` |
| база данных QA | `qa/qa.db` (таблицы `test_suite`, `test_case`, `test_run`, `test_result`, `method_metric`, представление `v_suite_stats`) |

Автор: Егоров И., группа 4ИБ2, ITHub. Преподаватель: Сухов А. А.
