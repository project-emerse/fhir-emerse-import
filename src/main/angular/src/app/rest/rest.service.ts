import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {Observable, of} from "rxjs";
import {Document} from "../model/document.model";
import {catchError, map, shareReplay, switchMap, take} from "rxjs/operators";

@Injectable({
    providedIn: "root"
})
export class RestService {

    constructor(private readonly httpClient: HttpClient) {}

    getConfig(): Observable<any> {
        return this.get("api/config").pipe(shareReplay(1));
    }

    login(username: string, password: string): Observable<boolean> {
        const headers = new HttpHeaders({
            authorization: 'Basic ' + btoa(username + ':' + password)
        });
        return this.get("api/login", headers).pipe(
            map(response => !!response),
            catchError(() => of(false))
        );
    }

    logout(): Observable<boolean> {
        return this.post("api/logout", null).pipe(
            switchMap(() => of(true)),
            catchError(() => of(false))
        )
    }

    findPatient(mrn: string): Observable<Patient> {
        return this.get(`api/patient/${mrn}`);
    }

    getDocuments(patientId: string): Observable<Document[]> {
        return this.get(`api/documents/${patientId}`);
    }

    batchIndex(formData): Observable<any> {
        return this.post("api/batch", formData);
    }

    private get<T>(url: string, headers?: HttpHeaders): Observable<T> {
        return this.httpClient.get<T>(url, {headers, responseType: "json"}).pipe(take(1));
    }

    private post<T>(url: string, body: any, headers?: HttpHeaders): Observable<T> {
        return this.httpClient.post<T>(url, body, {headers, responseType: "json"}).pipe(take(1));
    }
}
