package rowsmith.tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static rowsmith.tracker.Db.APP_USER;
import static rowsmith.tracker.Db.TASK;
import static rowsmith.tracker.Db.WORKSPACE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rowsmith.ErrorCode;
import rowsmith.Pg;
import rowsmith.RelativeTime;
import rowsmith.Row;
import rowsmith.Rowsmith;
import rowsmith.RowsmithException;
import rowsmith.RowsmithSetup;
import rowsmith.tracker.Db.Priority;
import rowsmith.tracker.Db.TaskStatus;

class RowsmithSetupTest {

    static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    static DataSource ds;

    static class TrackerSetup implements RowsmithSetup {
        @Override
        public void configure(Config c) {
            c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com");
            c.value(TASK.PRIORITY, n -> n % 2 == 0 ? Priority.HIGH : Priority.LOW);
            c.value("workspace", "name", n -> "Workspace " + n);
            c.value("task", "due_on", n -> RelativeTime.today().plusDays(n));
        }
    }

    @BeforeAll
    static void schema() {
        ds = Pg.withResource("tracker.sql");
    }

    final List<Rowsmith> instances = new ArrayList<>();

    Rowsmith rs(RowsmithSetup setup) {
        Rowsmith rs = Rowsmith.builder(ds).clock(Pg.clockAt(TODAY)).setup(setup).build();
        instances.add(rs);
        return rs;
    }

    @AfterEach
    void cleanup() {
        instances.forEach(Rowsmith::cleanup);
    }

    @Test
    void valuePatternsApplyToEveryGeneratedRowByRowNumber() {
        Rowsmith rs = rs(new TrackerSetup());
        List<Row> tasks = rs.createMany(TASK, 2);
        assertThat(tasks).extracting(t -> t.get(TASK.PRIORITY)).containsExactly(Priority.LOW, Priority.HIGH);
        assertThat(tasks).extracting(t -> t.get(TASK.DUE_ON)).containsExactly(TODAY.plusDays(1), TODAY.plusDays(2));
        Row workspace = tasks.get(0).parent(TASK.PROJECT_ID).parent(Db.PROJECT.WORKSPACE_ID);
        assertThat(workspace.get(WORKSPACE.NAME)).isEqualTo("Workspace 1");
        assertThat(rs.createMany(APP_USER, 2)).extracting(u -> u.get(APP_USER.EMAIL))
                .as("user1 is the tasks' shared reporter and owner")
                .containsExactly("user2@example.com", "user3@example.com");
    }

    @Test
    void valuesTheTestSetsWin() {
        Row task = rs(new TrackerSetup()).create(TASK, t -> t.set(TASK.PRIORITY, Priority.URGENT).setNull(TASK.DUE_ON));
        assertThat(task.get(TASK.PRIORITY)).isEqualTo(Priority.URGENT);
        assertThat(task.get(TASK.DUE_ON)).isNull();
    }

    @Test
    void uniqueValuesSkipRowNumbersTheTableAlreadyHolds() {
        Row earlier = Rowsmith.builder(ds).setup(new TrackerSetup()).build().create(APP_USER);
        Row earlierDefault = Rowsmith.builder(ds).build().create(APP_USER);
        try {
            assertThat(rs(new TrackerSetup()).createMany(APP_USER, 2)).extracting(u -> u.get(APP_USER.EMAIL))
                    .containsExactly("user2@example.com", "user3@example.com");
            assertThat(rs(new RowsmithSetup.None()).create(APP_USER).get(APP_USER.EMAIL)).isEqualTo("email-2");
        } finally {
            Pg.exec(ds, "DELETE FROM app_user WHERE id IN (" + earlier.id() + ", " + earlierDefault.id() + ")");
        }
    }

    @Test
    void aPatternThatBreaksACheckIsAClearError() {
        RowsmithSetup tooShort = c -> c.value(TASK.TITLE, n -> "T" + n);
        assertThatThrownBy(() -> rs(tooShort).create(TASK))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.CHECK_VIOLATION);
                    assertThat(e.getMessage()).contains("task.title = \"T1\" violates check task_title_check",
                            "title from setup value(task.title)", "(RowsmithSetupTest:");
                });
        RowsmithSetup constant = c -> c.value(APP_USER.EMAIL, n -> "same@example.com");
        Rowsmith rs = rs(constant);
        rs.create(APP_USER);
        assertThatThrownBy(() -> rs.create(APP_USER))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.UNIQUE_VIOLATION);
                    assertThat(e.getMessage()).contains("email from setup value(app_user.email)");
                });
        RowsmithSetup badLabel = c -> c.value("task", "status", n -> "later");
        assertThatThrownBy(() -> rs(badLabel).create(TASK)).hasMessageContaining("allowed: todo, in progress, done, won't do");
    }

    @Test
    void patternsMustNameInsertableNonForeignKeyColumns() {
        assertThatThrownBy(() -> rs(c -> c.value("task", "titel", n -> "x")).create(TASK))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_COLUMN))
                .hasMessageContaining("Columns: ").hasMessageContaining("title");
        assertThatThrownBy(() -> rs(c -> c.value(TASK.REPORTER_ID, n -> 1L)).create(TASK))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_CONFIG))
                .hasMessageContaining("foreign key");
    }

    @Test
    void enumPatternsWriteLabels() {
        Row task = rs(c -> c.value(TASK.STATUS, n -> TaskStatus.IN_PROGRESS)).create(TASK);
        assertThat(task.getString("status")).isEqualTo("in progress");
    }
}
