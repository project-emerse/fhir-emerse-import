import {Component, ViewEncapsulation} from "@angular/core";
import {combineLatest, Observable, of} from "rxjs";
import {FhirStu3Util, HumanName, Identifier, Patient} from "@uukmm/ng-fhir-model/stu3";
import {RestService} from "../../rest/rest.service";
import {catchError, filter, switchMap, tap} from "rxjs/operators";
import {Document} from "../../model/document.model";
import {PatientDemographics} from "../../model/patient-demographics.model";
import {ConfigService} from "../../config/config.service";

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

    message: string;

    searching: boolean;

    private target: any;

    constructor(
        private readonly restService: RestService,
        private readonly configService: ConfigService) {
    }

    search(): void {
        this.message = "Searching...";
        this.searching = true;
        const patient: Observable<Patient> = this.restService.findPatient(this.mrn);

        const documents: Observable<Document[]> = patient.pipe(
            tap(patient => {
                this.message = patient == null ? "No patient found.  Please try again." : null;
                this.searching = false;
            }),
            filter(patient => patient != null),
            switchMap(patient => this.restService.getDocuments(patient.id))
        );

        combineLatest(patient, documents).subscribe(([patient, documents]) => {
            this.extractDemographics(patient);
            this.documents = documents;
        });

    }

    private extractDemographics(patient: Patient): PatientDemographics {
        const mrn: Identifier = FhirStu3Util.getIdentifier(patient, this.configService.getSetting("fhir.mrn.system"));
        const name: HumanName[] = patient.name;
        const dob: string = patient.birthDate;
        return this.demographics = {
            name: name ? FhirStu3Util.formatName(patient.name[0]): null,
            mrn: mrn ? mrn.value : null,
            dob: dob,
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
