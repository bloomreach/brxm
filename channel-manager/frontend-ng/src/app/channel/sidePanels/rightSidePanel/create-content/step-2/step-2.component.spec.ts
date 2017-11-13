/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { TestBed, ComponentFixture, async, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import 'rxjs/add/operator/toPromise';

import { CreateContentService } from '../create-content.service';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { NameUrlFieldsComponent } from '../name-url-fields/name-url-fields.component';
import { SharedModule } from '../../../../../shared/shared.module';
import {
  ContentServiceMock, CreateContentServiceMock, DialogServiceMock, FieldServiceMock,
  MdDialogMock, MdDialogRefMock
} from '../create-content.mocks.spec';
import { CreateContentStep2Component } from './step-2.component';
import { SharedspaceToolbarDirective } from '../../fields/ckeditor/sharedspace-toolbar/sharedspace-toolbar.component';
import { FieldsEditorDirective } from '../../fieldsEditor/fields-editor.component';
import { TranslateModule, TranslateLoader, TranslateFakeLoader } from '@ngx-translate/core';

import ContentService from '../../../../../services/content.service';
import DialogService from '../../../../../services/dialog.service';
import FieldService from '../../fields/field.service';
import { DocumentTypeInfo, Document } from '../create-content.types';
import { MdDialog, MdDialogRef } from "@angular/material";
import { NameUrlFieldsDialogComponent } from './name-url-fields-dialog/name-url-fields-dialog';
import { BrowserDynamicTestingModule } from "@angular/platform-browser-dynamic/testing";

fdescribe('Create content step 2 component', () => {
  let component: CreateContentStep2Component;
  let fixture: ComponentFixture<CreateContentStep2Component>;
  let createContentService: CreateContentService;
  let contentService: ContentService;
  let dialogService: DialogService;
  let fieldService: FieldService;
  let mdDialog: MdDialog;
  let dialog: MdDialogRef<any>;

  const testDocument: Document = {
    id: 'testId',
    displayName: 'test document',
    info: {
      dirty: false,
      type: {
        id: 'ns:testdocument',
      }
    }
  };
  const testDocumentType: DocumentTypeInfo = { id: 'ns:testdocument', displayName: 'test-name 1' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        CreateContentStep2Component,
        HintsComponent,
        NameUrlFieldsComponent,
        SharedspaceToolbarDirective,
        FieldsEditorDirective,
        NameUrlFieldsDialogComponent
      ],
      imports: [
        SharedModule,
        FormsModule,
        TranslateModule.forRoot({
          loader: { provide: TranslateLoader, useClass: TranslateFakeLoader }
        })
      ],
      providers: [
        { provide: CreateContentService, useClass: CreateContentServiceMock },
        { provide: ContentService, useClass: ContentServiceMock },
        { provide: DialogService, useClass: DialogServiceMock },
        { provide: FieldService, useClass: FieldServiceMock },
        { provide: MdDialog, useClass: MdDialogMock },
        { provide: MdDialogRef, useClass: MdDialogRefMock }
      ]
    });

    TestBed.overrideModule(BrowserDynamicTestingModule, {
      set: {
        entryComponents: [NameUrlFieldsDialogComponent],
      },
    });

    fixture = TestBed.createComponent(CreateContentStep2Component);
    component = fixture.componentInstance;
    createContentService = fixture.debugElement.injector.get(CreateContentService);
    contentService = fixture.debugElement.injector.get(ContentService);
    dialogService = fixture.debugElement.injector.get(DialogService);
    fieldService = fixture.debugElement.injector.get(FieldService);
    mdDialog = fixture.debugElement.injector.get(MdDialog);
    dialog = fixture.debugElement.injector.get(MdDialogRef);

    spyOn(contentService, 'getDocumentType').and.callThrough();
    spyOn(createContentService, 'getDocument').and.callThrough();
    spyOn(dialogService, 'confirm').and.callThrough();

    fixture.detectChanges();
  });

  it('should detect ESC keypress', fakeAsync(() => {
    fixture.detectChanges();
    spyOn(component, 'close');
    const event = new KeyboardEvent('keypress');
    Object.defineProperty(event, 'which', { value: 27 });
    fixture.nativeElement.dispatchEvent(event);

    expect(component.close).toHaveBeenCalled();
  }));

  describe('setFullWidth', () => {
    it('call parent "on full width" mode on and off', () => {
      spyOn(component.onFullWidth, 'emit');
      component.setFullWidth(true);
      expect(component.isFullWidth).toBe(true);
      expect(component.onFullWidth.emit).toHaveBeenCalledWith(true);

      component.setFullWidth(false);
      expect(component.isFullWidth).toBe(false);
      expect(component.onFullWidth.emit).toHaveBeenCalledWith(false);
    });
  });

  describe('ngOnInit', () => {
    it('loads the document from the createContentService', () => {
      // Override emit function to trigger the emitted method, so we can test it is called
      component.onBeforeStateChange.emit = arg => arg();
      spyOn(component,'loadNewDocument');
      spyOn(component, 'discardAndClose');
      spyOn(component.onBeforeStateChange, 'emit').and.callThrough();

      component.ngOnInit();
      expect(component.loadNewDocument).toHaveBeenCalled();
      expect(component.onBeforeStateChange.emit).toHaveBeenCalled();
      expect(component.discardAndClose).toHaveBeenCalled();
    });
  });

  describe('loadNewDocument', () => {
    it('gets the newly created draft document from create content service', () => {
      component.loadNewDocument();

      expect(createContentService.getDocument).toHaveBeenCalled();
      expect(contentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
    });

    it('gets the newly created draft document from create content service', () => {
      component.loadNewDocument().then(() => {
        expect(component.doc).toEqual(testDocument);
        expect(component.docType).toEqual(testDocumentType);
        expect(component.loading).toEqual(false);
      });
    });
  });

  describe('close', () => {
    beforeEach(() => {
      spyOn(component.onClose, 'emit');

      component.loadNewDocument();
      component.doc = testDocument;
    });

    it('calls the confirmation dialog', () => {
      fixture.detectChanges();

      spyOn(component, 'discardAndClose').and.callThrough();

      component.close();
      expect(dialogService.confirm).toHaveBeenCalled();
    });

    it('calls discardAndClose method to confirm document discard and close the panel', () => {
      fixture.detectChanges();

      spyOn(component, 'discardAndClose').and.returnValue(Promise.resolve());

      component.close();
      expect(component.discardAndClose).toHaveBeenCalled();
    });

    it('discards the document when "discard" is selected', () => {
      component.close().then(() => {
        expect(component.documentId).not.toBeDefined();
        expect(component.doc).not.toBeDefined();
        expect(component.docType).not.toBeDefined();
        expect(component.feedback).not.toBeDefined();
        expect(component.title).toEqual('Create new content');
        expect(component.onClose.emit).toHaveBeenCalled();
      });
    });

    it('will not discard the document when cancel is clicked', () => {
      fixture.detectChanges();

      spyOn(component, 'discardAndClose').and.returnValue(Promise.reject(null));

      component.close().catch(() => {
        expect(component.onClose.emit).not.toHaveBeenCalled();
      });
    });
  });

  describe('editNameUrl', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.doc = testDocument;
    });

    it('changes document title if the change is submitted in dialog', () => {
      component.editNameUrl();
      fixture.detectChanges();

      expect(component.doc.displayName).toBe('docName');
    });
  });

  describe('isDocumentDirty', () => {
    it('returns true if document is set to dirty by the backend', () => {
      component.doc = testDocument;
      component.doc.info.dirty = true;
      expect(component.isDocumentDirty()).toBe(true);
    });
  });
});
