import {Injectable} from "@angular/core";
import {BehaviorSubject, Observable, Subject} from "rxjs";
import {filter, tap} from "rxjs/operators";
import {RestService} from "../rest/rest.service";
import {ConfigService} from "../config/config.service";

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    private authenticated: boolean;

    private timeout: number;

    private countdown: number;

    private interval: any;

    private notifier: Subject<number> = new BehaviorSubject(-1);

    constructor(
            private readonly restService: RestService,
            configService: ConfigService) {
        configService.getSettingAsync("app.timeout.seconds").subscribe(timeout => {
            this.timeout = Number(timeout);
            this.timeout = isNaN(this.timeout) ? 0 : this.timeout;
        });
    }


    private startTimer(): void {
        this.resetTimeout();
        this.interval = this.timeout <= 0 ? null : this.interval ? this.interval : setInterval(() => this.timerHandler(), 1000);
    }

    private timerHandler(): void {
        if (this.setCountdown(this.countdown - 1) <= 0) {
            this.logout();
        }
    }

    private stopTimer(): void {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
            this.setCountdown(-1);
        }
    }

    private setCountdown(timeout: number = this.timeout): number {
        this.notifier.next(this.countdown = timeout);
        return this.countdown;
    }

    resetTimeout(): void {
        this.setCountdown();
    }

    onTimer(after?: number): Observable<number> {
        return this.notifier.pipe(
            filter(countdown => this.interval && countdown >= 0 && (after == null || countdown < after))
        );
    }

    getTimeRemaining(): number {
        return this.countdown;
    }

    isAuthenticated(): boolean {
        return this.authenticated;
    }

    login(username: string, password: string): Observable<boolean> {
        return this.restService.login(username, password).pipe(
            tap(success => {
                this.authenticated = success;
                success ? this.startTimer() : this.stopTimer();
            })
        );
    }

    logout(): void {
        this.authenticated = false;
        this.stopTimer();
        this.restService.logout();
    }
}
