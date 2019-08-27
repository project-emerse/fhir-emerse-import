import 'hammerjs';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpClientModule} from '@angular/common/http';
import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {routing} from './app.routing';
import {LoggerModule} from "ngx-logger";
import {environment} from "../environments/environment";
import {HomeComponent} from "./home/home.component";
import {LoginComponent} from "./login/login.component";
import {ImportComponent} from "./import/import.component";
import {ImportSingleComponent} from "./import/single/import-single.component";
import {ImportBatchComponent} from "./import/batch/import-batch.component";
import {AngularSplitModule} from 'angular-split';
import {RestService} from "./rest/rest.service";
import {MockRestService} from "./rest/rest.service.mock";
import {ImportManagerComponent} from "./import/manager/import-manager.component";
import {MaterialModule} from '@uukmm/ng-widget-toolkit';

// Import plugin modules here:

@NgModule({
    declarations: [
        AppComponent,
        HomeComponent,
        LoginComponent,
        ImportComponent,
        ImportSingleComponent,
        ImportBatchComponent,
        ImportManagerComponent
    ],
    entryComponents: [
        HomeComponent,
        LoginComponent,
        ImportComponent,
        ImportSingleComponent,
        ImportBatchComponent,
        ImportManagerComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [
        LoggerModule.forRoot(environment.loggerConfig),
        AngularSplitModule.forRoot(),
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpClientModule,
        ReactiveFormsModule,
        MaterialModule,
        routing
    ],
    providers: [
        {
            provide: RestService,
            useClass: environment.serverEndpoint ? RestService : MockRestService
        }
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
