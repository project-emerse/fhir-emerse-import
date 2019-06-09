import {Injectable} from "@angular/core";

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    private authenticated: boolean;

    isAuthenticated(): boolean {
        return this.authenticated;
    }

    authenticate(username: string, password: string): boolean {
        this.authenticated = username != "bad";
        return this.isAuthenticated();
    }

    logout(): void {
        this.authenticated = false;
    }
}
