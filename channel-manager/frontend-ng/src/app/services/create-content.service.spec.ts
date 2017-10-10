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

import { TestBed, inject, async } from '@angular/core/testing';
import ContentService from './content.service';
import { CreateContentService, DocumentTypeInfo } from './create-content.service';

class MockContentService {
  _send(): Promise<DocumentTypeInfo[]> {
    return Promise.resolve(null);
  }
}

describe('CreateContentService', () => {
  let contentService;

  beforeEach(() => {
    contentService = new MockContentService();

    TestBed.configureTestingModule({
      providers: [
        { provide: ContentService, useValue: contentService },
        CreateContentService
      ]
    });
  });

  it('can get document types by template query',
    inject([CreateContentService], (createContentService: CreateContentService) => {

      const docTypes: DocumentTypeInfo[] = [
        { id: 'doctype1', displayName: 'Doctype 1'}
      ];
      const spy = spyOn(contentService, '_send').and.returnValue(Promise.resolve(docTypes));
      createContentService.getDocumentTypesFromTemplateQuery('test-template-query')
        .subscribe((resolvedDocTypes: DocumentTypeInfo[]) => {
          expect(spy).toHaveBeenCalledWith('GET', ['templatequery', 'test-template-query'], null, true);
          expect(resolvedDocTypes).toBe(docTypes);
        });
    }));

  it('can fail to get document types by template query',
    inject([CreateContentService], (createContentService: CreateContentService) => {

      spyOn(contentService, '_send').and.returnValue(Promise.reject('serverError'));
      createContentService.getDocumentTypesFromTemplateQuery('test-template-query')
        .subscribe(() => { }, (error) => {
          expect(error).toBe('serverError');
        });
    }));
});
