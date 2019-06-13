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
        return this.invoke("api/config").pipe(shareReplay(1));
    }

    login(username: string, password: string): Observable<boolean> {
        const headers = new HttpHeaders({
            authorization: 'Basic ' + btoa(username + ':' + password)
        });
        return this.invoke("api/login", null, {headers}).pipe(
            map(response => !!response),
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

    batchIndex(formData): Observable<any> {
        return this.invoke("api/batch", formData);
    }

    private invoke<T>(url: string, body?: any, options: any = {}): Observable<T> {
        const result = body ? this.httpClient.post(url, body, options) : this.httpClient.get(url, options);
        return <Observable<T>> result.pipe(take(1));
    }
}
