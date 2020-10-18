import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {combineLatest, Observable, of} from "rxjs";
import {catchError, map, switchMap} from "rxjs/operators";

@Injectable({
    providedIn: "root"
})
export class MockRestService {

    constructor(private readonly httpClient: HttpClient) {}

    login(username: string, password: string): Observable<boolean> {
        return of(username !== "bad");
    }

    logout(): Observable<boolean> {
        return of(true);
    }

    getServerConfig(): Observable<any> {
        return this.getMockObject("mock-server-config.json");
    }

    findPatient(mrn: string): Observable<Patient> {
        return this.getMockObject("mock-patient-stu3.jsonx");
    }

    getDocuments(patientId: string): Observable<Document[]> {
        return this.getMockObject("mock-documents.json");
    }

    private getMockObject<T>(file: string): Observable<T> {
        return this.getMockResource(file)
            .pipe(
                catchError(error => this.catchError(error)),
                map(text => text ? <T> JSON.parse(text) : null)
            );
    }

    private getMockResource(file: string): Observable<string> {
        return this.httpClient.get(`mock/${file}`, {responseType: "text"})
            .pipe(
                catchError(error => this.catchError(error)),
                switchMap(text => this.handleImport(text))
            );
    }

    private handleImport(text: string): Observable<string> {
        const i = text.indexOf("@@");
        const j = i === -1 ? -1 : text.indexOf("@@", i + 2);

        if (i === -1 || j === -1) {
            return of(text);
        }

        const imp = text.substring(i + 2, j);

        return combineLatest([
            of(text.substring(0, i)),
            this.getMockResource(imp),
            of(text.substring(j + 2))
        ]).pipe(
            switchMap(text => this.handleImport(text[0] + this.escapeText(text[1]) + text[2])
        ));
    }

    private escapeText(text: string): string {
        text = JSON.stringify(text);
        return text.substring(1, text.length - 1);
    }

    private catchError(error): Observable<any> {
        console.log(error);
        return of(null);
    }
}
