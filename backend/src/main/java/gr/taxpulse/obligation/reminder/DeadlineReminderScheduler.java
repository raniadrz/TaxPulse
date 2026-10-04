package gr.taxpulse.obligation.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the reminder job on a cron schedule (default 07:00 Europe/Athens) and once at start-up
 * so that a server that was down at the scheduled time still catches up.
 *
 * <p>For multi-instance deployments add a distributed lock (e.g. ShedLock); the job is already
 * idempotent thanks to dedup keys, so a lock only avoids redundant work.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "taxpulse.reminders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeadlineReminderScheduler {

    private final DeadlineReminderService reminderService;

    @Scheduled(cron = "${taxpulse.reminders.cron}", zone = "${taxpulse.reminders.zone}")
    public void scheduledRun() {
        runSafely();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        runSafely();
    }

    private void runSafely() {
        try {
            reminderService.run();
        } catch (RuntimeException ex) {
            // Never let an exception kill the scheduler thread; the next run will retry.
            log.error("Deadline reminder run failed", ex);
        }
    }
}
