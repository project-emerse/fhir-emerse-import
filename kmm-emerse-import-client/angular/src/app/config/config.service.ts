import {Injectable} from "@angular/core";
import {RestService} from "../rest/rest.service";
import {combineLatest, Observable, of} from "rxjs";
import {map, switchMap, take} from "rxjs/operators";
import {AlertDialogService, AlertSeverity} from '@uukmm/ng-widget-toolkit';

const ERROR_TITLE = "Cannot Continue";

const ERROR_TEXT = "Unable to retrieve configuration information.  The EMERSE-IT server may be down."

/**
 * Retrieve client settings from the server.
 */
@Injectable({
    providedIn: "root"
})
export class ConfigService {

    private config: any;

    private readonly config$: Observable<any>;

    constructor(
        restService: RestService,
        alertDialogService: AlertDialogService) {
        this.config$ = combineLatest([restService.getServerConfig(), restService.getClientInfo()]).pipe(
            map(([serverConfig, clientInfo]) => Object.assign({}, clientInfo, serverConfig)),
            take(1));
        this.config$.subscribe(
                config => this.config = config,
                () => {
                    alertDialogService.show({title: ERROR_TITLE, message: ERROR_TEXT, severity: AlertSeverity.FATAL});
                });
    }

    isLoaded(): boolean {
        return this.config != null;
    }

    getSettingAsync(name: string): Observable<string> {
        return this.config$.pipe(switchMap(config => of(config[name])));
    }

    getSetting(name: string): string {
        return this.config[name];
    }

    error(error: any): void {

    }
}
