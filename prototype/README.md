# Sorted Prototype

This is the first local Kotlin prototype for Sorted.

It validates:

- SMS parsing
- Ignore rules
- Merchant normalization
- Category assignment

## Run Fixture Tests

```bash
kotlinc src/main/kotlin src/test/kotlin -include-runtime -d build/parser-tests.jar
java -cp build/parser-tests.jar sorted.ParserFixtureTestKt
```

## Run One Sample

```bash
kotlinc src/main/kotlin -include-runtime -d build/sorted-prototype.jar
java -jar build/sorted-prototype.jar
```

