import {Component, ViewEncapsulation} from "@angular/core";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {catchError, tap} from 'rxjs/operators';
import {of, Subscription} from "rxjs";

@Component({
    selector: 'emerse-import-batch',
    templateUrl: './import-batch.component.html',
    styleUrls: ['./import-batch.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportBatchComponent {

    file: File;

    message: string;

    private target: any;

    constructor(private readonly httpClient: HttpClient) {
    }

    handleFileInput(event: any): void {
        this.target = event.target;
        const files: FileList = this.target.files;
        this.file = files.item(0);
    }

    indexFile(): void {
        const formData: FormData = new FormData();
        formData.append('fileKey', this.file, this.file.name);
        const subscription = this.httpClient
            .post("batch", formData, { headers: null })
            .pipe(
                tap(() => this.clear(), e => this.handleError(e))
            ).subscribe(() => subscription.unsubscribe());
    }

    clear(): void {
        if (this.target) {
            this.target.value = "";
            this.target = null;
            this.file = null;
        }
    }

    handleError(e: HttpErrorResponse): void {
        this.message = e.message;
    }
}
