import {Component, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";
import {QueueEntry} from "./queue-entry.model";

@Component({
    selector: 'emerse-import-manager',
    templateUrl: './import-manager.component.html',
    styleUrls: ['./import-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportManagerComponent {

    dataSource: QueueEntry[];

    columns = ['submitted', 'completed', 'total', 'processed', 'error_text', 'processing', 'type'];

    constructor(private readonly restService: RestService) {
    }

}
