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
import { DocumentDetails, DocumentTypeInfo, TemplateQuery, Folder } from './create-content.types';
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material';

export class CreateContentServiceMock {
  getTemplateQuery(id) {
    return Observable.of(null);
  }

  createDraft(documentDetails: DocumentDetails) {
    return Observable.of(null);
  }

  generateDocumentUrlByName(name, locale: string = '') {
    return Observable.of(name.replace(/\s+/g, '-').toLowerCase()); // will transform "TestName123" into "test-name-123"
  }

  getDocument() {
    return {
      id: 'testId',
      displayName: 'test document',
      info: {
        dirty: false,
        type: { id: 'ns:testdocument' }
      }
    };
  }

  getFolders(path: string) {
    return Promise.resolve([]);
  }

  setDraftNameUrl(documentId: string, data: { name, url }) {
    const document: any = this.getDocument();
    document.id = documentId;
    document.displayName = data.name;
    document.urlName = data.url;
    return Promise.resolve(document);
  }

  deleteDraft() {
    return Promise.resolve();
  }
}

export class FeedbackServiceMock {
  showError(key: string, params: Map<string, any>): void {}
}

export class ContentServiceMock {
  getDocumentType(id: string): Promise<DocumentTypeInfo> {
    return Observable.of({ id: 'ns:testdocument', displayName: 'test-name 1' }).toPromise();
  }

  saveDraft() {
    return Observable.of(true).toPromise();
  }
}

export class DialogServiceMock {
  confirm(): any {
    return new ConfirmDialogMock();
  }

  show(dialog: object): Promise<void> {
    return Promise.resolve();
  }
}

export class ConfirmDialogMock {
  title() {
    return this;
  }

  textContent() {
    return this;
  }

  ok() {
    return this;
  }

  cancel() {
    return this;
  }
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

export class ChannelServiceMock {
  getChannel() {
    return {
      contentRoot: '/channel/content'
    };
  }
}
