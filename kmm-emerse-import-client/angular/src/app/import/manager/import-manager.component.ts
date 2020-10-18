import {AfterViewInit, Component, ViewChild, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";
import {EntryAction, QueueEntry, VALID_ACTIONS} from "./queue-entry.model";
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {SelectionChange, SelectionModel} from '@angular/cdk/collections';
import {PromptDialogService} from '@uukmm/ng-widget-toolkit';

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

    selection = new SelectionModel<QueueEntry>(false, []);

    loading = true;

    selected: QueueEntry;

    actionLabel: string;

    constructor(
        private readonly restService: RestService,
        private readonly promptDialogService: PromptDialogService) {
    }

    ngAfterViewInit(): void {
        this.dataSource.paginator = this.paginator;
        this.selection.changed.asObservable().subscribe(onselect => this.onSelect(onselect))
        this.refresh();
    }

    trackBy(index: number, entry: QueueEntry): string {
        return entry.ID;
    }

    onSelect(selection: SelectionChange<QueueEntry>): void {
        this.selected = selection.added[0];
    }

    action(action: EntryAction, warn: string = null): void {
        if (!warn) {
            return this.doAction(action);
        }

        this.promptDialogService.confirm(`Are you sure you want to ${warn} this entry?`, "Confirm Action")
            .subscribe(response => response ? this.doAction(action) : null);
    }

    private doAction(action: EntryAction): void {
        this.restService.entryAction(this.selected, action);
        this.refresh();
    }

    supported(action: EntryAction): boolean {
        return this.selected && VALID_ACTIONS[this.selected.STATUS].includes(action)
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
