import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {Observable, of} from "rxjs";
import {Document} from "../model/document.model";
import {catchError, map, shareReplay, switchMap, take} from "rxjs/operators";
import {environment} from "../../environments/environment";

@Injectable({
    providedIn: "root"
})
export class RestService {

    private readonly serverEndpoint: string;

    constructor(private readonly httpClient: HttpClient) {
        this.serverEndpoint = environment.serverEndpoint;
        this.serverEndpoint = this.serverEndpoint.endsWith("/") ? this.serverEndpoint : this.serverEndpoint + "/";
    }

    getServerConfig(): Observable<any> {
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
        return this.httpClient.get<T>(this.serverEndpoint + url, {headers, responseType: "json"})
            .pipe(
                catchError(error => this.catchError(error)),
                take(1)
            );
    }

    private post<T>(url: string, body: any, headers?: HttpHeaders): Observable<T> {
        return this.httpClient.post<T>(this.serverEndpoint + url, body, {headers, responseType: "json"})
            .pipe(
                catchError(error => this.catchError(error)),
                take(1)
            );
    }

    private catchError(error): Observable<any> {
        console.log(error);
        return of(null);
    }

}