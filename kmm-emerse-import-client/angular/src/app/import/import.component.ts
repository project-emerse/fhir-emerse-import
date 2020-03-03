import {Component, HostListener, ViewEncapsulation} from "@angular/core";
import {LoginService} from "../login/login.service";

@Component({
    selector: 'emerse-import',
    templateUrl: './import.component.html',
    styleUrls: ['./import.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportComponent {
    constructor(private readonly loginService: LoginService){}

    @HostListener('document:keyup')
    @HostListener('document:click')
    private onActivity() {
        this.loginService.resetTimeout();
    }

}
