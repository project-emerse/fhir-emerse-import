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

    busy: boolean;

    constructor(public readonly loginService: LoginService) {}

    ngAfterViewInit(): void {
        this.focus(this.usernameInput);
    }

    login(): void {
        this.message = "Authenticating, please wait...";
        this.busy = true;

        this.loginService.login(this.username, this.password).subscribe(success => {
            this.busy = false;
            this.message = null;

            if (!success) {
                this.message = "Invalid username or password.  Please try again.";
                this.password = "";
                this.focus(this.usernameInput);
            }
        })
    }

    focus(element: ElementRef | HTMLInputElement): void {
        const input = element instanceof ElementRef ? element.nativeElement : element;
        input.focus();
        input.select();
    }
}
