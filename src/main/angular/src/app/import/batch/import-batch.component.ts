import {Component, ViewEncapsulation} from "@angular/core";
import {HttpErrorResponse} from "@angular/common/http";
import {RestService} from "../../rest/rest.service";

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

    constructor(private readonly restService: RestService) {
    }

    handleFileInput(event: any): void {
        this.target = event.target;
        const files: FileList = this.target.files;
        this.file = files.item(0);
    }

    indexFile(): void {
        const formData: FormData = new FormData();
        formData.append('file', this.file, this.file.name);
        this.restService
            .batchIndex(formData)
            .subscribe({
                next: count => this.clear(`Indexed ${count} patient(s).`),
                error: err => this.handleError(err)
            });
    }

    clear(message?: string): void {
        if (this.target) {
            this.target.value = "";
            this.target = null;
            this.file = null;
        }

        this.message = message;
    }

    handleError(e: HttpErrorResponse): void {
        this.message = e.message;
    }
}
