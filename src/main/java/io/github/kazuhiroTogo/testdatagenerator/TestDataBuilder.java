package io.github.kazuhiroTogo.testdatagenerator;

import com.github.javafaker.Faker;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * DSL Builder for generating test data.
 */
public class TestDataBuilder {
    private List<InsertConfig> insertConfigs = new ArrayList<>();
    private OutputFormatType outputFormat = OutputFormatType.TABLE; // Default format
    private Faker faker;
    private SimpleDateFormat dateFormatter;

    private TestDataBuilder() {
        this.faker = new Faker(Locale.JAPAN);
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    }

    /**
     * Creates a new builder instance with default Faker and DateFormatter.
     *
     * @return New builder instance.
     */
    public static TestDataBuilder newBuilder() {
        return new TestDataBuilder();
    }

    /**
     * Creates a new builder instance with the specified Faker instance.
     *
     * @param faker Faker instance to use.
     * @return New builder instance.
     */
    public static TestDataBuilder newBuilder(Faker faker) {
        TestDataBuilder builder = new TestDataBuilder();
        builder.faker = faker;
        return builder;
    }

    /**
     * Creates a new builder instance with the specified Faker instance and DateFormatter.
     *
     * @param faker Faker instance to use.
     * @param dateFormatter DateFormatter instance to use.
     * @return New builder instance.
     */
    public static TestDataBuilder newBuilder(Faker faker, SimpleDateFormat dateFormatter) {
        TestDataBuilder builder = new TestDataBuilder();
        builder.faker = faker;
        builder.dateFormatter = dateFormatter;
        return builder;
    }

    /**
     * Starts inserting into a new table.
     *
     * @param tableName Name of the table.
     * @return InsertIntoBuilder for chaining.
     */
    public InsertIntoBuilder insertInto(String tableName) {
        if (tableName == null) {
            throw new IllegalArgumentException("Table name cannot be null.");
        }
        InsertConfig config = new InsertConfig(tableName);
        insertConfigs.add(config);
        return new InsertIntoBuilder(this, config);
    }

    /**
     * Sets the output format.
     *
     * @param format Output format (e.g., OutputFormatType.TABLE).
     * @return Builder instance for chaining.
     */
    public TestDataBuilder outputFormat(OutputFormatType format) {
        if (format == null) {
            throw new IllegalArgumentException("OutputFormatType cannot be null.");
        }
        this.outputFormat = format;
        return this;
    }

    /**
     * Builds the final result based on the configurations.
     *
     * @return Generated data as a string in the specified format.
     */
    public String build() {
        // Generate data based on insertConfigs
        Map<String, List<Map<String, String>>> database = new LinkedHashMap<>();

        for (InsertConfig config : insertConfigs) {
            List<Map<String, String>> table = database.computeIfAbsent(config.tableName, k -> new ArrayList<>());

            if (config.references.isEmpty()) {
                // No references; generate 'times' number of rows
                for (int i = 0; i < config.times; i++) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (Column column : config.columns) {
                        String value = column.valueSupplier.get();
                        row.put(column.name, value != null ? value : "NULL");
                    }
                    table.add(row);
                }
            } else {
                // Has references; ensure all references point to the same table
                Set<String> refTables = config.references.stream()
                        .map(ref -> ref.refTable)
                        .collect(Collectors.toSet());

                if (refTables.size() != 1) {
                    throw new UnsupportedOperationException("All references must point to the same table.");
                }

                String refTable = refTables.iterator().next();
                List<Map<String, String>> refTableData = database.get(refTable);
                if (refTableData == null || refTableData.isEmpty()) {
                    throw new IllegalStateException("Reference table '" + refTable + "' has no data.");
                }

                for (Map<String, String> refRow : refTableData) {
                    for (int i = 0; i < config.times; i++) {
                        Map<String, String> row = new LinkedHashMap<>();
                        // Set all reference columns
                        for (Reference ref : config.references) {
                            row.put(ref.targetColumn, refRow.get(ref.refColumn));
                        }
                        // Set other columns
                        for (Column column : config.columns) {
                            String value = column.valueSupplier.get();
                            row.put(column.name, value != null ? value : "NULL");
                        }
                        table.add(row);
                    }
                }
            }
        }

        // Generate output based on format
        switch (outputFormat) {
            case TABLE:
                return generatePipeTableOutput(database);
            case TAB:
                return generateTabTableOutput(database);
            case CSV:
                return generateCsvOutput(database);
            case JSON:
                return generateJsonOutput(database);
            case INSERT:
                return generateInsertOutput(database);
            default:
                return "Unsupported format: " + outputFormat;
        }
    }

    /**
     * Generates a pipe-separated table string output.
     *
     * @param database Map of table names to their data.
     * @return Pipe-separated table formatted string.
     */
    private String generatePipeTableOutput(Map<String, List<Map<String, String>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            sb.append("Table: ").append(tableName).append("\n");
            List<Map<String, String>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                sb.append("(No data)\n\n");
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, String> row : rows) {
                columns.addAll(row.keySet());
            }

            // Header
            sb.append(String.join(" | ", columns)).append("\n");

            // Separator
            sb.append(columns.stream().map(col -> "---").collect(Collectors.joining(" | "))).append("\n");

            // Rows
            for (Map<String, String> row : rows) {
                List<String> values = columns.stream()
                        .map(col -> row.getOrDefault(col, "NULL"))
                        .collect(Collectors.toList());
                sb.append(String.join(" | ", values)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates a tab-separated table string output.
     *
     * @param database Map of table names to their data.
     * @return Tab-separated table formatted string.
     */
    private String generateTabTableOutput(Map<String, List<Map<String, String>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            sb.append("Table: ").append(tableName).append("\n");
            List<Map<String, String>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                sb.append("(No data)\n\n");
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, String> row : rows) {
                columns.addAll(row.keySet());
            }

            // Header
            sb.append(String.join("\t", columns)).append("\n");

            // Separator
            sb.append(columns.stream().map(col -> "----").collect(Collectors.joining("\t"))).append("\n");

            // Rows
            for (Map<String, String> row : rows) {
                List<String> values = columns.stream()
                        .map(col -> row.getOrDefault(col, "NULL"))
                        .collect(Collectors.toList());
                sb.append(String.join("\t", values)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates a CSV formatted string output.
     *
     * @param database Map of table names to their data.
     * @return CSV formatted string.
     */
    private String generateCsvOutput(Map<String, List<Map<String, String>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            sb.append("Table: ").append(tableName).append("\n");
            List<Map<String, String>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                sb.append("(No data)\n\n");
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, String> row : rows) {
                columns.addAll(row.keySet());
            }

            // Header
            sb.append(String.join(",", columns)).append("\n");

            // Rows
            for (Map<String, String> row : rows) {
                List<String> values = columns.stream()
                        .map(col -> escapeCsv(row.getOrDefault(col, "NULL")))
                        .collect(Collectors.toList());
                sb.append(String.join(",", values)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Escapes CSV values by wrapping them in quotes if they contain commas or quotes.
     *
     * @param value The CSV field value.
     * @return Escaped CSV field value.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    /**
     * Generates a JSON formatted string output.
     *
     * @param database Map of table names to their data.
     * @return JSON formatted string.
     */
    private String generateJsonOutput(Map<String, List<Map<String, String>>> database) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int tableCount = 0;
        int totalTables = database.size();
        for (String tableName : database.keySet()) {
            sb.append("  \"").append(tableName).append("\": [\n");
            List<Map<String, String>> rows = database.get(tableName);
            int rowCount = 0;
            int totalRows = rows.size();
            for (Map<String, String> row : rows) {
                sb.append("    {\n");
                int colCount = 0;
                int totalCols = row.size();
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    sb.append("      \"").append(entry.getKey()).append("\": ");
                    if (entry.getValue() == null) {
                        sb.append("null");
                    } else {
                        sb.append("\"").append(escapeJson(entry.getValue())).append("\"");
                    }
                    if (++colCount < totalCols) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("    }");
                if (++rowCount < totalRows) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]");
            if (++tableCount < totalTables) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escapes JSON special characters in a string.
     *
     * @param value The JSON field value.
     * @return Escaped JSON field value.
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Generates SQL INSERT statements as a string output.
     *
     * @param database Map of table names to their data.
     * @return SQL INSERT statements.
     */
    private String generateInsertOutput(Map<String, List<Map<String, String>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            List<Map<String, String>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, String> row : rows) {
                columns.addAll(row.keySet());
            }

            // Generate INSERT statements
            for (Map<String, String> row : rows) {
                sb.append("INSERT INTO ").append(tableName).append(" (");
                sb.append(String.join(", ", columns));
                sb.append(") VALUES (");
                List<String> values = columns.stream()
                        .map(col -> formatSqlValue(row.get(col)))
                        .collect(Collectors.toList());
                sb.append(String.join(", ", values));
                sb.append(");\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Formats a SQL value, adding quotes around strings and handling NULLs.
     *
     * @param value The value to format.
     * @return Formatted SQL value.
     */
    private String formatSqlValue(String value) {
        if (value == null) {
            return "NULL";
        }
        // Check if the value is a valid UUID or date, or else treat it as a string
        if (isValidUUID(value) || isValidDate(value)) {
            return "'" + value + "'";
        }
        // Escape single quotes in strings
        String escaped = value.replace("'", "''");
        return "'" + escaped + "'";
    }

    /**
     * Validates if a string is a valid UUID.
     *
     * @param value The string to validate.
     * @return True if valid UUID, else false.
     */
    private boolean isValidUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates if a string is a valid date in the specified format.
     *
     * @param value The string to validate.
     * @return True if valid date, else false.
     */
    private boolean isValidDate(String value) {
        try {
            dateFormatter.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Inner Builder Class for InsertInto.
     */
    public class InsertIntoBuilder {
        private TestDataBuilder parentBuilder;
        private InsertConfig currentConfig;

        public InsertIntoBuilder(TestDataBuilder parentBuilder, InsertConfig currentConfig) {
            this.parentBuilder = parentBuilder;
            this.currentConfig = currentConfig;
        }

        /**
         * Adds a column with a specified ColumnValueType.
         *
         * @param columnName Name of the column.
         * @param valueType  Type of value to generate.
         * @return Builder instance for chaining.
         */
        public InsertIntoBuilder column(String columnName, ColumnValueType valueType) {
            Supplier<String> supplier = mapColumnValueType(valueType);
            currentConfig.columns.add(new Column(columnName, supplier));
            return this;
        }

        /**
         * Adds a column with a custom Supplier<String>.
         *
         * @param columnName    Name of the column.
         * @param valueSupplier Supplier to generate the column value.
         * @return Builder instance for chaining.
         */
        public InsertIntoBuilder column(String columnName, Supplier<String> valueSupplier) {
            if (valueSupplier == null) {
                throw new IllegalArgumentException("ValueSupplier cannot be null.");
            }
            currentConfig.columns.add(new Column(columnName, valueSupplier));
            return this;
        }

        /**
         * Adds a column with a fixed value.
         *
         * @param columnName Name of the column.
         * @param value      Fixed value to set.
         * @return Builder instance for chaining.
         */
        public InsertIntoBuilder column(String columnName, String value) {
            Supplier<String> supplier = () -> value;
            currentConfig.columns.add(new Column(columnName, supplier));
            return this;
        }

        /**
         * Adds a reference to another table.
         *
         * @param refTable     Reference table name.
         * @param refColumn    Column in the reference table.
         * @param targetColumn Column in the current table to set the reference.
         * @return Builder instance for chaining.
         */
        public InsertIntoBuilder ref(String refTable, String refColumn, String targetColumn) {
            currentConfig.references.add(new Reference(refTable, refColumn, targetColumn));
            return this;
        }

        /**
         * Sets the number of times to insert.
         *
         * @param times Number of inserts per reference entry or overall.
         * @return Parent builder for chaining.
         */
        public TestDataBuilder times(int times) {
            if (times <= 0) {
                throw new IllegalArgumentException("Times must be positive.");
            }
            currentConfig.times = times;
            return parentBuilder;
        }

        /**
         * Maps ColumnValueType to corresponding Supplier<String>.
         *
         * @param type Type of column value.
         * @return Supplier for generating the value.
         */
        private Supplier<String> mapColumnValueType(ColumnValueType type) {
            switch (type) {
                case UUID:
                    return () -> UUID.randomUUID().toString();
                case FULL_NAME:
                    return () -> faker.name().fullName();
                case TITLE:
                    return () -> faker.book().title();
                case DATE:
                    return () -> dateFormatter.format(faker.date().birthday());
                case NULL:
                    return () -> null;
                default:
                    throw new IllegalArgumentException("Unsupported ColumnValueType: " + type);
            }
        }
    }

    /**
     * Helper class to store insert configurations.
     */
    private static class InsertConfig {
        String tableName;
        List<Column> columns = new ArrayList<>();
        List<Reference> references = new ArrayList<>();
        int times = 1;

        InsertConfig(String tableName) {
            this.tableName = tableName;
        }
    }

    /**
     * Helper class to store column information.
     */
    private static class Column {
        String name;
        Supplier<String> valueSupplier;

        Column(String name, Supplier<String> valueSupplier) {
            this.name = name;
            this.valueSupplier = valueSupplier;
        }
    }

    /**
     * Helper class to store reference information.
     */
    private static class Reference {
        String refTable;
        String refColumn;
        String targetColumn;

        Reference(String refTable, String refColumn, String targetColumn) {
            this.refTable = refTable;
            this.refColumn = refColumn;
            this.targetColumn = targetColumn;
        }
    }
}
