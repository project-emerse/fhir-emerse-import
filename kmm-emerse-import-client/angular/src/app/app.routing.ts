import {ModuleWithProviders} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HomeComponent} from "./home/home.component";
import {AppModule} from './app.module';

const appRoutes: Routes = [
  {
    path: '**',
    component: HomeComponent
  }
];

export const routing: ModuleWithProviders<AppModule> = RouterModule.forRoot(appRoutes);
