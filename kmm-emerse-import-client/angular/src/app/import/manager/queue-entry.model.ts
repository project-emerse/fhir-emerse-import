export interface QueueEntry {
    ID: string;
    SUBMITTED: number;
    SUBMITTED_DATE: Date;
    COMPLETED: number;
    COMPLETED_DATE: Date;
    TOTAL: number;
    PROCESSED: number;
    ERROR_TEXT: string;
    PROCESSING_FLAG: number;
    IDENTIFIER_TYPE: IdentifierType;
}

export enum IdentifierType {MRN= 'MRN', PATID = 'PATID', DOCID = 'DOCID'}
