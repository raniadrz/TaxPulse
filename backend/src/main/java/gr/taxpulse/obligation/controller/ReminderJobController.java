package gr.taxpulse.obligation.controller;

import gr.taxpulse.obligation.reminder.DeadlineReminderService;
import gr.taxpulse.obligation.reminder.DeadlineReminderService.ReminderRunResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual trigger of the (idempotent) deadline reminder job, for administrators. */
@RestController
@RequestMapping("/api/v1/obligations/reminders")
@RequiredArgsConstructor
public class ReminderJobController {

    private final DeadlineReminderService reminderService;

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ReminderRunResult run() {
        return reminderService.run();
    }
}
