package rowsmith.gradle;

/** Where the schema for code generation comes from; chosen in the {@code rowsmith { }} block with flyway, sql, url or custom. */
public enum SchemaSourceType {
    FLYWAY,
    SQL,
    URL,
    CUSTOM
}
