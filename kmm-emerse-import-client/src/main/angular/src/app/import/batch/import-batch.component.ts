import {Component, ViewEncapsulation} from "@angular/core";
import {HttpErrorResponse} from "@angular/common/http";
import {RestService} from "../../rest/rest.service";
import {MatRadioChange} from "@angular/material/radio";
import {IndexResultUtil} from "../../model/index-result.model";

@Component({
    selector: 'emerse-import-batch',
    templateUrl: './import-batch.component.html',
    styleUrls: ['./import-batch.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportBatchComponent {

    file: File;

    type: string;

    message: string;

    private target: any;

    constructor(private readonly restService: RestService) {
    }

    handleFileInput(event: any): void {
        this.target = event.target;
        const files: FileList = this.target.files;
        this.file = files.item(0);
    }

    handleIdFormat(event: MatRadioChange): void {
        this.type = event.source.value;
    }

    indexFile(): void {
        this.message = "Indexing...";
        const formData: FormData = new FormData();
        formData.set('file', this.file, this.file.name);
        formData.set('type', this.type);
        this.restService
            .batchIndex(formData)
            .subscribe({
                next: result => this.clear(IndexResultUtil.toString(result)),
                error: err => this.handleError(err)
            });
    }

    clear(message?: string): void {
        if (this.target) {
            this.target.value = "";
            this.target = null;
            this.file = null;
        }

        this.type = null;
        this.message = message;
    }

    handleError(e: HttpErrorResponse): void {
        this.message = e.message;
    }
}
