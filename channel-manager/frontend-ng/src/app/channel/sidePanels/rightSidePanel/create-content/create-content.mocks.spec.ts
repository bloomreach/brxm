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

import { Observable } from 'rxjs/Observable';
import { DocumentDetails, TemplateQuery } from './create-content.types';

export class CreateContentServiceMock {
  getTemplateQuery(id): Observable<TemplateQuery> {
    return Observable.of(null);
  }

  createDraft(documentDetails: DocumentDetails): Observable<TemplateQuery> {
    return Observable.of(null);
  }

  generateDocumentUrlByName(name): Observable<string> {
    return Observable.of(name.replace(/\s+/g, '-').toLowerCase()); // will transform "TestName123" into "test-name-123"
  }
}

export class FeedbackServiceMock {
  showError(key: string, params: Map<string, any>): void {}
}
