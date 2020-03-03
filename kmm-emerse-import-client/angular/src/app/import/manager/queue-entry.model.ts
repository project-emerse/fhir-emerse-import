export interface QueueEntry {
    id: string;
    submitted: string;
    completed: string;
    total: number;
    processed: number;
    error_text: string;
    processing_flag: boolean;
    identifier_type: IdentifierType;
}

export enum IdentifierType {MRN= 'MRN', PATID = 'PATID', DOCID = 'DOCID'}
