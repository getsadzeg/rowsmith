package rowsmith.tracker;

import static rowsmith.RelativeTime.now;
import static rowsmith.RelativeTime.today;
import static rowsmith.tracker.Db.TASK;

import rowsmith.RowSpec;
import rowsmith.tracker.Db.Priority;
import rowsmith.tracker.Db.TaskStatus;

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
