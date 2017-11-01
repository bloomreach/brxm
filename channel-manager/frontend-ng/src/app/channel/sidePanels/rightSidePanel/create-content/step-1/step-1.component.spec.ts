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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';

import { TemplateQuery, DocumentTypeInfo } from '../create-content.types';
import { CreateContentComponent } from '../step-1/step-1.component';
import { CreateContentService } from '../create-content.service';
import FeedbackService from '../../../../../services/feedback.service';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { MaterialModule } from '../../../../../shared/material/material.module';
import { NameUrlFieldsComponent } from "../name-url-fields/name-url-fields.component";

class CreateContentServiceMock {
  getTemplateQuery(id): Observable<TemplateQuery> {
    return Observable.of(null);
  }
}

class FeedbackServiceMock {
  showError(key: string, params: Map<string, any>): void {}
}

fdescribe('Create content component', () => {

  let component: CreateContentComponent;
  let fixture: ComponentFixture<CreateContentComponent>;
  let createContentService: CreateContentService;
  let feedbackService: FeedbackService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        CreateContentComponent,
        HintsComponent,
        NameUrlFieldsComponent
      ],
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        MaterialModule,
      ],
      providers: [
        { provide: CreateContentService, useClass: CreateContentServiceMock },
        { provide: FeedbackService, useClass: FeedbackServiceMock },
      ]
    });

    fixture = TestBed.createComponent(CreateContentComponent);
    component = fixture.componentInstance;
    createContentService = fixture.debugElement.injector.get(CreateContentService);
    feedbackService = fixture.debugElement.injector.get(FeedbackService);
  });

  it('throws an error if options is not set', () => {
    expect(() => {
      component.options = null;
      fixture.detectChanges();
    }).toThrowError('Input "options" is required');
  });

  it('throws an error if templateQuery is not set', () => {
    expect(() => {
      component.options = { templateQuery: null };
      fixture.detectChanges();
    }).toThrowError('Configuration option "templateQuery" is required');
  });

  it('loads documentTypes from the templateQuery', () => {
    const documentTypes: Array<DocumentTypeInfo> = [
      { id: 'test-id1', displayName: 'test-name 1' },
      { id: 'test-id2', displayName: 'test-name 2' },
    ];
    const spy = spyOn(createContentService, 'getTemplateQuery')
      .and.returnValue(Observable.of({ documentTypes }));

    component.options = { templateQuery: 'test-template-query' };
    fixture.detectChanges();

    expect(spy).toHaveBeenCalledWith('test-template-query');
    expect(component.documentType).toBeUndefined();
    expect(component.documentTypes).toBe(documentTypes);
  });

  it('pre-selects the documentType if only one is returned from the templateQuery', () => {
    const documentTypes: Array<DocumentTypeInfo> = [{ id: 'test-id1', displayName: 'test-name 1' }];
    const spy = spyOn(createContentService, 'getTemplateQuery')
      .and.returnValue(Observable.of({ documentTypes }));

    component.options = { templateQuery: 'test-template-query' };
    fixture.detectChanges();

    expect(component.documentType).toBe('test-id1');
  });

  it('sends feedback as error when server returns 500', async(() => {
    const feedbackSpy = spyOn(feedbackService, 'showError');
    spyOn(createContentService, 'getTemplateQuery')
      .and.returnValue(Observable.throw({
        status: 500,
        data: {
          'reason': 'INVALID_TEMPLATE_QUERY',
          'params': {
            'templateQuery': 'new-document' }
          }
        }));

    component.options = { templateQuery: 'test-template-query' };
    fixture.detectChanges();

    expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_TEMPLATE_QUERY', { 'templateQuery': 'new-document' });
  }));
});
