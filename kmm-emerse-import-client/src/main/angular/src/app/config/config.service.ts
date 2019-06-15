import {Injectable} from "@angular/core";
import {RestService} from "../rest/rest.service";
import {Observable, of} from "rxjs";
import {switchMap} from "rxjs/operators";

@Injectable({
    providedIn: "root"
})
export class ConfigService {

    private config: any;

    private readonly config$: Observable<any>;

    constructor(restService: RestService) {
        this.config$ = restService.getServerConfig();
        this.config$.subscribe(config => this.config = config);
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

}
