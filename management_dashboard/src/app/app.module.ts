import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { FooterComponent } from './components/footer/footer.component';
import { ContentComponent } from './components/content/content.component';
import { AsideComponent } from './components/aside/aside.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { ChartsModule } from 'ng2-charts';
import {HttpClientModule} from '@angular/common/http';
import {FormsModule} from "@angular/forms";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {CdkTableModule} from "@angular/cdk/table";
import {MatInputModule} from "@angular/material/input";
import {MatPaginatorModule} from "@angular/material/paginator";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";


@NgModule({
  declarations: [
    AppComponent,
    FooterComponent,
    ContentComponent,
    AsideComponent,
    NavbarComponent
  ],
  imports: [
    BrowserModule,
    ChartsModule,
    HttpClientModule,
    FormsModule,
    BrowserAnimationsModule,

    CdkTableModule,
    MatInputModule,
    MatPaginatorModule,
    MatSortModule,
    MatTableModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
