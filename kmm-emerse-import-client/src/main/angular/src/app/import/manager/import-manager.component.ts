import {Component, ViewEncapsulation} from "@angular/core";
import {RestService} from "../../rest/rest.service";

@Component({
    selector: 'emerse-import-manager',
    templateUrl: './import-manager.component.html',
    styleUrls: ['./import-manager.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportManagerComponent {

    constructor(private readonly restService: RestService) {
    }

}
