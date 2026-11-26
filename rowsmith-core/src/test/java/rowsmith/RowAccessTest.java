package rowsmith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RowAccessTest {

    static DataSource ds;
    static Rowsmith rs;

    static final class Counter extends Table {
        final Column<Integer> ID = column("id", Integer.class);

        Counter() {
            super("public", "counter");
        }
    }

    static final Counter COUNTER = new Counter();

    enum Mood implements PgEnum {
        CALM("calm"),
        TENSE("tense");

        private final String label;

        Mood(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    @BeforeAll
    static void schema() {
        ds = Pg.withSchema("""
                CREATE TABLE counter (id serial PRIMARY KEY, label text);
                CREATE TABLE ledger (id bigserial PRIMARY KEY);
                CREATE TABLE pair (a int, b int, PRIMARY KEY (a, b));
                CREATE TABLE note (body text);
                CREATE TYPE mood AS ENUM ('calm', 'tense');
                CREATE TABLE mood_note (mood mood PRIMARY KEY);
                CREATE TABLE blob (id bytea PRIMARY KEY);
                """);
        rs = Rowsmith.of(ds);
    }

    @Test
    void serialPrimaryKeyReadsAsLongOrInt() {
        Row row = rs.create(COUNTER);
        assertThat(row.id()).isInstanceOf(Integer.class);
        long id = row.getLong("id");
        int asInt = row.getInt("id");
        assertThat(id).isEqualTo(asInt).isEqualTo(row.get(COUNTER.ID).longValue());
    }

    @Test
    void bigserialPrimaryKeyReadsAsLongOrInt() {
        Row row = rs.create("ledger");
        assertThat(row.id()).isInstanceOf(Long.class);
        assertThat(row.getInt("id")).isEqualTo(row.getLong("id"));
    }

    @Test
    void typedGetterOnNullFailsWithTheColumnName() {
        Row row = rs.create(COUNTER);
        assertThatThrownBy(() -> row.getLong("label")).isInstanceOf(NullPointerException.class).hasMessage("counter.label is NULL");
    }

    @Test
    void findWithoutMatchIsRowNotFound() {
        assertThatThrownBy(() -> rs.find(COUNTER, 999_999)).isInstanceOfSatisfying(RowsmithException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.ROW_NOT_FOUND));
    }

    @Test
    void findBindsAnEnumIdByItsLabel() {
        rs.create("mood_note", m -> m.set("mood", Mood.TENSE));
        assertThat(rs.find("mood_note", Mood.TENSE).getString("mood")).isEqualTo("tense");
    }

    @Test
    void findBindsABinaryId() {
        byte[] id = {1, 2, (byte) 0xff};
        rs.create("blob", b -> b.set("id", id));
        assertThat((byte[]) rs.find("blob", id).get("id")).isEqualTo(id);
    }

    @Test
    void findWithAMalformedIdIsInvalidValue() {
        assertThatThrownBy(() -> rs.find(COUNTER, "not a number")).isInstanceOfSatisfying(RowsmithException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_VALUE));
    }

    @Test
    void findOnCompositeKeyIsNoSinglePrimaryKey() {
        assertThatThrownBy(() -> rs.find("pair", 1)).isInstanceOfSatisfying(RowsmithException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.NO_SINGLE_PRIMARY_KEY));
    }

    @Test
    void idWithoutSinglePrimaryKeyIsNoSinglePrimaryKey() {
        Row pair = rs.create("pair");
        Row note = rs.create("note");
        for (Row row : new Row[] {pair, note}) {
            assertThatThrownBy(row::id).isInstanceOfSatisfying(RowsmithException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCode.NO_SINGLE_PRIMARY_KEY));
        }
    }

    @Test
    void negativeCountsAreIllegalArguments() {
        assertThatThrownBy(() -> rs.createMany(COUNTER, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rs.planMany(COUNTER, -1, c -> {})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rs.create("ledger", l -> l.with("counter", -1))).isInstanceOf(IllegalArgumentException.class);
    }
}
