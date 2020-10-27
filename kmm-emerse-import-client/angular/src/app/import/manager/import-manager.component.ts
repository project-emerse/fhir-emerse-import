import {AfterViewInit, Component, ViewChild, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {PromptDialogService} from '@uukmm/ng-widget-toolkit';
import {
    actionText,
    EntryAction,
    EntryStatus,
    isValidAction,
    QueueEntry,
    STATUS_TEXT
} from '../../model/queue-entry.model';
import {noop, timer} from 'rxjs';

@Component({
    selector: 'emerse-import-manager',
    templateUrl: './import-manager.component.html',
    styleUrls: ['./import-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportManagerComponent implements AfterViewInit{

    readonly STATUS_TEXT = STATUS_TEXT.slice(0, EntryStatus.DELETED);

    @ViewChild('paginator') paginator: MatPaginator;

    readonly EntryAction = EntryAction;

    readonly filters: Set<EntryStatus> = new Set();

    dataSource = new MatTableDataSource<QueueEntry>();

    columns = ['submitted', 'completed', 'elapsed', 'total', 'processed', 'status', 'identifier_type', 'error_text'];

    busy = true;

    selected: QueueEntry = null;

    message: string;

    actionLabel: string;

    constructor(
        private readonly restService: RestService,
        private readonly promptDialogService: PromptDialogService) {
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
        this.dataSource.filterPredicate = data => this.includeEntry(data);
        this.dataSource.filter = "dummy";
        timer().subscribe(() => this.refresh());
    }

    trackBy(index: number, entry: QueueEntry): string {
        return entry.id;
    }

    setSelection(entry: QueueEntry) {
        this.selected = this.busy ? this.selected : entry;
    }

    action(action: EntryAction, warn: string = null): void {
        if (!warn) {
            return this.doAction(action);
        }

        this.promptDialogService.confirm(`Are you sure you want to ${warn} this entry?`, "Confirm Action")
            .subscribe(response => response ? this.doAction(action) : null);
    }

    private includeEntry(entry: QueueEntry): boolean {
        return this.filters.size === 0 || this.filters.has(entry.status);
    }

    hasFilter(status: EntryStatus): boolean {
        return this.filters.has(status);
    }

    updateFilter(status: EntryStatus, add: boolean): void {
        add ? this.filters.add(status) : this.filters.delete(status);
        this.dataSource._updateChangeSubscription();
    }

    clearFilters(): void {
        this.filters.clear();
        this.dataSource._updateChangeSubscription();
    }

    private doAction(action: EntryAction): void {
        const selected: QueueEntry = this.selected;
        const data: QueueEntry[] = this.dataSource.data;
        this.busy = true;
        this.clear(actionText(action) + " selected entry, please wait...");
        this.restService.entryAction(selected, action).subscribe(
            entry => this.update(data, entry),
            error => this.refresh(error));
    }

    private update(data: QueueEntry[], entry: QueueEntry): void {
        const index = entry == null ? -1 : data.findIndex(value => value.id === entry.id);
        const deleted = entry == null || entry.status === EntryStatus.DELETED;

        if (index === -1) {
            deleted ? noop() : data.push(entry);
        } else {
            deleted ? data.splice(index, 1) : data[index] = entry;
        }

        this.dataSource.data = data;
        this.selected = entry;
        this.message = null;
        this.busy = false;
    }

    supported(action: EntryAction): boolean {
        return this.selected && isValidAction(this.selected.status, action);
    }

    clear(message?: any): void {
        this.message = message;
        this.selected = null;
        this.dataSource.data = [];
    }

    refresh(message?: any): void {
        this.clear(message || "Fetching data...");
        this.busy = true;
        this.restService.fetchQueue().subscribe(entries => {
            this.message = null;
            this.dataSource.data = entries;
            this.busy = false;
        }, error => {
            this.message = error.message || error;
            this.busy = false;
        });
    }
}
