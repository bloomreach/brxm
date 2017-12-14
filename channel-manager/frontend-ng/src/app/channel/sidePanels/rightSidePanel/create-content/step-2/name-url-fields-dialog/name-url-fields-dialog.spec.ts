import { ComponentFixture } from "@angular/core/testing";
import { FormsModule } from '@angular/forms';
import { TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

import { NameUrlFieldsDialogComponent } from "./name-url-fields-dialog";
import { NameUrlFieldsComponent } from './../../name-url-fields/name-url-fields.component';
import { SharedModule } from '../../../../../../shared/shared.module';
import { HintsComponent } from '../../../../../../shared/components/hints/hints.component';
import CreateContentService from '../../createContent.service.js';
import { CreateContentServiceMock } from "../../create-content.mocks.spec";

xdescribe('NameUrlFields Component', () => {
    let fixture: ComponentFixture<NameUrlFieldsDialogComponent>;
    let component: NameUrlFieldsDialogComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                HintsComponent,
                NameUrlFieldsComponent,
                NameUrlFieldsDialogComponent
            ],
            imports: [
                BrowserAnimationsModule,
                FormsModule,
                SharedModule
            ],
            providers: [
                { provide: CreateContentService, useClass: CreateContentServiceMock },
                { provide: MatDialogRef, useValue: {} },
                { provide: MAT_DIALOG_DATA, useValue: {} }
            ]
        });

        fixture = TestBed.createComponent(NameUrlFieldsDialogComponent);
        component = fixture.componentInstance;


        component.ngOnInit();
        fixture.detectChanges();
    });
});
