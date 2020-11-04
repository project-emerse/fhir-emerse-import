export interface IndexStatus {
    collection: string;
    numDocs: number;
    maxDoc: number;
    deletedDocs: number;
    version: number;
    lastModified: number;
    size: string;
}
