import {Component, ViewEncapsulation} from "@angular/core";
import {LoginService} from "../login/login.service";

@Component({
    selector: 'emerse-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class HomeComponent {
    
    constructor(public readonly loginService: LoginService) {

    }

}
