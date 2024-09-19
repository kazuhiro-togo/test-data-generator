# TestDataGenerator

`TestDataGenerator` は、テストデータを生成するためのDSL（Domain Specific Language）を使って、柔軟なデータ生成と多様な出力フォーマットをサポートしています。

## 特徴

- **SQLライクで直感的な構文**: insertInto, columnなど。
- **大量データの生成:** `times` メソッドを使用して、簡単に大量データを生成可能。
- **カスタムデータ生成**: `Faker` を使用してランダムなデータ生成が可能。
- **複数の出力フォーマット**: TABLE（パイプ区切り）、TAB（タブ区切り）、CSV、JSON、INSERT。
- **リファレンス処理**: テーブル間のリレーションシップを簡単に設定可能。

## インストール

### Prerequisites

- Java 8以上
- Maven または Gradle（ビルドツール）

### Mavenを使用する場合

```xml
<dependency>
    <groupId>io.github.kazuhiroTogo</groupId>
    <artifactId>testdatagenerator</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Gradleを使用する場合

```groovy
implementation 'io.github.kazuhiroTogo:testdatagenerator:0.0.1'
```

### 使い方

```java
import com.github.javafaker.Faker;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        var builder = TestDataBuilder.newBuilder()
                .insertInto("Singers")
                .column("SingerId", ColumnValueType.UUID)
                .column("SingerName", ColumnValueType.FULL_NAME)
                .times(2)
                .insertInto("Singers")
                .column("SingerId", ColumnValueType.UUID)
                .column("SingerName", ColumnValueType.NULL)
                .times(1)
                .insertInto("Albums")
                .ref("Singers", "SingerId", "SingerId")
                .column("AlbumId", ColumnValueType.UUID)
                .column("Title", ColumnValueType.TITLE)
                .column("ReleaseDate", ColumnValueType.DATE)
                .times(2)
                .insertInto("Songs")
                .ref("Albums", "SingerId", "SingerId")
                .ref("Albums", "AlbumId", "AlbumId")
                .column("TrackId", ColumnValueType.UUID)
                .column("SongName", ColumnValueType.TITLE)
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

        // 例2: TABフォーマットでタブ区切り
        var tabResult = builder.outputFormat(OutputFormatType.TAB).build();
        System.out.println("=== TAB Format (Tab Delimited) ===");
        System.out.println(tabResult);

        // 例3: CSVフォーマットで出力
        var csvResult = builder.outputFormat(OutputFormatType.CSV).build();
        System.out.println("=== CSV Format ===");
        System.out.println(csvResult);

        // 例4: JSONフォーマットで出力
        var jsonResult = builder.outputFormat(OutputFormatType.JSON).build();
        System.out.println("=== JSON Format ===");
        System.out.println(jsonResult);

        // 例5: INSERTフォーマットで出力
        var insertResult = builder.outputFormat(OutputFormatType.INSERT).build();
        System.out.println("=== INSERT Format ===");
        System.out.println(insertResult);

        // 例6: カスタムSupplierを使用してTitleを生成
        Faker faker = new Faker(Locale.US);
        var dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String customSupplierResult =
                TestDataBuilder.newBuilder()
                        .insertInto("CustomTable")
                        .column("CustomTitle", () -> "固定タイトル-" + faker.book().title())
                        .column("CustomDate", () -> dateFormatter.format(faker.date().past(10, TimeUnit.DAYS)))
                        .times(3)
                        .outputFormat(OutputFormatType.INSERT)
                        .build();

        System.out.println("=== Custom Supplier Example ===");
        System.out.println(customSupplierResult);
    }
}
```
