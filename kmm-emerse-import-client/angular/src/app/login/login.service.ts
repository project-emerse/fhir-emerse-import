import {Injectable} from "@angular/core";
import {BehaviorSubject, Observable, Subject} from "rxjs";
import {filter, tap} from "rxjs/operators";
import {RestService} from "../rest/rest.service";
import {ConfigService} from "../config/config.service";
import {LogMonitorService} from '@uukmm/ng-logger';

/**
 * Handles interaction with login REST service and inactivity timeout.
 */
@Injectable({
    providedIn: 'root'
})
export class LoginService {

    private authenticated: boolean;

    private timeout: number;

    private remaining: number;

    private interval: any;

    private notifier: Subject<number> = new BehaviorSubject(-1);

    /**
     * Retrieves the application timeout setting from the server config.
     *
     * @param restService The REST service to perform authentication.
     * @param configService The application config service.
     */
    constructor(
        private readonly restService: RestService,
        private readonly logMonitor: LogMonitorService,
        configService: ConfigService
    ) {
        configService.getSettingAsync("app.timeout.seconds").subscribe(timeout => {
            this.timeout = Number(timeout);
            this.timeout = isNaN(this.timeout) ? 0 : this.timeout;
        });
    }

    /**
     * Starts (or resets) the inactivity timer.
     */
    private startTimer(): void {
        this.resetTimeout();
        this.interval = this.timeout <= 0 ? null : this.interval ? this.interval : setInterval(() => this.timerHandler(), 1000);
    }

    /**
     * Handles inactivity timer events.  Decrements the remaining time and forces logout when it reaches 0.
     */
    private timerHandler(): void {
        if (this.setRemainingTime(this.remaining - 1) <= 0) {
            this.logout();
        }
    }

    /**
     * Stops the inactivity timer if it is active.
     */
    private stopTimer(): void {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
            this.setRemainingTime(-1);
        }
    }

    /**
     * Returns the remaining time.
     */
    getRemainingTime(): number {
        return this.remaining;
    }

    /**
     * Updates the remaining time and causes the notifier to emit the value.
     *
     * @param remaining The new remaining time (defaults to initial value).
     */
    private setRemainingTime(remaining: number = this.timeout): number {
        this.notifier.next(this.remaining = remaining);
        return this.remaining;
    }

    /**
     * Resets the remaining time to its initial value (i.e., the timeout setting).
     */
    resetTimeout(): void {
        this.setRemainingTime();
    }

    /**
     * Use to monitor remaining time.  Emits the time remaining each time it changes (contingent on the filter parameter).
     *
     * @param threshold Use to filter out events until the remaining time reaches this threshold.
     *      Defaults to no filter (so all timer events are emitted).
     * @return An observable of the remaining time.
     */
    onTimer(threshold?: number): Observable<number> {
        return this.notifier.pipe(
            filter(remaining => this.interval && remaining >= 0 && (threshold == null || remaining < threshold))
        );
    }

    /**
     * True if the user successfully authenticated.
     */
    isAuthenticated(): boolean {
        return this.authenticated;
    }

    /**
     * Attempt to authenticate the user.  If successful, starts the inactivity timer.
     *
     * @param username The username.
     * @param password The password.
     * @return An observable that emits true if authentication was successful.
     */
    login(username: string, password: string): Observable<boolean> {
        return this.restService.login(username, password).pipe(
            tap(success => {
                this.authenticated = success;
                success ? this.startTimer() : this.stopTimer();
            })
        );
    }

    /**
     * Log out the user.
     */
    logout(): void {
        this.authenticated = false;
        this.stopTimer();
        this.logMonitor.clear();
        // this.restService.logout();
    }
}
