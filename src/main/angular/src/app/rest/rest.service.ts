import {Injectable} from "@angular/core";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {Observable, of} from "rxjs";
import {Document} from "../model/document.model";
import {catchError, switchMap, take} from "rxjs/operators";

@Injectable({
    providedIn: "root"
})
export class RestService {

    private config: any;

    constructor(private readonly httpClient: HttpClient) {
        this.invoke("api/config").subscribe(config => this.config = config);
    }

    getConfig(key: string): string {
        return this.config[key];
    }

    login(username: string, password: string): Observable<boolean> {
        const payload = new HttpParams()
            .set('username', username)
            .set('password', password);
        return this.invoke("api/login", payload).pipe(
            switchMap(() => of(true)),
            catchError(() => of(false))
        );
    }

    logout(): Observable<boolean> {
        return this.invoke("api/logout", {}).pipe(
            switchMap(() => of(true)),
            catchError(() => of(false))
        )
    }

    findPatient(mrn: string): Observable<Patient> {
        return this.invoke(`api/patient/${mrn}`);
    }

    getDocuments(patientId: string): Observable<Document[]> {
        return this.invoke(`api/documents/${patientId}`);
    }

    private invoke<T>(url: string, payload?: any): Observable<T> {
        const result = payload ? this.httpClient.post(url, payload, {}) : this.httpClient.get(url);
        return <Observable<T>> result.pipe(take(1));
    }
}
