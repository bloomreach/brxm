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

import { TestBed, ComponentFixture, async } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';

import FeedbackService from '../../../../../services/feedback.service';
import { CreateContentService } from '../create-content.service';
import ContentService from '../../../../../services/content.service';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { NameUrlFieldsComponent } from "../name-url-fields/name-url-fields.component";
import { SharedModule } from '../../../../../shared/shared.module';
import { ContentServiceMock, CreateContentServiceMock, FeedbackServiceMock } from '../create-content.mocks.spec';
import { CreateContentStep2Component } from './step-2.component';

describe('Create content step 2 component', () => {
  let component: CreateContentStep2Component;
  let fixture: ComponentFixture<CreateContentStep2Component>;
  let createContentService: CreateContentService;
  let ContentService: ContentService;
  let feedbackService: FeedbackService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        CreateContentStep2Component,
        HintsComponent,
        NameUrlFieldsComponent
      ],
      imports: [
        SharedModule,
        FormsModule,
      ],
      providers: [
        { provide: CreateContentService, useClass: CreateContentServiceMock },
        { provide: FeedbackService, useClass: FeedbackServiceMock },
        { provide: ContentService, useClass: ContentServiceMock },
      ]
    });

    fixture = TestBed.createComponent(CreateContentStep2Component);
    component = fixture.componentInstance;
    createContentService = fixture.debugElement.injector.get(CreateContentService);
    ContentService = fixture.debugElement.injector.get(ContentService);
    feedbackService = fixture.debugElement.injector.get(FeedbackService);
  });

  // it('should call parent "on full width" mode on and off', () => {
  //   spyOn(component, 'onFullWidth');
  //   component.setFullWidth(true);
  //   expect(component.isFullWidth).toBe(true);
  //   expect(component.onFullWidth).toHaveBeenCalledWith();
  //
  //   component.setFullWidth(false);
  //   expect(component.isFullWidth).toBe(false);
  //   expect(component.onFullWidth).toHaveBeenCalledWith();
  // });

  // it('should detect ESC keypress', () => {
  //   const e = angular.element.Event('keydown');
  //   e.which = 27;
  //
  //   spyOn(component, 'close');
  //   component.$element.trigger(e);
  //   expect(component.close).toHaveBeenCalled();
  // });

  // it('on init, loads the document from the createContentService', () => {
  //   spyOn(component,'loadNewDocument');
  //   // spyOn(component,'resetBeforeStateChange');
  //
  //   component.ngOnInit();
  //   expect(component.loadNewDocument).toHaveBeenCalled();
  //   // expect(component.resetBeforeStateChange).toHaveBeenCalled();
  // });

  // describe('opening a document', () => {
  //   beforeEach(() => {
  //     ContentService.getDocumentType.and.returnValue(Observable.of(testDocumentType));
  //     CreateContentService.getDocument.and.returnValue(testDocument);
  //   });
  //
  //   it('gets the newly created draft document from create content service', () => {
  //     component.loadNewDocument();
  //     expect(CreateContentService.getDocument).toHaveBeenCalled();
  //     expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
  //   });
  //
  //   it('gets the newly created draft document from create content service', () => {
  //     spyOn(component, '_onLoadSuccess');
  //     component.loadNewDocument();
  //     $rootScope.$apply();
  //     expect(component._onLoadSuccess).toHaveBeenCalledWith(testDocument, testDocumentType);
  //     expect(component.loading).not.toBeDefined();
  //   });
  // });
  //
  // describe('closing the panel', () => {
  //   beforeEach(() => {
  //     ContentService.getDocumentType.and.returnValue(Observable.of(testDocumentType));
  //     CreateContentService.getDocument.and.returnValue(testDocument);
  //     // DialogService.confirm.and.callThrough();
  //
  //     component.$onInit();
  //     $rootScope.$digest();
  //   });
  //
  //   it('Calls discardAndClose method to confirm document discard and close the panel', () => {
  //     spyOn(component, '_discardAndClose').and.returnValue($q.resolve());
  //
  //     component.close();
  //     expect(component._discardAndClose).toHaveBeenCalled();
  //   });
  //
  //   it('Discards the document when "discard" is selected', () => {
  //     spyOn(component, '_confirmDiscardChanges').and.returnValue($q.resolve());
  //
  //     component.close();
  //     $rootScope.$digest();
  //
  //     expect(component.doc).toBeUndefined();
  //     expect(component.documentId).toBeUndefined();
  //     expect(component.docType).toBeUndefined();
  //     expect(component.editing).toBeUndefined();
  //     expect(component.feedback).toBeUndefined();
  //     expect(component.title).toBe(component.defaultTitle);
  //     expect(component.form.$setPristine).toHaveBeenCalled();
  //   });
  //
  //   it('Will not discard the document when cancel is clicked', () => {
  //     spyOn(component, '_confirmDiscardChanges').and.returnValue($q.reject());
  //
  //     component.close();
  //     $rootScope.$digest();
  //
  //     expect(component.doc).not.toBeUndefined();
  //     expect(component.documentId).not.toBeUndefined();
  //     expect(component.docType).not.toBeUndefined();
  //     expect(component.editing).not.toBeUndefined();
  //     expect(component.title).not.toBe(component.defaultTitle);
  //     expect(component.form.$setPristine).not.toHaveBeenCalled();
  //   });
  // });
  //
  // describe('changing name or URL of the document', () => {
  //   beforeEach(() => {
  //     spyOn(component, '_openEditNameUrlDialog');
  //     ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
  //     CreateContentService.getDocument.and.returnValue(testDocument);
  //     DialogService.confirm.and.callThrough();
  //
  //     component.$onInit();
  //     $rootScope.$digest();
  //   });
  //   it('open a change url-name dialog', () => {
  //     component._openEditNameUrlDialog.and.returnValue($q.resolve());
  //     component.editNameUrl();
  //
  //     expect(component._openEditNameUrlDialog).toHaveBeenCalled();
  //   });
  //
  //   it('changes document title if the change is submitted in dialog', () => {
  //     spyOn(component, '_submitEditNameUrl');
  //     component._openEditNameUrlDialog.and.returnValue($q.resolve({ name: 'docName', url: 'doc-url' }));
  //     component.editNameUrl();
  //     $rootScope.$apply();
  //
  //     expect(component._submitEditNameUrl).toHaveBeenCalledWith({ name: 'docName', url: 'doc-url' });
  //   });
  //
  //   it('takes no action if user clicks cancel on the dialog', () => {
  //     spyOn(component, '_submitEditNameUrl');
  //     component._openEditNameUrlDialog.and.returnValue($q.reject());
  //     component.editNameUrl();
  //
  //     expect(component._submitEditNameUrl).not.toHaveBeenCalled();
  //   });
  // });
  //
  // it('_openEditNameUrlDialog method open a dialog with the correct details', () => {
  //   ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
  //   CreateContentService.getDocument.and.returnValue(testDocument);
  //   component.$onInit();
  //   $rootScope.$apply();
  //
  //   component._openEditNameUrlDialog();
  //
  //   const dialogArguments = DialogService.show.calls.mostRecent().args[0];
  //
  //   expect(dialogArguments.locals.title).toBe('CHANGE_DOCUMENT_NAME');
  //   expect(dialogArguments.locals.name).toBe('testDoc');
  // });
  //
  // it('show correct dialog to change name or URL of the document', () => {});
  //
  // it('knows the document is dirty when the backend says so', () => {
  //   component.doc = {
  //     info: {
  //       dirty: true,
  //     },
  //   };
  //   expect(component.isDocumentDirty()).toBe(true);
  // });
  //
  // it('knows the document is dirty when the form is dirty', () => {
  //   component.form.$dirty = true;
  //   expect(component.isDocumentDirty()).toBe(true);
  // });
  //
  // it('knows the document is dirty when both the backend says so and the form is dirty', () => {
  //   component.doc = {
  //     info: {
  //       dirty: true,
  //     },
  //   };
  //   component.form.$dirty = true;
  //   expect(component.isDocumentDirty()).toBe(true);
  // });
});
