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
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import 'rxjs/add/operator/toPromise';

import { CreateContentService } from '../create-content.service';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { NameUrlFieldsComponent } from '../name-url-fields/name-url-fields.component';
import { SharedModule } from '../../../../../shared/shared.module';
import {
  ContentServiceMock, CreateContentServiceMock, DialogServiceMock, FeedbackServiceMock, FieldServiceMock
} from '../create-content.mocks.spec';
import { CreateContentStep2Component } from './step-2.component';
import { SharedspaceToolbarDirective } from '../../fields/ckeditor/sharedspace-toolbar/sharedspace-toolbar.component';
import { FieldsEditorDirective } from '../../fieldsEditor/fields-editor.component';
import FeedbackService from '../../../../../services/feedback.service.js';

import ContentService from '../../../../../services/content.service';
import DialogService from '../../../../../services/dialog.service';
import FieldService from '../../fields/field.service';
import { DocumentTypeInfo, Document } from '../create-content.types';
import { MatDialog, MatDialogRef } from '@angular/material';
import { NameUrlFieldsDialogComponent } from './name-url-fields-dialog/name-url-fields-dialog';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';

describe('Create content step 2 component', () => {
  let component: CreateContentStep2Component;
  let fixture: ComponentFixture<CreateContentStep2Component>;
  let createContentService: CreateContentService;
  let contentService: ContentService;
  let dialogService: DialogService;
  let fieldService: FieldService;
  let feedbackService: FeedbackService;
  let matDialog: MatDialog;
  let dialog: MatDialogRef<any>;

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
        FormsModule
      ],
      providers: [
        { provide: CreateContentService, useClass: CreateContentServiceMock },
        { provide: ContentService, useClass: ContentServiceMock },
        { provide: DialogService, useClass: DialogServiceMock },
        { provide: FieldService, useClass: FieldServiceMock },
        { provide: FeedbackService, useClass: FeedbackServiceMock },
        { provide: MatDialog },
        { provide: MatDialogRef }
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
    feedbackService = fixture.debugElement.injector.get(FeedbackService);
    matDialog = fixture.debugElement.injector.get(MatDialog);
    dialog = fixture.debugElement.injector.get(MatDialogRef);

    spyOn(contentService, 'getDocumentType').and.callThrough();
    spyOn(createContentService, 'getDocument').and.callThrough();
    spyOn(dialogService, 'confirm').and.callThrough();

    component.options = {
      name: testDocument.displayName,
      url: 'test-document',
      locale: 'en'
    };

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
    it('loads the document from createContentService', () => {
      // Override emit function to trigger the emitted method, so we can test it is called
      component.onBeforeStateChange.emit = arg => arg();
      spyOn(component, 'loadNewDocument').and.callThrough();
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
      // fixture.detectChanges();
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
      component.onBeforeStateChange.emit = arg => arg();
      spyOn(component.onBeforeStateChange, 'emit').and.callThrough();
      spyOn(Promise, 'resolve').and.callThrough();
      component.close().then(() => {
        expect(component.documentId).not.toBeDefined();
        expect(component.doc).not.toBeDefined();
        expect(component.docType).not.toBeDefined();
        expect(component.feedback).not.toBeDefined();
        expect(component.title).toEqual('Create new content');
        expect(component.onBeforeStateChange.emit).toHaveBeenCalled();
        expect(Promise.resolve).toHaveBeenCalled();
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

 describe('onEditNameUrlClose', () => {
   let _component;

   beforeEach(() => {
     // The "as any" cast is needed because otherwise our only way to test this
     // functionality is to mock the whole Angular Material dialog workflow and prototype methods.
     _component = (component as any);
     component.doc = testDocument;
   });

   it('receives new document name and URL when dialog is submitted', () => {
     spyOn(createContentService, 'setDraftNameUrl').and.callThrough();

     expect(component.doc.displayName).toEqual('test document');
     _component.onEditNameUrlDialogClose({ name: 'New name', url: 'new-url' }).then(() => {
       expect(createContentService.setDraftNameUrl).toHaveBeenCalledWith(component.doc.id, { name: 'New name', url: 'new-url' });
       expect(component.doc.displayName).toEqual('New name');
       expect(_component.documentUrl).toEqual('new-url');
     });
   });

   it('calls feedbackService.showError when an error is returned from the back-end', fakeAsync(() => {
     spyOn(createContentService, 'setDraftNameUrl').and.returnValue(Promise.reject({
       data: { reason: 'TEST', params: {} },
     }));
     spyOn(feedbackService, 'showError');

     expect(component.doc.displayName).toEqual('test document');
     _component.onEditNameUrlDialogClose({ name: 'New name', url: 'new-url' });
     tick();
     expect(feedbackService.showError).toHaveBeenCalledWith('ERROR_TEST', {});
     expect(component.doc.displayName).toEqual('test document');
   }));
 });

  describe('isDocumentDirty', () => {
    it('returns true if document is set to dirty by the backend', () => {
      component.doc = testDocument;
      component.doc.info.dirty = true;
      expect(component.isDocumentDirty()).toBe(true);
    });
  });

  describe('discardAndClose', () => {
    let deleteDraftSpy;
    beforeEach(() => {
      component.doc = testDocument;
      spyOn(feedbackService, 'showError');
      deleteDraftSpy = spyOn(createContentService, 'deleteDraft').and.returnValue(Promise.resolve());
    });

    it('deletes the draft after confirming the discard dialog', fakeAsync(() => {
      const _component = (component as any);
      spyOn(_component, 'confirmDiscardChanges').and.returnValue(Promise.resolve());

      component.discardAndClose();
      tick();

      expect(_component.confirmDiscardChanges).toHaveBeenCalled();
      expect(deleteDraftSpy).toHaveBeenCalledWith(testDocument.id);
      expect(feedbackService.showError).not.toHaveBeenCalled();
    }));

    it('calls feedbackService.showError if deleting the draft has failed', fakeAsync(() => {
      deleteDraftSpy.and.returnValue(Promise.reject({
        data: { reason: 'TEST', params: {} },
      }));

      component.discardAndClose();
      tick();

      expect(deleteDraftSpy).toHaveBeenCalledWith(testDocument.id);
      expect(feedbackService.showError).toHaveBeenCalledWith('ERROR_TEST', {});
    }))
  });

  describe('saveDocument', () => {
    beforeEach(() => {
      component.doc = testDocument;
    });

    it('creates a draft of the current document', () => {
      spyOn(contentService, 'saveDraft').and.callThrough();
      component.saveDocument();
      expect(contentService.saveDraft).toHaveBeenCalledWith(testDocument);
    });

    it('emits the id of the saved document', fakeAsync(() => {
      spyOn(component.onSave, 'emit');
      component.saveDocument();
      tick();
      expect(component.onSave.emit).toHaveBeenCalledWith(testDocument.id);
    }));

    it('does not trigger a discardAndClose dialog by resetting onBeforeStateChange', fakeAsync(() => {
      spyOn(component.onBeforeStateChange, 'emit');
      component.saveDocument();
      tick();
      expect(component.onBeforeStateChange.emit).toHaveBeenCalled();
    }));
  });
});
