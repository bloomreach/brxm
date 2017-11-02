import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromPromise';
import { Injectable } from '@angular/core';

import { DocumentDetails, TemplateQuery } from './create-content.types';
import ContentService from '../../../../services/content.service';

@Injectable()
export class CreateContentService {
  constructor(private contentService: ContentService) {}
  private doc: any;

  getTemplateQuery(id): Observable<TemplateQuery> {
    const promise = this.contentService._send('GET', ['templatequery', id], null, true);
    return Observable.fromPromise(promise);
  }

  getDocument() {
    return this.doc;
  }

  createDraft(documentDetails: DocumentDetails) {
    const promise = this.contentService._send('POST', ['documents'], documentDetails).then(doc => this.doc = doc);
    return Observable.fromPromise(promise);
  }

  generateDocumentUrlByName(name: string): Observable<string> {
    const promise = this.contentService._send('POST', ['slugs'], name, true);
    return Observable.fromPromise(promise);
  }
}
