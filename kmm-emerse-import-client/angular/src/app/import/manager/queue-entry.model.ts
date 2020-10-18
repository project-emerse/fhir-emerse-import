export enum EntryStatus {
    COMPLETED = 0,
    QUEUED = 1,
    RUNNING = 2,
    SUSPENDED = 3,
    ABORTED = 4,
    ERROR = 5
}

export const ENTRY_STATUS: string[] = ['completed', 'queued', 'running', 'suspended', 'aborted', 'error'];

export enum EntryAction {
    DELETE = 0,
    SUSPEND = 1,
    RESUME = 3,
    ABORT = 4
}

export const VALID_ACTIONS: {[status: number] : EntryAction[]} = {
    [EntryStatus.QUEUED]: [EntryAction.DELETE, EntryAction.SUSPEND, EntryAction.ABORT],
    [EntryStatus.RUNNING]: [EntryAction.SUSPEND, EntryAction.ABORT],
    [EntryStatus.SUSPENDED]: [EntryAction.DELETE, EntryAction.RESUME, EntryAction.ABORT],
    [EntryStatus.ABORTED]: [EntryAction.DELETE, EntryAction.RESUME],
    [EntryStatus.ERROR]: [EntryAction.DELETE, EntryAction.RESUME],
    [EntryStatus.COMPLETED]: [EntryAction.DELETE]
}

export interface QueueEntry {
    ID: string;
    SUBMITTED: number;
    SUBMITTED_DATE: Date;
    COMPLETED: number;
    COMPLETED_DATE: Date;
    ELAPSED: number;
    ELAPSED_TEXT: string;
    TOTAL: number;
    PROCESSED: number;
    ERROR_TEXT: string;
    STATUS: EntryStatus;
    STATUS_TEXT: string;
    IDENTIFIER_TYPE: IdentifierType;
}

export enum IdentifierType {MRN= 'MRN', PATID = 'PATID', DOCID = 'DOCID'}
