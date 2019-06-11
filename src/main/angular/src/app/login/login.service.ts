import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable, of} from "rxjs";
import {map, switchMap} from "rxjs/operators";

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    private authenticated: boolean;

    constructor(private readonly httpClient: HttpClient) {}

    isAuthenticated(): boolean {
        return this.authenticated;
    }

    authenticate(username: string, password: string): Observable<boolean> {
        const headers = new HttpHeaders({
            authorization: 'Basic ' + btoa(username + ':' + password)
        } );

        return this.httpClient.get("api/user", {headers}).pipe(
            switchMap(response => of(this.authenticated = response['name'] !== null))
        );
    }

    logout(): void {
        this.httpClient.post("api/logout", {})
            .subscribe(() => this.authenticated = false);
    }
}
