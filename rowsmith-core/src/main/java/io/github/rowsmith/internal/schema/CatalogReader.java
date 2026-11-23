package io.github.rowsmith.internal.schema;

import io.github.rowsmith.internal.sql.SqlParser;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads pg_catalog directly: DatabaseMetaData misses CHECKs, partial-index predicates, identity and generated columns, and enums. */
public final class CatalogReader {

    private static final String USER_SCHEMAS =
            "n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg\\_toast%' "
                    + "AND n.nspname NOT LIKE 'pg\\_temp%'";

    private CatalogReader() {}

    public static Schema read(Connection conn) throws SQLException {
        int version;
        List<String> searchPath = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT current_setting('server_version_num')::int, current_schemas(false)")) {
            rs.next();
            version = rs.getInt(1);
            Array arr = rs.getArray(2);
            for (Object o : (Object[]) arr.getArray()) searchPath.add(String.valueOf(o));
        }
        Schema schema = new Schema(searchPath, version);
        Map<Long, PgType> types = readTypes(conn);
        readTables(conn, schema);
        readColumns(conn, schema, types, volatileFunctions(conn));
        readConstraints(conn, schema);
        readIndexes(conn, schema, version);
        readPartitions(conn, schema);
        resolveDisplayNames(schema);
        return schema;
    }

    private static Map<Long, PgType> readTypes(Connection conn) throws SQLException {
        Map<Long, PgType> types = new HashMap<>();
        String sql = "SELECT t.oid, n.nspname, t.typname, format_type(t.oid, NULL), t.typtype, t.typcategory, "
                + "t.typelem, t.typbasetype, t.typtypmod, t.typnotnull "
                + "FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace "
                + "WHERE t.typtype IN ('b','d','e','r','m') OR t.typcategory = 'A'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                PgType t = new PgType(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        PgType.Kind.of(rs.getString(5)), PgType.Category.of(rs.getString(6)), rs.getLong(7), rs.getLong(8),
                        rs.getInt(9), rs.getBoolean(10));
                types.put(t.oid, t);
            }
        }
        for (PgType t : types.values()) {
            if (t.elemOid != 0 && t.category == PgType.Category.ARRAY) t.element = types.get(t.elemOid);
            if (t.baseOid != 0) t.base = types.get(t.baseOid);
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT enumtypid, enumlabel FROM pg_enum ORDER BY enumtypid, enumsortorder")) {
            while (rs.next()) {
                PgType t = types.get(rs.getLong(1));
                if (t != null) t.enumLabels.add(rs.getString(2));
            }
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT con.contypid, con.conname, pg_get_expr(con.conbin, 0)"
                     + " FROM pg_constraint con WHERE con.contypid <> 0 AND con.contype = 'c' ORDER BY con.contypid, con.conname")) {
            while (rs.next()) {
                PgType t = types.get(rs.getLong(1));
                if (t != null) t.domainChecks.add(new CheckConstraint(rs.getString(2), rs.getString(3), List.of()));
            }
        }
        return types;
    }

    private static void readTables(Connection conn, Schema schema) throws SQLException {
        String sql = "SELECT c.oid, n.nspname, c.relname, c.relispartition "
                + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE c.relkind IN ('r','p') AND " + USER_SCHEMAS + " "
                + "AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e') "
                + "ORDER BY n.nspname, c.relname";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                schema.add(new TableDef(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
            }
        }
    }

    private static Set<String> volatileFunctions(Connection conn) throws SQLException {
        Set<String> out = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT proname FROM pg_proc WHERE provolatile = 'v'")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private static void readColumns(Connection conn, Schema schema, Map<Long, PgType> types, Set<String> volatileFunctions) throws SQLException {
        String sql = "SELECT a.attrelid, a.attnum, a.attname, a.atttypid, a.atttypmod, "
                + "format_type(a.atttypid, a.atttypmod), a.attnotnull, pg_get_expr(d.adbin, d.adrelid), "
                + "a.attidentity, a.attgenerated <> '', "
                + "COALESCE((SELECT s.oid::regclass::text FROM pg_depend dep JOIN pg_class s ON s.oid = dep.refobjid AND s.relkind = 'S' "
                + "WHERE dep.classid = 'pg_attrdef'::regclass AND dep.objid = d.oid LIMIT 1), "
                + "pg_get_serial_sequence(c.oid::regclass::text, a.attname)), ty.typtype, ty.typcategory "
                + "FROM pg_attribute a JOIN pg_class c ON c.oid = a.attrelid JOIN pg_type ty ON ty.oid = a.atttypid "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "LEFT JOIN pg_attrdef d ON d.adrelid = a.attrelid AND d.adnum = a.attnum "
                + "WHERE a.attnum > 0 AND NOT a.attisdropped AND c.relkind IN ('r','p') AND " + USER_SCHEMAS + " "
                + "ORDER BY a.attrelid, a.attnum";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TableDef t = schema.byOid(rs.getLong(1));
                if (t == null) continue;
                PgType type = types.get(rs.getLong(4));
                if (type == null) {
                    type = new PgType(rs.getLong(4), "", rs.getString(6), rs.getString(6),
                            PgType.Kind.of(rs.getString(12)), PgType.Category.of(rs.getString(13)), 0, 0, -1, false);
                }
                boolean generated = rs.getBoolean(10);
                String def = generated ? null : rs.getString(8);
                t.addColumn(new ColumnDef(rs.getString(3), rs.getInt(2), type, rs.getInt(5), rs.getString(6),
                        rs.getBoolean(7), def, ColumnDef.Identity.of(rs.getString(9)), generated, rs.getString(11), volatileFunctions));
            }
        }
    }

    private static List<String> existingColumns(List<ColumnDef> columns) {
        List<String> names = new ArrayList<>();
        for (ColumnDef c : columns) if (c != null) names.add(c.name);
        return names;
    }

    private static void readConstraints(Connection conn, Schema schema) throws SQLException {
        String sql = "SELECT con.conrelid, con.conname, con.contype, con.conkey::int[], con.confrelid, "
                + "con.confkey::int[], con.condeferrable, con.condeferred, pg_get_expr(con.conbin, con.conrelid) "
                + "FROM pg_constraint con JOIN pg_class c ON c.oid = con.conrelid "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE con.contype IN ('f','c') AND " + USER_SCHEMAS + " ORDER BY con.conrelid, con.conname";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TableDef t = schema.byOid(rs.getLong(1));
                if (t == null) continue;
                String type = rs.getString(3);
                List<ColumnDef> cols = new ArrayList<>();
                for (int num : ints(rs.getArray(4))) cols.add(t.columnByNum(num));
                if (type.equals("c")) {
                    t.checks.add(new CheckConstraint(rs.getString(2), rs.getString(9), existingColumns(cols)));
                } else {
                    t.foreignKeys.add(new ForeignKey(rs.getString(2), t, cols, rs.getLong(5), ints(rs.getArray(6)),
                            rs.getBoolean(7), rs.getBoolean(8)));
                }
            }
        }
        for (TableDef t : schema.allTables()) {
            List<ForeignKey> unresolved = new ArrayList<>();
            for (ForeignKey fk : t.foreignKeys) {
                TableDef ref = schema.byOid(fk.refTableOid);
                if (ref != null && ref.isPartition) ref = null;
                if (ref == null) { unresolved.add(fk); continue; }
                fk.refTable = ref;
                fk.refColumns = new ArrayList<>();
                for (int num : fk.refColumnNums) fk.refColumns.add(ref.columnByNum(num));
            }
            t.foreignKeys.removeAll(unresolved);
        }
    }

    private static void readIndexes(Connection conn, Schema schema, int version) throws SQLException {
        String nnd = version >= 150000 ? "i.indnullsnotdistinct" : "false";
        String sql = "SELECT i.indrelid, ic.relname, i.indisprimary, i.indkey::int[], "
                + "pg_get_expr(i.indpred, i.indrelid), " + nnd + ", "
                + "ARRAY(SELECT pg_get_indexdef(i.indexrelid, k, true) FROM generate_series(1, i.indnkeyatts) k ORDER BY k), "
                + "ARRAY(SELECT format_type(a.atttypid, a.atttypmod) FROM pg_attribute a "
                + "WHERE a.attrelid = i.indexrelid AND a.attnum <= i.indnkeyatts ORDER BY a.attnum) "
                + "FROM pg_index i JOIN pg_class ic ON ic.oid = i.indexrelid "
                + "JOIN pg_class c ON c.oid = i.indrelid JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE i.indisunique AND i.indisvalid AND " + USER_SCHEMAS + " ORDER BY i.indrelid, i.indisprimary DESC, ic.relname";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TableDef t = schema.byOid(rs.getLong(1));
                if (t == null) continue;
                List<Integer> keys = ints(rs.getArray(4));
                Object[] texts = (Object[]) rs.getArray(7).getArray();
                List<String> types = new ArrayList<>();
                for (Object type : (Object[]) rs.getArray(8).getArray()) types.add(String.valueOf(type));
                List<UniqueKey.KeyPart> parts = new ArrayList<>();
                for (int k = 0; k < texts.length; k++) {
                    int num = k < keys.size() ? keys.get(k) : 0;
                    String text = String.valueOf(texts[k]);
                    parts.add(num > 0 ? new UniqueKey.ColumnPart(t.columnByNum(num)) : new UniqueKey.ExprPart(text, types.get(k), SqlParser.tryParse(text)));
                }
                String pred = rs.getString(5);
                UniqueKey uk = new UniqueKey(rs.getString(2), rs.getBoolean(3), parts, pred,
                        pred == null ? null : SqlParser.tryParse(pred), rs.getBoolean(6));
                t.uniqueKeys.add(uk);
                if (uk.primary) t.primaryKey = uk;
            }
        }
    }

    private static void readPartitions(Connection conn, Schema schema) throws SQLException {
        String sql = "SELECT p.partrelid, p.partattrs::int[], p.partstrat = 'h' OR p.partdefid <> 0 FROM pg_partitioned_table p";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TableDef t = schema.byOid(rs.getLong(1));
                if (t == null) continue;
                List<ColumnDef> keys = new ArrayList<>();
                for (int num : ints(rs.getArray(2))) if (num > 0) keys.add(t.columnByNum(num));
                t.partitioning = new Partitioning(keys, rs.getBoolean(3));
            }
        }
    }

    private static void resolveDisplayNames(Schema schema) {
        Map<String, Integer> counts = new HashMap<>();
        for (TableDef t : schema.tables()) counts.merge(t.name, 1, Integer::sum);
        String first = schema.searchPath().isEmpty() ? "public" : schema.searchPath().get(0);
        for (TableDef t : schema.tables()) {
            boolean visible = t.schema.equals(first)
                    || (schema.searchPath().contains(t.schema) && counts.get(t.name) == 1);
            t.displayName = visible ? t.name : t.schema + "." + t.name;
        }
    }

    private static List<Integer> ints(Array arr) throws SQLException {
        List<Integer> out = new ArrayList<>();
        if (arr == null) return out;
        for (Object o : (Object[]) arr.getArray()) out.add(((Number) o).intValue());
        return out;
    }
}
