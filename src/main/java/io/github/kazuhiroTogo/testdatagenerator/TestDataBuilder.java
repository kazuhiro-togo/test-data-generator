package io.github.kazuhiroTogo.testdatagenerator;

import com.github.javafaker.Faker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private TestDataBuilder() {
        this.faker = new Faker(Locale.JAPAN);
    }

    /**
     * Creates a new builder instance with default Faker
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
        Map<String, List<Map<String, Object>>> database = new LinkedHashMap<>();

        for (InsertConfig config : insertConfigs) {
            List<Map<String, Object>> table = database.computeIfAbsent(config.tableName, k -> new ArrayList<>());

            if (config.references.isEmpty()) {
                // No references; generate 'times' number of rows
                for (int i = 0; i < config.times; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (Column column : config.columns) {
                        Object value = column.valueSupplier.get();
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
                List<Map<String, Object>> refTableData = database.get(refTable);
                if (refTableData == null || refTableData.isEmpty()) {
                    throw new IllegalStateException("Reference table '" + refTable + "' has no data.");
                }

                for (Map<String, Object> refRow : refTableData) {
                    for (int i = 0; i < config.times; i++) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        // Set all reference columns
                        for (Reference ref : config.references) {
                            row.put(ref.targetColumn, refRow.get(ref.refColumn));
                        }
                        // Set other columns
                        for (Column column : config.columns) {
                            Object value = column.valueSupplier.get();
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
            case TSV:
                return generateTsvOutput(database);
            case CSV:
                return generateCsvOutput(database);
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
    private String generatePipeTableOutput(Map<String, List<Map<String, Object>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            sb.append("Table: ").append(tableName).append("\n");
            List<Map<String, Object>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                sb.append("(No data)\n\n");
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                columns.addAll(row.keySet());
            }

            // Header
            sb.append("| ").append(String.join(" | ", columns)).append(" |\n");

            // Separator
            sb.append("| ").append(columns.stream().map(col -> "---").collect(Collectors.joining(" | "))).append(" |\n");

            // Rows
            for (Map<String, Object> row : rows) {
                List<String> values = columns.stream()
                        .map(col -> formatValue(row.getOrDefault(col, "NULL")))
                        .collect(Collectors.toList());
                sb.append("| ").append(String.join(" | ", values)).append(" |\n");
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
    private String generateTsvOutput(Map<String, List<Map<String, Object>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            sb.append("Table: ").append(tableName).append("\n");
            List<Map<String, Object>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                sb.append("(No data)\n\n");
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                columns.addAll(row.keySet());
            }

            // Header
            sb.append(String.join("\t", columns)).append("\n");

            // Rows
            for (Map<String, Object> row : rows) {
                List<String> values = columns.stream()
                        .map(col -> formatValue(row.getOrDefault(col, "NULL")))
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
    private String generateCsvOutput(Map<String, List<Map<String, Object>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            sb.append("Table: ").append(tableName).append("\n");
            List<Map<String, Object>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                sb.append("(No data)\n\n");
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                columns.addAll(row.keySet());
            }

            // Header
            sb.append(String.join(",", columns)).append("\n");

            // Rows
            for (Map<String, Object> row : rows) {
                List<String> values = columns.stream()
                        .map(col -> escapeCsv(formatValue(row.getOrDefault(col, "NULL"))))
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
     * Generates SQL INSERT statements as a string output.
     *
     * @param database Map of table names to their data.
     * @return SQL INSERT statements.
     */
    private String generateInsertOutput(Map<String, List<Map<String, Object>>> database) {
        StringBuilder sb = new StringBuilder();
        for (String tableName : database.keySet()) {
            List<Map<String, Object>> rows = database.get(tableName);
            if (rows.isEmpty()) {
                continue;
            }

            // Get all column names
            Set<String> columns = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                columns.addAll(row.keySet());
            }

            // Generate INSERT statements
            for (Map<String, Object> row : rows) {
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
    private String formatSqlValue(Object value) {
        if (value == null || "NULL".equals(value)) {
            return "NULL";
        }
        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof UUID) {
            return "'" + value.toString() + "'";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        // Escape single quotes in strings
        String escaped = value.toString().replace("'", "''");
        return "'" + escaped + "'";
    }

    /**
     * Formats a value based on its type.
     *
     * @param value The value to format.
     * @return Formatted string representation of the value.
     */
    private String formatValue(Object value) {
        if (value == null || "NULL".equals(value)) {
            return "NULL";
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).toString(); // "yyyy-MM-dd"
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toString(); // "yyyy-MM-ddTHH:mm:ss"
        }
        return value.toString();
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
     * Helper class to store column information.
     */
    private static class Column {
        String name;
        Supplier<Object> valueSupplier;
        Class<?> type;

        Column(String name, Supplier<Object> valueSupplier, Class<?> type) {
            this.name = name;
            this.valueSupplier = valueSupplier;
            this.type = type;
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
         * Adds a column with a specified ColumnType.
         *
         * @param columnName Name of the column.
         * @param valueType  Type of value to generate.
         * @return Builder instance for chaining.
         */
        public InsertIntoBuilder column(String columnName, ColumnType valueType) {
            Supplier<Object> supplier = mapColumnType(valueType);
            Class<?> type = mapColumnTypeToClass(valueType);
            currentConfig.columns.add(new Column(columnName, supplier, type));
            return this;
        }

        public <T> InsertIntoBuilder column(String columnName, T value, Class<?> type) {
            currentConfig.columns.add(new Column(columnName, () -> value, type));
            return this;
        }

        /**
         * Adds a column with a custom Supplier.
         *
         * @param columnName    Name of the column.
         * @param valueSupplier Supplier to generate the column value.
         * @param type          Class type of the value.
         * @return Builder instance for chaining.
         */
        public InsertIntoBuilder column(String columnName, Supplier<?> valueSupplier, Class<?> type) {
            if (valueSupplier == null || type == null) {
                throw new IllegalArgumentException("ValueSupplier and type cannot be null.");
            }
            currentConfig.columns.add(new Column(columnName, () -> valueSupplier.get(), type));
            return this;
        }

        public <T> InsertIntoBuilder column(String columnName, T value) {
            return column(columnName, value, value.getClass());
        }

        public <T> InsertIntoBuilder column(String columnName, Supplier<T> value) {
            return column(columnName, value, value.getClass());
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
         * Maps ColumnType to corresponding Supplier and Class type.
         *
         * @param type Type of column value.
         * @return Supplier for generating the value.
         */
        private Supplier<Object> mapColumnType(ColumnType type) {
            switch (type) {
                case UUID:
                    return () -> UUID.randomUUID().toString();
                case STRING:
                    return () -> faker.name().fullName();
                case DATE:
                    return () -> faker.date().birthday().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                case DATETIME:
                    return () -> faker.date().birthday().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                case BOOLEAN:
                    return () -> faker.bool().bool();
                case INT:
                    return () -> faker.number().numberBetween(1, 100);
                case DOUBLE:
                    return () -> faker.number().randomDouble(2, 1, 100);
                case NULL:
                    return () -> null;
                default:
                    throw new IllegalArgumentException("Unsupported ColumnType: " + type);
            }
        }

        /**
         * Maps ColumnType to corresponding Class type.
         *
         * @param type Type of column value.
         * @return Class representing the type.
         */
        private Class<?> mapColumnTypeToClass(ColumnType type) {
            switch (type) {
                case UUID:
                    return String.class;
                case STRING:
                    return String.class;
                case INT:
                    return Integer.class;
                case DOUBLE:
                    return Double.class;
                case DATE:
                    return LocalDate.class;
                case DATETIME:
                    return LocalDateTime.class;
                case NULL:
                    return Object.class;
                default:
                    throw new IllegalArgumentException("Unsupported ColumnType: " + type);
            }
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
}
