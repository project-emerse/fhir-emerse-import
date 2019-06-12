import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {filter, tap} from "rxjs/operators";
import {RestService} from "../rest/rest.service";

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    private authenticated: boolean;

    constructor(
        private readonly restService: RestService) {}

    isAuthenticated(): boolean {
        return this.authenticated;
    }

    login(username: string, password: string): Observable<boolean> {
        return this.restService.login(username, password).pipe(
            tap(success => this.authenticated = success)
        );
    }

    logout(): void {
        this.restService.logout().pipe(
            filter(success => success))
            .subscribe(() => {
                this.authenticated = false;
            });
    }
}
