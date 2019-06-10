import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Patient} from "@uukmm/ng-fhir-model/stu3";
import {Observable} from "rxjs";
import {Document} from "../model/document.model";

@Injectable({
    providedIn: "root"
})
export class RestService {

    private config: any;

    constructor(private readonly httpClient: HttpClient) {
        this.httpClient.get("config").subscribe(config => this.config = config);
    }

    getConfig(key: string): string {
        return this.config[key];
    }

    findPatient(mrn: string): Observable<Patient> {
        return <Observable<Patient>> this.httpClient.get(`patient/${mrn}`);
    }

    getDocuments(patientId: string): Observable<Document[]> {
        return <Observable<Document[]>> this.httpClient.get(`documents/${patientId}`);
    }
}
