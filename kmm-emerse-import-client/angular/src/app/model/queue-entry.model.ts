export enum EntryStatus {
    QUEUED = 0,
    RUNNING = 1,
    SUSPENDED = 2,
    COMPLETED = 3,
    ABORTED = 4,
    ERROR = 5,
    DELETED = 6
}

const STATUS_TEXT: string[] = ['queued', 'running', 'suspended', 'completed', 'aborted', 'error', 'deleted'];

export enum EntryAction {
    DELETE = 0,
    RESUME = 1,
    SUSPEND = 2,
    ABORT = 3,
    RESTART = 4
}

const ACTION_TEXT: string[] = ['Deleting', 'Resuming', 'Suspending', 'Aborting', 'Restarting'];

const VALID_ACTIONS: {[status: number] : EntryAction[]} = {
    [EntryStatus.QUEUED]: [EntryAction.DELETE, EntryAction.SUSPEND, EntryAction.ABORT],
    [EntryStatus.RUNNING]: [EntryAction.SUSPEND, EntryAction.ABORT],
    [EntryStatus.SUSPENDED]: [EntryAction.DELETE, EntryAction.RESUME, EntryAction.ABORT, EntryAction.RESTART],
    [EntryStatus.ABORTED]: [EntryAction.DELETE, EntryAction.RESUME, EntryAction.RESTART],
    [EntryStatus.ERROR]: [EntryAction.DELETE, EntryAction.RESUME, EntryAction.RESTART],
    [EntryStatus.COMPLETED]: [EntryAction.DELETE, EntryAction.RESTART],
    [EntryStatus.DELETED]: []
}

export interface QueueEntry {
    id: string;
    submitted: number;
    submitted_date: Date;
    completed: number;
    completed_date: Date;
    elapsed: number;
    elapsed_text: string;
    total: number;
    processed: number;
    error_text: string;
    status: EntryStatus;
    status_text: string;
    identifier_type: IdentifierType;
}

export enum IdentifierType {MRN = 'MRN', PATID = 'PATID', DOCID = 'DOCID'}

export function isValidAction(status: EntryStatus, action: EntryAction): boolean {
    return VALID_ACTIONS[status].includes(action);
}

export function statusText(status: EntryStatus): string {
    return status == null ? null : STATUS_TEXT[status];
}

export function actionText(action: EntryAction): string {
    return action == null ? null : ACTION_TEXT[action];
}
