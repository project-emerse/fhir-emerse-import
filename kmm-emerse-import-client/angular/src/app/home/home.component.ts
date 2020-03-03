import {Component, ViewChild, ViewEncapsulation} from "@angular/core";
import {LoginService} from "../login/login.service";
import {ConfigService} from "../config/config.service";
import {MatTooltip} from "@angular/material";
import {LogMonitorDialogService} from "@uukmm/ng-logger";

@Component({
    selector: 'emerse-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class HomeComponent {

    @ViewChild("timeoutTooltip", {static: false})
    timeoutTooltip: MatTooltip;

    constructor(
        public readonly loginService: LoginService,
        public readonly configService: ConfigService) {
        this.loginService.onTimer().subscribe(value => {
            if (this.timeoutTooltip != null) {
                if (value < 30 && value > 0) {
                    this.timeoutTooltip.message = `Application will timeout in ${value} second(s)`;
                    this.timeoutTooltip.show();
                } else {
                    this.timeoutTooltip.message = null;
                    this.timeoutTooltip.hide();
                }
            }
        })
     }

    emerseUrl(): string {
        return this.configService.getSetting("emerse.home.url");
    }

    getTimeoutTooltip(): string {
        const timeRemaining = this.loginService.getTimeRemaining();
        const show = timeRemaining < 30;
         return show ? `Application will timeout in ${timeRemaining} second(s)` : null;
    }

}
