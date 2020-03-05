import {Component, ViewEncapsulation} from "@angular/core";
import {combineLatest, Observable} from "rxjs";
import {FhirStu3Util, HumanName, Identifier, Patient} from "@uukmm/ng-fhir-model/stu3";
import {RestService} from "../../rest/rest.service";
import {filter, map, switchMap, tap} from "rxjs/operators";
import {Document} from "../../model/document.model";
import {PatientDemographics} from "../../model/patient-demographics.model";
import {ConfigService} from "../../config/config.service";
import {IndexResult, IndexResultUtil} from "../../model/index-result.model";

@Component({
    selector: 'emerse-import-single',
    templateUrl: './import-single.component.html',
    styleUrls: ['./import-single.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportSingleComponent {

    documents: Document[];

    demographics: PatientDemographics;

    mrn: string;

    patient: Patient;

    htmlBody: string;

    textBody: string;

    message: string;

    busy: boolean;

    private target: any;

    constructor(
        private readonly restService: RestService,
        private readonly configService: ConfigService) {
    }

    search(): void {
        this.clear();
        this.message = "Searching for patient...";
        this.busy = true;
        this.restService.findPatient(this.mrn)
            .pipe(
                tap(patient => {
                    this.message = patient == null ? "No patient found.  Please try again." : null;
                    this.busy = patient != null;
                }),
                filter(patient => patient != null),
                tap(patient => {
                    this.extractDemographics(patient);
                    this.message = "Searching for documents..."
                }),
                switchMap(patient => this.restService.getDocuments(patient.id)),
                tap(() => {
                    this.message = null;
                    this.busy = false;
                })).subscribe(docs => this.documents = docs);
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

    index(): void {
        this.message = null;
        this.busy = true;
        this.message = "Indexing documents..."
        this.restService.singleIndex(this.mrn).pipe(
            tap(() => {
                this.message = null;
                this.busy = false;
            })
        ).subscribe(result => this.message = IndexResultUtil.toString(result));
    }

    clear(): void {
        this.textBody = null;
        this.htmlBody = null;
        this.documents = null;
        this.demographics = null;
        this.message = null;
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