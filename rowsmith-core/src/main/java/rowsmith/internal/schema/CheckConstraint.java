package rowsmith.internal.schema;

import java.util.List;
import rowsmith.internal.sql.SqlExpr;
import rowsmith.internal.sql.SqlParser;

public final class CheckConstraint {
    public final String name;
    public final String body;
    public final SqlExpr expr;
    /** The columns the CHECK reads (pg_constraint.conkey); empty for domain CHECKs, which read VALUE. */
    public final List<String> columns;

    public CheckConstraint(String name, String body, List<String> columns) {
        this.name = name;
        this.body = body;
        this.columns = columns;
        this.expr = SqlParser.tryParse(body);
    }

    public String definition() { return "CHECK (" + body + ")"; }

    @Override
    public String toString() { return name + " " + definition(); }
}
