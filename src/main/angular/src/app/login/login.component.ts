import {AfterViewInit, Component, ElementRef, ViewChild, ViewEncapsulation} from "@angular/core";
import {LoginService} from "./login.service";

@Component({
    selector: 'emerse-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class LoginComponent implements AfterViewInit {

    @ViewChild("usernameInput") usernameInput: ElementRef;

    @ViewChild("passwordInput") passwordInput: ElementRef;

    username: string;

    password: string;

    message: string;

    constructor(public readonly loginService: LoginService) {
    }

    ngAfterViewInit(): void {
        this.focus(this.usernameInput);
    }

    login() {
        if (!this.loginService.authenticate(this.username, this.password)) {
            this.message = "Invalid username or password.  Please try again.";
            this.password = "";
            this.focus(this.usernameInput);
        }
    }

    focus(element: ElementRef | HTMLInputElement) {
        const input = element instanceof ElementRef ? element.nativeElement : element;
        input.focus();
        input.select();
    }
}
