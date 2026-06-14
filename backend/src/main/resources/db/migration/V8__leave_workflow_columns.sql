-- Multi-level approval tracking: add current_approval_level to leave_requests,
-- and add status + relax NOT NULL on action/acted_at in leave_approvals so
-- pre-created PENDING approval rows can exist before anyone acts on them.

ALTER TABLE leave_requests
    ADD COLUMN current_approval_level INT NOT NULL DEFAULT 1;

ALTER TABLE leave_approvals
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ALTER COLUMN action  DROP NOT NULL,
    ALTER COLUMN acted_at DROP NOT NULL;

CREATE INDEX idx_leave_approvals_approver_status ON leave_approvals(approver_id, status);
