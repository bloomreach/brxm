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
import 'rxjs/add/operator/toPromise';
import { DocumentDetails, DocumentTypeInfo, TemplateQuery } from './create-content.types';
import { Component } from "@angular/core";
import { MdDialogRef } from "@angular/material";

export class CreateContentServiceMock {
  getTemplateQuery(id): Observable<TemplateQuery> {
    return Observable.of(null);
  }

  createDraft(documentDetails: DocumentDetails): Observable<TemplateQuery> {
    return Observable.of(null);
  }

  generateDocumentUrlByName(name, locale: string = ''): Observable<string> {
    return Observable.of(name.replace(/\s+/g, '-').toLowerCase()); // will transform "TestName123" into "test-name-123"
  }

  getDocument() {
    return {
      id: 'testId',
      displayName: 'test document',
      info: {
        dirty: false,
        type: {
          id: 'ns:testdocument',
        }
      }
    };
  }
}

export class FeedbackServiceMock {
  showError(key: string, params: Map<string, any>): void {}
}

export class ContentServiceMock {
  getDocumentType(id: string): Promise<DocumentTypeInfo> {
    return Observable.of({ id: 'ns:testdocument', displayName: 'test-name 1' }).toPromise();
  }
}

export class DialogServiceMock {
  confirm(): void {}
}

export class FieldServiceMock {
  setDocumentId(id: string): void {}
}

export class MdDialogMock {
  open() {
    return new MdDialogRefMock();
  }
}

export class MdDialogRefMock {
  afterClosed () {
    return Observable.of({ name: 'docName', url: 'doc-url' });
  }
}
