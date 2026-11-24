package io.github.rowsmith.tracker;

import static io.github.rowsmith.RelativeTime.now;
import static io.github.rowsmith.RelativeTime.today;
import static io.github.rowsmith.tracker.Db.TASK;

import io.github.rowsmith.RowSpec;
import io.github.rowsmith.tracker.Db.Priority;
import io.github.rowsmith.tracker.Db.TaskStatus;

final class TaskData {

    private TaskData() {}

    static void overdue(RowSpec t) {
        t.set(TASK.STATUS, TaskStatus.TODO).set(TASK.DUE_ON, today().minusDays(3));
    }

    static void urgent(RowSpec t) {
        t.set(TASK.PRIORITY, Priority.URGENT);
    }

    static void done(RowSpec t) {
        t.set(TASK.STATUS, TaskStatus.DONE).set(TASK.DONE_AT, now().minusHours(1));
    }
}
