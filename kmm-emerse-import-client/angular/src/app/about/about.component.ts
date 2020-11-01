import {Component, ViewEncapsulation} from "@angular/core";
import {ConfigService} from "../config/config.service";

@Component({
    selector: 'emerse-about',
    templateUrl: './about.component.html',
    styleUrls: ['./about.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class AboutComponent {

    constructor(private readonly configService: ConfigService) {
    }

    get(setting: string): string {
        return this.configService.getSetting(setting) || "unavailable";
    }
}
