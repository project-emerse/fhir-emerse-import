import {AfterViewInit, Component, ViewChild, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";
import {QueueEntry} from "./queue-entry.model";
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {SelectionModel} from '@angular/cdk/collections';

@Component({
    selector: 'emerse-import-manager',
    templateUrl: './import-manager.component.html',
    styleUrls: ['./import-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportManagerComponent implements AfterViewInit{

    @ViewChild('paginator') paginator: MatPaginator;

    dataSource = new MatTableDataSource<QueueEntry>();

    columns = ['submitted', 'completed', 'total', 'processed', 'processing_flag', 'identifier_type', 'error_text'];

    selection = new SelectionModel<QueueEntry>(false, []);

    loading = true;

    constructor(private readonly restService: RestService) {
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
        this.selection.changed.asObservable().subscribe(onselect => this.onSelect(onselect))
        this.refresh();
    }

    trackBy(index: number, entry: QueueEntry): string {
        return entry.ID;
    }

    onSelect(selection): void {

    }

    refresh(): void {
        this.dataSource.data = [];
        this.loading = true;
        this.restService.fetchQueue().subscribe(entries => {
            this.dataSource.data = entries;
            this.dataSource._updateChangeSubscription();
            this.loading = false;
        });
    }
}
