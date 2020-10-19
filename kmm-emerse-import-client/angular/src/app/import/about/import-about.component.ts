import {Component, ViewEncapsulation} from "@angular/core";
import {ConfigService} from "../../config/config.service";

@Component({
    selector: 'emerse-import-about',
    templateUrl: './import-about.component.html',
    styleUrls: ['./import-about.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportAboutComponent {

    constructor(private readonly configService: ConfigService) {
    }

    get(setting: string): string {
        return this.configService.getSetting(setting);
    }
}
