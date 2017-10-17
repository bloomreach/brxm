import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromPromise';
import { Injectable } from '@angular/core';

import ContentService from './content.service';

export interface TemplateQuery {
  documentTypes: Array<DocumentTypeInfo>;
}
export interface DocumentTypeInfo {
  id: string;
  displayName: string;
}

@Injectable()
export class CreateContentService {

  constructor(private contentService: ContentService) {}

  getTemplateQuery(id): Observable<TemplateQuery> {
    const promise = this.contentService._send('GET', ['templatequery', id], null, true);
    return Observable.fromPromise(promise);
  }
}
