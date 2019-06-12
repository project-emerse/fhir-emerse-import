import {Injectable} from "@angular/core";
import {RestService} from "../rest/rest.service";
import {Observable, of} from "rxjs";
import {switchMap} from "rxjs/operators";

@Injectable({
    providedIn: "root"
})
export class ConfigService {

    private readonly configObservable: Observable<any>;

    private config: any;

    constructor(restService: RestService) {
        this.configObservable = restService.getConfig();
        this.configObservable.subscribe(config => this.config = config);
    }

    getSetting(name: string): string {
        return this.config[name];
    }

    getSettingAsync(name: string): Observable<string> {
        return this.configObservable.pipe(
            switchMap(config => of(config[name]))
        );
    }
}
