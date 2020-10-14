import {Component, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";
import {QueueEntry} from "./queue-entry.model";
import {MatTableDataSource} from '@angular/material/table';

@Component({
    selector: 'emerse-import-manager',
    templateUrl: './import-manager.component.html',
    styleUrls: ['./import-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportManagerComponent {

    dataSource = new MatTableDataSource<QueueEntry>();

    columns = ['submitted', 'completed', 'total', 'processed', 'error_text', 'processing_flag', 'identifier_type'];

    constructor(private readonly restService: RestService) {
        this.refresh();
    }

    trackBy(index: number, entry: QueueEntry): string {
        return entry.id;
    }

    refresh() {
        this.restService.fetchQueue().subscribe(entries => this.dataSource.data = entries);
    }
}
