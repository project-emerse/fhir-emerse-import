import {Component, ViewEncapsulation} from "@angular/core";
import {LoginService} from "../login/login.service";
import {ConfigService} from "../config/config.service";
import {Observable} from "rxjs";

@Component({
    selector: 'emerse-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class HomeComponent {

    readonly emerseUrl: Observable<string>;

    constructor(
        public readonly loginService: LoginService,
        configService: ConfigService) {
        this.emerseUrl = configService.getSettingAsync("emerse.home.url");
    }

}
