# TestDataGenerator

`TestDataGenerator` は、テストデータを生成するためのDSL（Domain Specific Language）を使って、柔軟なデータ生成と多様な出力フォーマットをサポートしています。

## 特徴

- **SQLライクで直感的な構文**: insertInto, columnなど。
- **大量データの生成:** `times` メソッドを使用して、簡単に大量データを生成可能。
- **カスタムデータ生成**: `Faker` を使用してランダムなデータ生成が可能。
- **複数の出力フォーマット**: TABLE（パイプ区切り）、TSV（タブ区切り）、CSV、INSERT。
- **リファレンス処理**: テーブル間のリレーションシップを簡単に設定可能。

## インストール

### Prerequisites

- Java 8以上
- Maven または Gradle（ビルドツール）

### Mavenを使用する場合

```xml
<dependency>
    <groupId>com.github.kazuhiro-togo</groupId>
    <artifactId>testdatagenerator</artifactId>
    <version>v0.0.2</version>
</dependency>
```

### Gradleを使用する場合

```groovy
implementation 'com.github.kazuhiro-togo:test-data-generator:v0.0.2'
```

### 使い方

```java
import com.github.javafaker.Faker;

import java.time.LocalDateTime;
import java.util.Locale;
import java.time.LocalDate;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Faker faker = new Faker(Locale.JAPAN, new Random(1234L));
        var builder = TestDataBuilder.newBuilder(faker)
                .insertInto("Singers")
                .column("SingerId", ColumnType.UUID)
                .column("SingerName", ColumnType.STRING)
                .times(2)
                .insertInto("Singers")
                .column("SingerId", ColumnType.UUID)
                .column("SingerName", ColumnType.NULL)
                .times(1)

                .insertInto("Albums")
                .ref("Singers", "SingerId", "SingerId")
                .column("AlbumId", ColumnType.UUID)
                .column("Title", () -> faker.book().title())
                .column("ReleaseDate", ColumnType.DATE)
                .times(2)

                .insertInto("Songs")
                .ref("Albums", "SingerId", "SingerId")
                .ref("Albums", "AlbumId", "AlbumId")
                .column("TrackId", ColumnType.UUID)
                .column("SongName", () -> faker.file().fileName())
                .times(2)

                .insertInto("ActiveSongs")
                .ref("Songs", "SingerId", "SingerId")
                .ref("Songs", "AlbumId", "AlbumId")
                .ref("Songs", "TrackId", "TrackId")
                .times(1);

        // 例1: TABLEフォーマットで '|' 区切り
        var tableResult = builder.outputFormat(OutputFormatType.TABLE).build();
        System.out.println("=== TABLE Format (| Delimited) ===");
        System.out.println(tableResult);

        // 例2: TSVフォーマットでタブ区切り
        var tabResult = builder.outputFormat(OutputFormatType.TSV).build();
        System.out.println("=== TSV Format (Tab Delimited) ===");
        System.out.println(tabResult);

        // 例3: CSVフォーマットで出力
        var csvResult = builder.outputFormat(OutputFormatType.CSV).build();
        System.out.println("=== CSV Format ===");
        System.out.println(csvResult);

        // 例4: INSERTフォーマットで出力
        var insertResult = builder.outputFormat(OutputFormatType.INSERT).build();
        System.out.println("=== INSERT Format ===");
        System.out.println(insertResult);

        // 例5: カスタムSupplierを使用して生成
        Faker fakerUS = new Faker(Locale.US);
        String customSupplierResult =
                TestDataBuilder.newBuilder(fakerUS)
                        .insertInto("CustomTable")
                        .column("CustomTitle", () -> "固定タイトル-" + faker.book().title())
                        .column("CustomDate", () -> LocalDate.now().plusDays(faker.number().numberBetween(1, 100)))
                        .column("CustomDateTime", () -> LocalDateTime.now().plusDays(faker.number().numberBetween(1, 100)))
                        .column("CustomDouble", () -> faker.number().randomDouble(2, 1, 100))
                        .column("CustomInt", () -> faker.number().numberBetween(1, 100))
                        .times(3)
                        .outputFormat(OutputFormatType.INSERT)
                        .build();

        System.out.println("=== Custom Supplier Example ===");
        System.out.println(customSupplierResult);
    }
}
```

INSERTフォーマットで出力例
```sql
=== INSERT Format ===
INSERT INTO Singers (SingerId, SingerName) VALUES ('15810360-7125-4ea6-be7a-357200db81b6', '森 健');
INSERT INTO Singers (SingerId, SingerName) VALUES ('208cf6b6-ebf4-4978-bed0-ef27bd59f51f', '西村 優那');
INSERT INTO Singers (SingerId, SingerName) VALUES ('cea18fce-f5ce-465a-9331-d2ec0aa4d094', NULL);
```
