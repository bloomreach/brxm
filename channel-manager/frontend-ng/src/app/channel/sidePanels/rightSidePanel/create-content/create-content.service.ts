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
import 'rxjs/add/observable/fromPromise';
import { Injectable } from '@angular/core';

import { Document, DocumentDetails, TemplateQuery } from './create-content.types';
import ContentService from '../../../../services/content.service';

@Injectable()
export class CreateContentService {
  constructor(private contentService: ContentService) {}
  private doc: Document;

  getTemplateQuery(id): Observable<TemplateQuery> {
    const promise = this.contentService._send('GET', ['templatequery', id], null, true);
    return Observable.fromPromise(promise);
  }

  getDocument(): Document {
    return this.doc;
  }

  createDraft(documentDetails: DocumentDetails): Observable<Document> {
    const promise = this.contentService._send('POST', ['documents'], documentDetails).then(doc => this.doc = doc);
    return Observable.fromPromise(promise);
  }

  generateDocumentUrlByName(name: string, locale: string = ''): Observable<string> {
    const promise = this.contentService._send('POST', ['slugs'], name, true, { locale });
    return Observable.fromPromise(promise);
  }
}
