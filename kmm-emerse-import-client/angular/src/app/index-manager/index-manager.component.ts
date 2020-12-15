import {AfterViewInit, Component, ViewEncapsulation} from "@angular/core";
import {RestService} from "../rest/rest.service";
import {MatTableDataSource} from '@angular/material/table';
import {IndexStatus} from '../model/index-status.model';
import {PromptDialogService} from '@uukmm/ng-widget-toolkit';
import {timer} from 'rxjs';

const WARNING_DOCS_ONLY = 'You are about to delete all entries in the documents index.'
const WARNING_BOTH = 'You are about to delete all entries in the documents and patient indexes.'
const WARNING_CONFIRM = '\nAre you sure you want to do this?' +
    ''
@Component({
    selector: 'emerse-index-manager',
    templateUrl: './index-manager.component.html',
    styleUrls: ['./index-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class IndexManagerComponent implements AfterViewInit {

    dataSource = new MatTableDataSource<IndexStatus>();

    columns = ['collection', 'numdocs', 'size', 'lastmodified'];

    message: string;

    busy: boolean;

    constructor(
        private readonly restService: RestService,
        private readonly promptDialogService: PromptDialogService) {
    }

    ngAfterViewInit(): void {
        timer().subscribe(() => this.refresh());
    }

    formatDate(value: number): String {
        return value == null ? "" : new Date(value).toLocaleString();
    }

    refresh(): void {
        this.message = null;
        this.busy = true;
        this.restService.getIndexStatus().subscribe(status => {
            this.dataSource = status;
            this.busy = false;
        });
    }

    reset(documentsOnly: boolean): void {
        this.promptDialogService.confirm((documentsOnly ? WARNING_DOCS_ONLY : WARNING_BOTH) + WARNING_CONFIRM, 'WARNING!!!')
            .subscribe(confirmed => {
                if (confirmed) {
                    this.restService.resetIndexes(documentsOnly).subscribe(
                        () => this.refresh(),
                        error => this.message = error);
                }
            })
    }
}
