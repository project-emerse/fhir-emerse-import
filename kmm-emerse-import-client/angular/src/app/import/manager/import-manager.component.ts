import {AfterViewInit, Component, ViewChild, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {PromptDialogService} from '@uukmm/ng-widget-toolkit';
import {EntryAction, isValidAction, QueueEntry} from '../../model/queue-entry.model';
import {timer} from 'rxjs';

@Component({
    selector: 'emerse-import-manager',
    templateUrl: './import-manager.component.html',
    styleUrls: ['./import-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportManagerComponent implements AfterViewInit{

    @ViewChild('paginator') paginator: MatPaginator;

    readonly EntryAction = EntryAction;

    dataSource = new MatTableDataSource<QueueEntry>();

    columns = ['submitted', 'completed', 'elapsed', 'total', 'processed', 'status', 'identifier_type', 'error_text'];

    loading = true;

    selected: QueueEntry = null;

    message: string;

    actionLabel: string;

    constructor(
        private readonly restService: RestService,
        private readonly promptDialogService: PromptDialogService) {
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
        timer().subscribe(() => this.refresh());
    }

    trackBy(index: number, entry: QueueEntry): string {
        return entry.ID;
    }

    setSelection(entry: QueueEntry) {
        this.selected = this.loading ? null : entry;
    }

    action(action: EntryAction, warn: string = null): void {
        if (!warn) {
            return this.doAction(action);
        }

        this.promptDialogService.confirm(`Are you sure you want to ${warn} this entry?`, "Confirm Action")
            .subscribe(response => response ? this.doAction(action) : null);
    }

    private doAction(action: EntryAction): void {
        const selected: QueueEntry = this.selected;
        this.clear("Performing selected operation...");
        this.restService.entryAction(selected, action).subscribe(
            () => this.refresh(),
            error => this.refresh(error));
    }

    supported(action: EntryAction): boolean {
        return this.selected && isValidAction(this.selected.STATUS, action);
    }

    clear(message?: any): void {
        this.message = message;
        this.selected = null;
        this.dataSource.data = [];
    }

    refresh(message?: any): void {
        this.clear(message || "Fetching data...");
        this.loading = true;
        this.restService.fetchQueue().subscribe(entries => {
            this.message = null;
            this.dataSource.data = entries;
            this.dataSource._updateChangeSubscription();
            this.loading = false;
        });
    }
}
