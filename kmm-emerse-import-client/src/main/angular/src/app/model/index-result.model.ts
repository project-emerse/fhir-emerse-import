export interface IndexResult {
    succeeded: number;
    failed: number;
}

export class IndexResultUtil {

    static toString(result: IndexResult): string {
        return 'Indexing result: ' + (result ? `succeeded: ${result.succeeded}  failed: ${result.failed}`
            : 'an unspecified error has occurred.');
    }
}
