
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpClientModule} from '@angular/common/http';
import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {routing} from './app.routing';
import {environment} from "../environments/environment";
import {HomeComponent} from "./home/home.component";
import {LoginComponent} from "./login/login.component";
import {AngularSplitModule} from 'angular-split';
import {RestService} from "./rest/rest.service";
import {MockRestService} from "./rest/rest.service.mock";
import {MaterialModule} from '@uukmm/ng-widget-toolkit';
import {LoggerModule} from "@uukmm/ng-logger";
import {APP_BASE_HREF} from "@angular/common";
import {NgbTooltipModule} from '@ng-bootstrap/ng-bootstrap';
import {MainComponent} from './main/main.component';
import {SingleImportComponent} from './single-import/single-import.component';
import {AboutComponent} from './about/about.component';
import {QueueManagerComponent} from './queue-manager/queue-manager.component';
import {BatchImportComponent} from './batch-import/batch-import.component';
import {IndexManagerComponent} from './index-manager/index-manager.component';

// Import plugin modules here:

@NgModule({
    declarations: [
        AppComponent,
        HomeComponent,
        LoginComponent,
        MainComponent,
        AboutComponent,
        SingleImportComponent,
        BatchImportComponent,
        QueueManagerComponent,
        IndexManagerComponent
    ],
    entryComponents: [
        HomeComponent,
        LoginComponent,
        MainComponent,
        AboutComponent,
        SingleImportComponent,
        BatchImportComponent,
        QueueManagerComponent,
        IndexManagerComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [
        AngularSplitModule.forRoot(),
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpClientModule,
        LoggerModule,
        ReactiveFormsModule,
        MaterialModule,
        NgbTooltipModule,
        routing
    ],
    providers: [
        {
            provide: RestService,
            useClass: environment.serverEndpoint ? RestService : MockRestService
        },
        {
            provide: 'environment',
            useValue: environment
        },
        {
            provide: APP_BASE_HREF,
            useValue: '/' + (window.location.pathname.split('/')[1] || '')
        }
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
