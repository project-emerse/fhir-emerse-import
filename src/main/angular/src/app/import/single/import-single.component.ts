import {Component, ViewEncapsulation} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";

interface Document {

    title: string;

    body: string;

    date: string;

    isHtml: boolean;
}

interface Patient {
    name: string;

    mrn: string;

    dob: string;

    gender: string;
}

@Component({
    selector: 'emerse-import-single',
    templateUrl: './import-single.component.html',
    styleUrls: ['./import-single.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class ImportSingleComponent {

    mrn: string;

    documents: Document[];

    patient: Patient;

    htmlBody: string;

    textBody: string;

    private fakeBody: string;

    private target: any;

    constructor(httpClient: HttpClient) {
        const result: Observable<string> = httpClient.get("assets/test/cda.html", {responseType: "text"});
        result.subscribe(html => this.fakeBody = html);
    }

    search(): void {
        this.documents = [];

        for (let i = 1; i < 20; i++) {
            const title = `Clinical Summary #${i}`;
            const date: string = new Date(new Date().getTime() - i * 10000000000).toLocaleDateString();
            const body = this.fakeBody
                .replace("@@title@@", title)
                .replace("@@mrn@@", this.mrn)
                .replace("@@date@@", date);
            this.documents.push({
                title,
                date,
                body,
                isHtml: true
            });
        }

        this.patient = {
            name: "EDWRUC Najnqm",
            mrn: this.mrn,
            dob: "11/14/1950",
            gender: "Male"
        }
    }

    index(): void {

    }

    clear(): void {
        this.documents = null;
        this.patient = null;
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
