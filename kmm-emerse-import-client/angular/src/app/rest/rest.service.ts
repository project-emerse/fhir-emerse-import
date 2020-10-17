import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {Observable, of} from "rxjs";
import {Document} from "../model/document.model";
import {catchError, map, shareReplay, switchMap, tap} from "rxjs/operators";
import {environment} from "../../environments/environment";
import {v4 as uuid} from "uuid";
import {IndexResult} from "../model/index-result.model";
import {QueueEntry} from "../import/manager/queue-entry.model";
import {LoggerService, LoggerStopwatch} from "@uukmm/ng-logger";

@Injectable({
    providedIn: "root"
})
export class RestService {

    private readonly serverEndpoint: string;

    private readonly emerseId = uuid();

    private authorization: string;

    constructor(
        private readonly httpClient: HttpClient,
        private readonly loggerService: LoggerService
    ) {
        this.serverEndpoint = environment.serverEndpoint;
        this.serverEndpoint = this.serverEndpoint.endsWith("/") ? this.serverEndpoint : this.serverEndpoint + "/";
    }

    getServerConfig(): Observable<any> {
        return this.get("api/config").pipe(shareReplay(1));
    }

    login(username: string, password: string): Observable<boolean> {
        this.authorization = "Basic " + btoa(username + ":" + password);
        return this.get("api/login").pipe(
            map(response => !!response),
            catchError(() => of(false))
        );
    }

    logout(): Observable<boolean> {
        this.authorization = null;

        return this.post("api/logout", null).pipe(
            switchMap(() => of(true)),
            catchError(() => of(false))
        )
    }

    findPatient(mrn: string): Observable<Patient> {
        return this.get(`api/patient?mrn=${mrn}`);
    }

    getDocuments(patid: string): Observable<Document[]> {
        return this.get(`api/documents?patid=${patid}`);
    }

    singleIndex(mrn: string): Observable<IndexResult> {
        return this.get(`api/index?mrn=${mrn}`);
    }

    batchIndexForeground(formData): Observable<IndexResult> {
        return this.post("api/batch-fg", formData);
    }

    batchIndexBackground(formData): Observable<boolean> {
        return this.post("api/batch-bg", formData);
    }

    fetchQueue(): Observable<QueueEntry[]> {
        return this.get("api/queue").pipe(
            map((entries: QueueEntry[]) => {
                entries.forEach(entry => {
                    entry.COMPLETED_DATE = new Date(entry.COMPLETED);
                    entry.SUBMITTED_DATE = new Date(entry.SUBMITTED);
                })
                return entries;
            })
        );
    }

    private get<T>(url: string, headers?: HttpHeaders): Observable<T> {
        headers = this.addHeaders(headers);
        const sw = new LoggerStopwatch(`GET operation: ${url}`, this.loggerService);
        return this.httpClient.get<T>(this.serverEndpoint + url, {headers, responseType: "json"})
            .pipe(
                tap(sw),
                catchError(error => this.catchError(error)),
                shareReplay(1)
            );
    }

    private post<T>(url: string, body: any, headers?: HttpHeaders): Observable<T> {
        headers = this.addHeaders(headers);
        const sw = new LoggerStopwatch(`POST operation: ${url}`, this.loggerService);
        return this.httpClient.post<T>(this.serverEndpoint + url, body, {headers, responseType: "json"})
            .pipe(
                tap(sw),
                catchError(error => this.catchError(error)),
                shareReplay(1)
            );
    }

    private addHeaders(headers?: HttpHeaders): HttpHeaders {
        headers = headers || new HttpHeaders();
        headers = headers.set("emerse_id", this.emerseId);

        if (this.authorization) {
            headers = headers.set("authorization", this.authorization);
        }

        return headers;
    }

    private catchError(error): Observable<any> {
        console.log(error);
        return of(null);
    }

}
