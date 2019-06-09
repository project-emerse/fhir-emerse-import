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
import {MaterialModule} from "./material/material.module";
import {ImportComponent} from "./import/import.component";
import {ImportSingleComponent} from "./import/single/import-single.component";
import {ImportBatchComponent} from "./import/batch/import-batch.component";
import {AngularSplitModule} from 'angular-split';

// Import plugin modules here:

@NgModule({
  declarations: [
      AppComponent,
      HomeComponent,
      LoginComponent,
      ImportComponent,
      ImportSingleComponent,
      ImportBatchComponent
  ],
  entryComponents: [
      HomeComponent,
      LoginComponent,
      ImportComponent,
      ImportSingleComponent,
      ImportBatchComponent
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
  bootstrap: [AppComponent]
})
export class AppModule {
}
