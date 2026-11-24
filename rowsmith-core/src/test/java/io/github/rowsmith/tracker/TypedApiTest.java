package io.github.rowsmith.tracker;

import static io.github.rowsmith.RelativeTime.today;
import static io.github.rowsmith.tracker.Db.APP_USER;
import static io.github.rowsmith.tracker.Db.PROJECT;
import static io.github.rowsmith.tracker.Db.TASK;
import static io.github.rowsmith.tracker.Db.TASK_COMMENT;
import static io.github.rowsmith.tracker.Db.WORKSPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rowsmith.ErrorCode;
import io.github.rowsmith.Pg;
import io.github.rowsmith.Plan;
import io.github.rowsmith.RelativeTime;
import io.github.rowsmith.Row;
import io.github.rowsmith.Rowsmith;
import io.github.rowsmith.RowsmithException;
import io.github.rowsmith.ValueSource;
import io.github.rowsmith.tracker.Db.AppUserRole;
import io.github.rowsmith.tracker.Db.Priority;
import io.github.rowsmith.tracker.Db.TaskStatus;
import io.github.rowsmith.tracker.Db.WorkspacePlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TypedApiTest {

    static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    static DataSource ds;

    @BeforeAll
    static void schema() {
        ds = Pg.withResource("tracker.sql");
    }

    Rowsmith rs() {
        return Rowsmith.builder(ds).clock(Pg.clockAt(TODAY)).build();
    }

    @Test
    void setsAndReadsTypedValues() {
        Row task = rs().create(TASK, t -> t
                .set(TASK.TITLE, "Write the release notes")
                .set(TASK.STATUS, TaskStatus.IN_PROGRESS)
                .set(TASK.PRIORITY, Priority.HIGH)
                .set(TASK.ESTIMATE_HOURS, 5)
                .set(TASK.LABELS, List.of("docs", "release")));
        assertThat(task.get(TASK.TITLE)).isEqualTo("Write the release notes");
        assertThat(task.get(TASK.STATUS)).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getString("status")).isEqualTo("in progress");
        assertThat(task.get(TASK.PRIORITY)).isEqualTo(Priority.HIGH);
        assertThat(task.get(TASK.ESTIMATE_HOURS)).isEqualTo(5);
        assertThat(task.get(TASK.LABELS)).containsExactly("docs", "release");
        Long id = task.get(TASK.ID);
        assertThat(id).isEqualTo(task.id());
        assertThat(task.get(TASK.DONE_AT)).isNull();
    }

    @Test
    void generatedValuesHaveTheGeneratedJavaTypes() {
        Row project = rs().create(PROJECT, p -> p.set(PROJECT.BUDGET, new BigDecimal("1200.50")));
        UUID id = project.get(PROJECT.ID);
        assertThat(id).isNotNull();
        assertThat(project.get(PROJECT.BUDGET)).isEqualByComparingTo("1200.50");
        assertThat(project.get(PROJECT.STARTS_ON)).isInstanceOf(LocalDate.class);
        assertThat(project.get(PROJECT.ARCHIVED)).isFalse();
        Row workspace = project.parent(PROJECT.WORKSPACE_ID);
        assertThat(workspace.get(WORKSPACE.PLAN)).isIn((Object[]) WorkspacePlan.values());
        assertThat(workspace.get(WORKSPACE.SETTINGS)).isEqualTo("{}");
        Row owner = workspace.parent(APP_USER);
        assertThat(owner.get(APP_USER.ROLE)).isEqualTo(AppUserRole.MEMBER);
        assertThat(owner.get(APP_USER.CREATED_AT)).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void constraintsLinkingColumnsNameTheColumnsToSet() {
        assertThatThrownBy(() -> rs().create(TASK, t -> t.set(TASK.STATUS, TaskStatus.DONE)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_GENERATE);
                    assertThat(e.getMessage()).contains("check task_check", "set done_at with .set(...) in the test or with RowsmithSetup.value(...)");
                });
        Row done = rs().create(TASK, t -> t.set(TASK.STATUS, TaskStatus.DONE).set(TASK.DONE_AT, RelativeTime.now()));
        assertThat(done.get(TASK.DONE_AT)).isNotNull();
        Row todo = rs().create(TASK, t -> t.set(TASK.STATUS, TaskStatus.TODO));
        assertThat(todo.get(TASK.DONE_AT)).isNull();
    }

    @Test
    void relativeDatesFollowRowsmithsClock() {
        Row task = rs().create(TASK, t -> t.set(TASK.DUE_ON, today().minusDays(3)));
        assertThat(task.get(TASK.DUE_ON)).isEqualTo(TODAY.minusDays(3));
        Row project = rs().create(PROJECT, p -> p
                .set(PROJECT.STARTS_ON, RelativeTime.parse("today-1m"))
                .set(PROJECT.ENDS_ON, today().plus(Period.ofWeeks(2))));
        assertThat(project.get(PROJECT.STARTS_ON)).isEqualTo(TODAY.minusMonths(1));
        assertThat(project.get(PROJECT.ENDS_ON)).isEqualTo(TODAY.plusWeeks(2));
    }

    @Test
    void presetsArePlainMethodsAndCompose() {
        Rowsmith rs = rs();
        Row overdue = rs.create(TASK, TaskData::overdue);
        assertThat(overdue.get(TASK.DUE_ON)).isEqualTo(TODAY.minusDays(3));
        assertThat(overdue.get(TASK.STATUS)).isEqualTo(TaskStatus.TODO);

        Row both = rs.create(TASK, t -> t.apply(TaskData::overdue).apply(TaskData::urgent).set(TASK.TITLE, "Fix login"));
        assertThat(both.get(TASK.PRIORITY)).isEqualTo(Priority.URGENT);
        assertThat(both.get(TASK.DUE_ON)).isEqualTo(TODAY.minusDays(3));
        assertThat(both.get(TASK.TITLE)).isEqualTo("Fix login");

        Row laterPresetWins = rs.create(TASK, t -> t.apply(TaskData::overdue).apply(TaskData::done));
        assertThat(laterPresetWins.get(TASK.STATUS)).isEqualTo(TaskStatus.DONE);
        assertThat(laterPresetWins.get(TASK.DONE_AT)).isNotNull();
    }

    @Test
    void childrenAndParentsByTable() {
        Row project = rs().create(PROJECT, p -> p
                .with(TASK, 2, TaskData::overdue)
                .with(TASK, t -> t.apply(TaskData::done).with(TASK_COMMENT, 2)));
        List<Row> tasks = project.children(TASK);
        assertThat(tasks).hasSize(3);
        assertThat(tasks).allSatisfy(t -> assertThat(t.get(TASK.PROJECT_ID)).isEqualTo(project.get(PROJECT.ID)));
        assertThat(tasks.get(2).children(TASK_COMMENT)).hasSize(2);
        assertThat(tasks.get(0).parent(PROJECT)).isSameAs(project);
        assertThat(project.children(TASK_COMMENT)).isEmpty();
    }

    @Test
    void foreignKeysTakeRows() {
        Rowsmith rs = rs();
        Row alice = rs.create(APP_USER, u -> u.set(APP_USER.EMAIL, "alice@example.com").set(APP_USER.ROLE, AppUserRole.ADMIN));
        Row bob = rs.create(APP_USER);
        Row project = rs.create(PROJECT);
        Row task = rs.create(TASK, t -> t
                .set(TASK.PROJECT_ID, project)
                .set(TASK.REPORTER_ID, alice)
                .use(TASK.ASSIGNEE_ID, bob));
        assertThat(task.get(TASK.PROJECT_ID)).isEqualTo(project.get(PROJECT.ID));
        assertThat(task.get(TASK.REPORTER_ID)).isEqualTo(alice.get(APP_USER.ID));
        assertThat(task.parent(TASK.ASSIGNEE_ID)).isSameAs(bob);
        assertThat(rs.find(APP_USER, alice.id()).get(APP_USER.ROLE)).isEqualTo(AppUserRole.ADMIN);
    }

    @Test
    void withNamesTheForeignKeyWhenThereAreSeveral() {
        Row user = rs().create(APP_USER, u -> u
                .with(TASK, TASK.ASSIGNEE_ID, 2, t -> {})
                .with(TASK, TASK.REPORTER_ID, 1, t -> {}));
        List<Row> tasks = user.children(TASK);
        assertThat(tasks).hasSize(3);
        assertThat(tasks).extracting(t -> t.get(TASK.ASSIGNEE_ID)).startsWith(user.get(APP_USER.ID), user.get(APP_USER.ID));
        assertThat(tasks.get(2).get(TASK.REPORTER_ID)).isEqualTo(user.get(APP_USER.ID));
    }

    @Test
    void createManyAndPlanAcceptTables() {
        Rowsmith rs = rs();
        List<Row> tasks = rs.createMany(TASK, 3, t -> t.setNull(TASK.ASSIGNEE_ID));
        assertThat(tasks).hasSize(3).allSatisfy(t -> assertThat(t.get(TASK.ASSIGNEE_ID)).isNull());
        Plan plan = rs.plan(TASK, TaskData::urgent);
        assertThat(plan.counts()).containsKeys("task", "project", "workspace", "app_user");
        assertThat(rs.describe(TASK)).contains("status text NOT NULL");
    }

    @Test
    void planShowsWhereEachValueComesFrom() {
        Plan plan = rs().plan(TASK, t -> t.set(TASK.STATUS, TaskStatus.DONE).set(TASK.DONE_AT, RelativeTime.now()));
        Plan.PlannedRow task = plan.rows().get(plan.rows().size() - 1);
        assertThat(task.sources())
                .containsEntry("id", ValueSource.DATABASE_GENERATED)
                .containsEntry("status", ValueSource.USER)
                .containsEntry("project_id", ValueSource.PARENT_ROW)
                .containsEntry("assignee_id", ValueSource.LEFT_NULL)
                .containsEntry("title", ValueSource.ROWSMITH)
                .containsEntry("priority", ValueSource.DATABASE_DEFAULT)
                .containsEntry("done_at", ValueSource.USER);
        assertThat(plan.toString()).contains("status: USER; ");
    }

    @Test
    void columnsOfAnotherTableAreRejected() {
        assertThatThrownBy(() -> rs().create(PROJECT, p -> p.set(TASK.TITLE, "x")))
                .isInstanceOfSatisfying(RowsmithException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.UNKNOWN_COLUMN);
                    assertThat(e.getMessage()).contains("public.task.title is not a column of project", "(TypedApiTest:");
                });
        Row project = rs().create(PROJECT);
        assertThatThrownBy(() -> project.get(TASK.TITLE)).hasMessageContaining("public.task.title is not a column of public.project");
        assertThatThrownBy(() -> rs().create(APP_USER, u -> u.with(TASK, PROJECT.WORKSPACE_ID, 1, t -> {})))
                .hasMessageContaining("public.project.workspace_id is not a column of public.task");
    }

    @Test
    void typedValuesAreStillCheckedAgainstTheSchema() {
        assertThatThrownBy(() -> rs().create(TASK, t -> t.set(TASK.TITLE, "ab")))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CHECK_VIOLATION));
        assertThatThrownBy(() -> rs().create(TASK, t -> t.setNull(TASK.TITLE)))
                .isInstanceOfSatisfying(RowsmithException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_NULL_VIOLATION));
    }

    @Test
    void stringAndTypedCallsMix() {
        Rowsmith rs = rs();
        Row task = rs.create("task", t -> t.set("status", TaskStatus.WON_T_DO).set(TASK.PRIORITY, Priority.LOW));
        assertThat(task.get(TASK.STATUS)).isEqualTo(TaskStatus.WON_T_DO);
        assertThat(task.getString("status")).isEqualTo("won't do");
        assertThat(task.get(TASK.PRIORITY)).isEqualTo(Priority.LOW);
    }
}
