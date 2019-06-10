import {Component, ViewEncapsulation} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {combineLatest, Observable} from "rxjs";
import {FhirStu3Util, Identifier, Patient} from "@uukmm/ng-fhir-model/stu3";
import {RestService} from "../../rest/rest.service";
import {filter, switchMap} from "rxjs/operators";
import {Document} from "../../model/document.model";
import {PatientDemographics} from "../../model/patient-demographics.model";

@Component({
    selector: 'emerse-import-single',
    templateUrl: './import-single.component.html',
    styleUrls: ['./import-single.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportSingleComponent {

    mrn: string;

    documents: Document[];

    demographics: PatientDemographics;

    patient: Patient;

    htmlBody: string;

    textBody: string;

    private fakeBody: string;

    private target: any;

    constructor(
        private readonly restService: RestService,
        httpClient: HttpClient) {
        const result: Observable<string> = httpClient.get("assets/test/cda.html", {responseType: "text"});
        result.subscribe(html => this.fakeBody = html);
    }

    search(): void {
        const patient: Observable<Patient> = this.restService.findPatient(this.mrn);

        const documents: Observable<Document[]> = patient.pipe(
            filter(patient => patient != null),
            switchMap(patient => this.restService.getDocuments(patient.id))
        );

        combineLatest(patient, documents).subscribe(([patient, documents]) => {
            this.extractDemographics(patient);
            this.documents = documents;
        });

    }

    private extractDemographics(patient: Patient): PatientDemographics {
        const mrn: Identifier = FhirStu3Util.getIdentifier(patient, this.restService.getConfig("fhir.mrn.system"));

        return {
            name: FhirStu3Util.formatName(patient.name[0]),
            mrn: mrn ? mrn.value : null,
            dob: patient.birthDate.toString(),
            gender: patient.gender
        }
    }

    private searchForPatient(mrn: string): Observable<Patient> {
        return this.restService.findPatient(mrn);
    }

    private searchForDocuments(patient: Patient): any {

    }

    index(): void {

    }

    clear(): void {
        this.documents = null;
        this.demographics = null;
        this.mrn = null;
    }
    documentSelected(event: any, document: Document): void {
        this.htmlBody = document.isHtml ? document.body : null;
        this.textBody = !document.isHtml ? document.body : null;

        if (this.target) {
            this.target.removeAttribute("selected");
        }

        this.target = event.currentTarget;
        this.target.setAttribute("selected", true)
    }

}
