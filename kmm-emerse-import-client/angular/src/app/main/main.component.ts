import {Component, HostListener, ViewEncapsulation} from "@angular/core";
import {LoginService} from "../login/login.service";

@Component({
    selector: 'emerse-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class MainComponent {
    constructor(private readonly loginService: LoginService){}

    @HostListener('document:keyup')
    @HostListener('document:click')
    private onActivity() {
        this.loginService.resetTimeout();
    }

}
