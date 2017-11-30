import { ComponentFixture } from "@angular/core/testing";
import { FormsModule } from '@angular/forms';
import { TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

import { NameUrlFieldsDialogComponent } from "./name-url-fields-dialog";
import { NameUrlFieldsComponent } from './../../name-url-fields/name-url-fields.component';
import { SharedModule } from '../../../../../../shared/shared.module';
import { HintsComponent } from '../../../../../../shared/components/hints/hints.component';
import { CreateContentService } from '../../create-content.service';
import { CreateContentServiceMock } from "../../create-content.mocks.spec";

describe('NameUrlFields Component', () => {
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

    describe('urlNameFieldsValid', () => {
        describe('conditions scenarios', () => {
            it('returns true, all conditions resolved to "true"', () => {
                component.nameUrlFields.nameField = 'name';
                component.nameUrlFields.urlField = 'url';
                expect(component.urlNameFieldsValid()).toEqual(true);
            });

            it('returns false, name field is empty (conditions index 0)', () => {
                // Condition index 0 (empty name field)
                component.nameUrlFields.nameField = '';
                component.nameUrlFields.urlField = 'url';
                expect(component.urlNameFieldsValid()).toEqual(false);
            });

            it('returns false, url field is empty (conditions index 1)', () => {
                // Condition index 0 (empty name field)
                component.nameUrlFields.nameField = '';
                component.nameUrlFields.urlField = '';
                expect(component.urlNameFieldsValid()).toEqual(false);
            });

            it('returns false, name field is only whitespace(s) (conditions index 2)', () => {
                // Condition index 0 (empty name field)
                component.nameUrlFields.nameField = '     ';
                component.nameUrlFields.urlField = 'url';
                expect(component.urlNameFieldsValid()).toEqual(false);
            });

            it('returns false, url field is only whitespace(s) (conditions index 3)', () => {
                // Condition index 0 (empty name field)
                component.nameUrlFields.nameField = 'name';
                component.nameUrlFields.urlField = '     ';
                expect(component.urlNameFieldsValid()).toEqual(false);
            });
        });
    });
});