# API Reference

Use Dokka output when you need a fuller view of the public Android SDK surface.

## Generate the Docs

From the repository root:

```bash
./gradlew :fusabase:dokkaHtml
```

That generates the Dokka site for the Android library module.

## Output Location

The default Dokka HTML output for this module is written under:

```text
fusabase/build/dokka/html/
```

## Notes

- The active consumer SDK module in this repository is `:fusabase`.
- Dokka is configured directly in `fusabase/build.gradle.kts`.
- The Dokka config suppresses several internal implementation packages such as `core`, `http`, `logger`, `models`, and `utils`, which is a good signal for what application code should avoid importing directly.

## Useful Local Source Anchors

- `settings.gradle.kts`
- `fusabase/build.gradle.kts`
- `fusabase/src/main/java/com/oracle/mobile/fusabase/`
- `fusabase/src/main/java/com/oracle/mobile/fusabase/auth/`
- `fusabase/src/main/java/com/oracle/mobile/fusabase/oracledb/`
- `fusabase/src/main/java/com/oracle/mobile/fusabase/storage/`
- `fusabase/src/main/java/com/oracle/mobile/fusabase/task/`
