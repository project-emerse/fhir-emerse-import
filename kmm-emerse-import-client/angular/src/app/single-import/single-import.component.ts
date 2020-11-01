import {Component, ViewEncapsulation} from "@angular/core";
import {FhirStu3Util, HumanName, Identifier, Patient} from "@uukmm/ng-fhir-model/stu3";
import {RestService} from "../rest/rest.service";
import {filter, switchMap, tap} from "rxjs/operators";
import {Document} from "../model/document.model";
import {PatientDemographics} from "../model/patient-demographics.model";
import {ConfigService} from "../config/config.service";
import {IndexResultUtil} from "../model/index-result.model";
import {IdentifierType} from '../model/queue-entry.model';

@Component({
    selector: 'emerse-single-import',
    templateUrl: './single-import.component.html',
    styleUrls: ['./single-import.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class SingleImportComponent {

    documents: Document[];

    demographics: PatientDemographics;

    mrn: string;

    patient: Patient;

    htmlBody: string;

    textBody: string;

    message: string;

    busy: boolean;

    private target: any;

    private error = error => this.setMessage("An error occurred: " + error, false);

    constructor(
        private readonly restService: RestService,
        private readonly configService: ConfigService) {
    }

    search(): void {
        this.clear();
        this.setMessage("Searching for patient...", true);
        this.restService.findPatient(this.mrn, IdentifierType.MRN)
            .pipe(
                tap(patient => {
                    this.message = patient == null ? "No patient found.  Please try again." : null;
                    this.busy = patient != null;
                }, this.error),
                filter(patient => patient != null),
                tap(patient => {
                    this.extractDemographics(patient);
                    this.message = "Searching for documents..."
                }, this.error),
                switchMap(patient => this.restService.getDocuments(patient.id, IdentifierType.PATID)),
                tap(() => {
                    this.message = null;
                    this.busy = false;
                }, this.error)).subscribe(docs => this.documents = docs);
    }

    private extractDemographics(patient: Patient): PatientDemographics {
        const mrn: Identifier = FhirStu3Util.getIdentifier(patient, this.configService.getSetting("fhir.mrn.system"));
        const name: HumanName = patient.name?.[0];
        const dob: string = patient.birthDate;
        return this.demographics = {
            name: name ? FhirStu3Util.formatName(name): null,
            mrn: mrn?.value,
            dob: dob,
            gender: patient.gender
        }
    }

    index(): void {
        this.setMessage("Indexing documents...", true);
        this.restService.singleIndex(this.mrn, IdentifierType.MRN).subscribe(
            result => this.setMessage(IndexResultUtil.toString(result), false),
            this.error);
    }

    private setMessage(message: string, busy = this.busy): void {
        this.message = message;
        this.busy = busy;
    }

    clear(): void {
        this.textBody = null;
        this.htmlBody = null;
        this.documents = null;
        this.demographics = null;
        this.setMessage(null, false);
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
