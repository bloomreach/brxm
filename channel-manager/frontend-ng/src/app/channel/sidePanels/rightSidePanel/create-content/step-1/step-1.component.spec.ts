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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';

import ChannelService from '../../../../channel.service';
import FeedbackService from '../../../../../services/feedback.service';
import CreateContentService from '../createContent.service.js';
import { ChannelServiceMock, CreateContentServiceMock, FeedbackServiceMock } from '../create-content.mocks.spec';
import { CreateContentComponent } from './step-1.component';
import { DocumentLocationFieldComponent } from '../document-location/document-location-field.component';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { NameUrlFieldsComponent } from '../name-url-fields/name-url-fields.component';
import { SharedModule } from '../../../../../shared/shared.module';

describe('Create content step 1 component', () => {
    let component: CreateContentComponent;
    let fixture: ComponentFixture<CreateContentComponent>;
    let createContentService: CreateContentService;
    let feedbackService: FeedbackService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                CreateContentComponent,
                DocumentLocationFieldComponent,
                HintsComponent,
                NameUrlFieldsComponent
            ],
            imports: [
                SharedModule,
                FormsModule
            ],
            providers: [
                { provide: ChannelService, useClass: ChannelServiceMock },
                { provide: CreateContentService, useClass: CreateContentServiceMock },
                { provide: FeedbackService, useClass: FeedbackServiceMock }
            ]
        });

        fixture = TestBed.createComponent(CreateContentComponent);
        component = fixture.componentInstance;
        createContentService = fixture.debugElement.injector.get(CreateContentService);
        feedbackService = fixture.debugElement.injector.get(FeedbackService);
    });

    // Step1 tests inactive till Step1 is downgraded to AngularJS. do not remove these tests
    //
    // describe('DocumentType', () => {
    //   it('throws an error if options is not set', () => {
    //     expect(() => {
    //       component.options = null;
    //       fixture.detectChanges();
    //     }).toThrowError('Input "options" is required');
    //   });
    //
    //   it('throws an error if templateQuery is not set', () => {
    //     expect(() => {
    //       component.options = {templateQuery: null};
    //       fixture.detectChanges();
    //     }).toThrowError('Configuration option "templateQuery" is required');
    //   });
    //
    //   it('loads documentTypes from the templateQuery', fakeAsync(() => {
    //     const documentTypes: Array<DocumentTypeInfo> = [
    //       {id: 'test-id1', displayName: 'test-name 1'},
    //       {id: 'test-id2', displayName: 'test-name 2'},
    //     ];
    //     const spy = spyOn(createContentService, 'getTemplateQuery')
    //       .and.returnValue(Promise.resolve({documentTypes}));
    //
    //     component.options = {templateQuery: 'test-template-query'};
    //     fixture.detectChanges();
    //     tick();
    //     expect(spy).toHaveBeenCalledWith('test-template-query');
    //     expect(component.documentType).toBeUndefined();
    //     expect(component.documentTypes).toBe(documentTypes);
    //   }));
    //
    //   it('pre-selects the documentType if only one is returned from the templateQuery', fakeAsync(() => {
    //     const documentTypes: Array<DocumentTypeInfo> = [{id: 'test-id1', displayName: 'test-name 1'}];
    //     spyOn(createContentService, 'getTemplateQuery').and.returnValue(Promise.resolve({documentTypes}));
    //
    //     component.options = {templateQuery: 'test-template-query'};
    //     fixture.detectChanges();
    //     tick();
    //     expect(component.documentType).toBe('test-id1');
    //   }));
    //
    //   it('sends feedback as error when server returns 500', fakeAsync(() => {
    //     const feedbackSpy = spyOn(feedbackService, 'showError');
    //     spyOn(createContentService, 'getTemplateQuery')
    //       .and.returnValue(Promise.reject({
    //       status: 500,
    //       data: {
    //         'reason': 'INVALID_TEMPLATE_QUERY',
    //         'params': {
    //           'templateQuery': 'new-document'
    //         }
    //       }
    //     }));
    //
    //     component.options = {templateQuery: 'test-template-query'};
    //     fixture.detectChanges();
    //     tick();
    //     expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_TEMPLATE_QUERY', {'templateQuery': 'new-document'});
    //   }));
    // });
    //
    // describe('Creating new draft', () => {
    //   beforeEach(() => {
    //     // Mock templateQuery calls that gets executed on "onInit"
    //     // Disabling this will fail the tests
    //     component.options = {
    //       templateQuery: 'test-template-query',
    //       rootPath: '/content/documents/hap/news',
    //       defaultPath: '2017/12'
    //     };
    //     const documentTypes: Array<DocumentTypeInfo> = [
    //       {id: 'test-id1', displayName: 'test-name 1'},
    //     ];
    //     spyOn(createContentService, 'getTemplateQuery').and.returnValue(Promise.resolve({documentTypes}));
    //   });
    //
    //   it('assembles document object and sends it to the server', () => {
    //     component.nameUrlFields.nameField = 'New doc';
    //     component.nameUrlFields.urlField = 'new-doc';
    //     component.documentType = 'hap:contentdocument';
    //     component.documentLocationField.rootPath = '/content/documents/hap/news';
    //     component.documentLocationField.defaultPath = '2017/12';
    //
    //     const data = {
    //       name: 'New doc',
    //       slug: 'new-doc',
    //       templateQuery: 'test-template-query',
    //       documentTypeId: 'hap:contentdocument',
    //       rootPath: '/content/documents/hap/news',
    //       defaultPath: '2017/12',
    //     };
    //     const spy = spyOn(createContentService, 'createDraft')
    //       .and.returnValue(Promise.resolve('resolved'));
    //
    //     component.submit();
    //     fixture.detectChanges();
    //
    //     expect(spy).toHaveBeenCalledWith(data);
    //   });
    //
    //   it('sends feedback as error when server returns 500', fakeAsync(() => {
    //     const feedbackSpy = spyOn(feedbackService, 'showError');
    //     spyOn(createContentService, 'createDraft')
    //       .and.returnValue(Promise.reject({
    //       status: 500,
    //       data: {
    //         'reason': 'INVALID_DOCUMENT_DETAILS',
    //       }
    //     }));
    //
    //     component.submit();
    //     fixture.detectChanges();
    //     tick();
    //     expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_DOCUMENT_DETAILS');
    //   }));
    // });
});
