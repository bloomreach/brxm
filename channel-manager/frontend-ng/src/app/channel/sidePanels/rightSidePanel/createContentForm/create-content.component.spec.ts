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

import { CreateContentComponent } from './create-content.component';
import { CreateContentService, DocumentTypeInfo } from '../../../../services/create-content.service';
import { HintsComponent } from '../../../../shared/components/hints/hints.component';
import { MaterialModule } from '../../../../material/material.module';

class CreateContentServiceMock {
  getDocumentTypesFromTemplateQuery(id): Observable<DocumentTypeInfo[]> {
    return Observable.of([]);
  }
}

describe('Create content component', () => {

  let component: CreateContentComponent;
  let fixture: ComponentFixture<CreateContentComponent>;
  let createContentService: CreateContentService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        CreateContentComponent,
        HintsComponent,
      ],
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        MaterialModule,
      ],
      providers: [
        { provide: CreateContentService, useClass: CreateContentServiceMock },
      ]
    });

    fixture = TestBed.createComponent(CreateContentComponent);
    component = fixture.componentInstance;
    createContentService = fixture.debugElement.injector.get(CreateContentService);
  });

  it('should be defined', () => {
    expect(component).toBeDefined();
  });

  it('throws an error if templateQuery is not set', () => {
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
    const spy = spyOn(createContentService, 'getDocumentTypesFromTemplateQuery')
      .and.returnValue(Observable.of(documentTypes));

    component.options = { templateQuery: 'test-template-query' };
    fixture.detectChanges(true);

    expect(spy).toHaveBeenCalledWith('test-template-query');
    expect(component.documentType).toBeUndefined();
    expect(component.documentTypes).toBe(documentTypes);
  });

  it('pre-selects the documentType if only one is returned from the templateQuery', () => {
    const documentTypes: Array<DocumentTypeInfo> = [{ id: 'test-id1', displayName: 'test-name 1' }];
    const spy = spyOn(createContentService, 'getDocumentTypesFromTemplateQuery')
      .and.returnValue(Observable.of(documentTypes));

    component.options = { templateQuery: 'test-template-query' };
    fixture.detectChanges(true);

    expect(component.documentType).toBe('test-id1');
  });

  it('emits feedback if template query is not found', async(() => {
    const spy = spyOn(createContentService, 'getDocumentTypesFromTemplateQuery')
      .and.returnValue(Observable.throw({ status: 404 }));

    component.onError.subscribe((feedback) => {
      expect(feedback.title).toBe('Error loading template query');
      expect(feedback.message).toBe('Can not find template query with name "test-template-query"');
    });

    component.options = { templateQuery: 'test-template-query' };
    fixture.detectChanges(true);
  }));
});
