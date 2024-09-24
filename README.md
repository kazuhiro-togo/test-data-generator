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
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.kazuhiro-togo</groupId>
    <artifactId>testdatagenerator</artifactId>
    <version>v0.0.4</version>
</dependency>
```

### Gradleを使用する場合

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

implementation 'com.github.kazuhiro-togo:test-data-generator:v0.0.4'
```

### 使い方

```java
import com.github.kazuhiroTogo.testdatagenerator.*;
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
INSERT INTO Singers (SingerId, SingerName) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', '森 健');
INSERT INTO Singers (SingerId, SingerName) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', '西村 優那');
INSERT INTO Singers (SingerId, SingerName) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', NULL);

INSERT INTO Albums (SingerId, AlbumId, Title, ReleaseDate) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', '5979abea-fccf-43c3-a6ed-c4d9ca18ffd0', 'Many Waters', '2003-10-20');
INSERT INTO Albums (SingerId, AlbumId, Title, ReleaseDate) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', 'c25dd926-80e2-4e06-a587-13653dbab15a', 'If I Forget Thee Jerusalem', '1970-02-26');
INSERT INTO Albums (SingerId, AlbumId, Title, ReleaseDate) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', '4fff812d-bd22-4444-bc84-4341de07ec77', 'Such, Such Were the Joys', '1988-04-04');
INSERT INTO Albums (SingerId, AlbumId, Title, ReleaseDate) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', 'ee6db50f-faef-40c8-b15d-0c9388c5a9db', 'The Last Enemy', '1964-11-14');
INSERT INTO Albums (SingerId, AlbumId, Title, ReleaseDate) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', 'a49b801c-1cac-4e6d-ac5d-c23401a3c276', 'Blithe Spirit', '1996-09-18');
INSERT INTO Albums (SingerId, AlbumId, Title, ReleaseDate) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', '2b897ee4-9251-46c0-8147-190013647e8d', 'The Glory and the Dream', '1974-01-29');

INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', '5979abea-fccf-43c3-a6ed-c4d9ca18ffd0', '79283ee3-f127-47e1-8f09-c21b88c454eb', '貫く_貫く/こうせい.webm');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', '5979abea-fccf-43c3-a6ed-c4d9ca18ffd0', 'c6c23283-064b-490d-aeff-1d328943cee3', '済ます_じじょでん/閉める.odt');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', 'c25dd926-80e2-4e06-a587-13653dbab15a', '32d690f3-c2c1-4a33-be5d-57427431692a', '血液_屈む/馬.png');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', 'c25dd926-80e2-4e06-a587-13653dbab15a', '1973ad60-ad27-482e-b399-55a907b29ecf', '高値_独裁/殻.flac');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', '4fff812d-bd22-4444-bc84-4341de07ec77', 'e26e1dee-e502-44cd-9dc1-3df27678c0c4', '騎兵_特殊/さいぼう, さいほう.html');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', '4fff812d-bd22-4444-bc84-4341de07ec77', '6d3a084d-5459-4bb6-ae4a-8b8432a813e1', 'いちだい_きょうはんしゃ/かい.odt');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', 'ee6db50f-faef-40c8-b15d-0c9388c5a9db', '67b93a51-cead-4c5b-a448-943c57a62ffc', '超音波_こはん/窓.docx');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', 'ee6db50f-faef-40c8-b15d-0c9388c5a9db', 'a632965f-626e-4711-b2b1-8344a0ad92f0', '大丈夫_凝固/せいぞう.jpeg');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', 'a49b801c-1cac-4e6d-ac5d-c23401a3c276', '7f600016-5bda-4dee-8d8f-2da8a161f308', 'かんしん_れつあく/白菊.gif');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', 'a49b801c-1cac-4e6d-ac5d-c23401a3c276', 'c7222cf1-0b84-4b4f-8ad1-51670329e419', 'きゅうりょう_評価/原油.ppt');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', '2b897ee4-9251-46c0-8147-190013647e8d', '3fbd76e6-c9d4-47f5-9160-3ddac2e19fab', 'かんぜん_あおい/ほうげん.pages');
INSERT INTO Songs (SingerId, AlbumId, TrackId, SongName) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', '2b897ee4-9251-46c0-8147-190013647e8d', 'db8ea043-e470-40d4-8ad7-ff6863d34015', 'こうせい_ひきざん/たいこうする.numbers');

INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', '5979abea-fccf-43c3-a6ed-c4d9ca18ffd0', '79283ee3-f127-47e1-8f09-c21b88c454eb');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', '5979abea-fccf-43c3-a6ed-c4d9ca18ffd0', 'c6c23283-064b-490d-aeff-1d328943cee3');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', 'c25dd926-80e2-4e06-a587-13653dbab15a', '32d690f3-c2c1-4a33-be5d-57427431692a');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('235bcad7-c5d9-4d35-8485-23b49f343d6f', 'c25dd926-80e2-4e06-a587-13653dbab15a', '1973ad60-ad27-482e-b399-55a907b29ecf');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', '4fff812d-bd22-4444-bc84-4341de07ec77', 'e26e1dee-e502-44cd-9dc1-3df27678c0c4');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', '4fff812d-bd22-4444-bc84-4341de07ec77', '6d3a084d-5459-4bb6-ae4a-8b8432a813e1');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', 'ee6db50f-faef-40c8-b15d-0c9388c5a9db', '67b93a51-cead-4c5b-a448-943c57a62ffc');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('e11ebb8f-de62-4ad0-ad06-6b74cb124f01', 'ee6db50f-faef-40c8-b15d-0c9388c5a9db', 'a632965f-626e-4711-b2b1-8344a0ad92f0');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', 'a49b801c-1cac-4e6d-ac5d-c23401a3c276', '7f600016-5bda-4dee-8d8f-2da8a161f308');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', 'a49b801c-1cac-4e6d-ac5d-c23401a3c276', 'c7222cf1-0b84-4b4f-8ad1-51670329e419');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', '2b897ee4-9251-46c0-8147-190013647e8d', '3fbd76e6-c9d4-47f5-9160-3ddac2e19fab');
INSERT INTO ActiveSongs (SingerId, AlbumId, TrackId) VALUES ('ef31cf06-6b6b-4bd9-bf27-f20888a51870', '2b897ee4-9251-46c0-8147-190013647e8d', 'db8ea043-e470-40d4-8ad7-ff6863d34015');
```
