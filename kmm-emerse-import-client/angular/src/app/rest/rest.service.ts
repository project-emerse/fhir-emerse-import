import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {Observable, of, throwError} from "rxjs";
import {Document} from "../model/document.model";
import {catchError, map, shareReplay, switchMap, tap} from "rxjs/operators";
import {environment} from "../../environments/environment";
import {v4 as uuid} from "uuid";
import {IndexResult} from "../model/index-result.model";
import {EntryAction, IdentifierType, isValidAction, QueueEntry, statusText} from "../model/queue-entry.model";
import {LoggerService, LoggerStopwatch} from "@uukmm/ng-logger";
import {formatDuration, intervalToDuration} from 'date-fns';
import {IndexStatus} from '../model/index-status.model';

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
        return this.get("api/config");
    }

    getClientInfo(): Observable<any> {
        return this.get("assets/about.json");
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

    findPatient(id: string, idType: IdentifierType): Observable<Patient> {
        return this.get("api/patient" + this.formatId(id, idType));
    }

    getDocuments(id: string, idType: IdentifierType): Observable<Document[]> {
        return this.get("api/documents" + this.formatId(id, idType));
    }

    singleIndex(id: string, idType: IdentifierType): Observable<IndexResult> {
        return this.get("api/index" + this.formatId(id, idType));
    }

    private formatId(id: string, idType: IdentifierType): string {
        return `?id=${id}&type=${idType}`
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
                entries.forEach((entry, index) => entries[index] = this.transformEntry(entry))
                return entries;
            })
        );
    }

    private transformEntry(value: any): QueueEntry {
        const entry: any = {};
        Object.keys(value).forEach(key => entry[key.toLowerCase()] = value[key])
        entry.completed_date = entry.completed ? new Date(entry.completed) : null;
        entry.submitted_date = entry.submitted ? new Date(entry.submitted) : null;
        entry.status_text = statusText(entry.status);
        const duration: Duration = entry.elapsed == null ? null : intervalToDuration({start: 0, end: entry.elapsed});
        entry.elapsed_text = duration ? formatDuration(duration) : null;
        return entry;
    }

    entryAction(entry: QueueEntry, action: EntryAction): Observable<QueueEntry> {
        if (isValidAction(entry.status, action)) {
            return this.post("api/entry-action", {id: entry.id, action}).pipe(
                map(ent => this.transformEntry(ent))
            );
        } else {
            return of(null);
        }
    }

    private getEndpoint(url: string): string {
        return (url.startsWith("api") ? this.serverEndpoint : "") + url;
    }

    getIndexStatus(): Observable<any> {
        return this.get("api/status");
    }

    resetIndexes(documentsOnly: boolean): Observable<any> {
        return this.get("api/reset?documentsOnly=" + documentsOnly);
    }

    private get<T>(url: string): Observable<T> {
        const sw = new LoggerStopwatch(`GET operation: ${url}`, this.loggerService);
        return this.httpClient.get<T>(this.getEndpoint(url), {headers: this.createHeaders(true), responseType: "json"})
            .pipe(
                tap(sw),
                catchError(error => this.catchError(error)),
                shareReplay(1)
            );
    }

    private post<T>(url: string, body: any): Observable<T> {
        const sw = new LoggerStopwatch(`POST operation: ${url}`, this.loggerService);
        return this.httpClient.post<T>(this.getEndpoint(url), body, {headers: this.createHeaders(false), responseType: "json"})
            .pipe(
                tap(sw),
                catchError(error => this.catchError(error)),
                shareReplay(1)
            );
    }

    private createHeaders(nocache: boolean): HttpHeaders {
        let headers: any = {
            emerse_id: this.emerseId
        }

        if (nocache) {
            headers = {
                ...headers,
                "Cache-Control": ["no-store", "must-revalidate"],
                "Pragma": "no-cache",
                "Expires": "0"
            }
        }

        if (this.authorization) {
            headers = {
                ...headers,
                "authorization": this.authorization
            }
        }

        return new HttpHeaders(headers);
    }

    private catchError(error): Observable<any> {
        console.log(error);
        return throwError(error.error || error.message || error);
    }

}
