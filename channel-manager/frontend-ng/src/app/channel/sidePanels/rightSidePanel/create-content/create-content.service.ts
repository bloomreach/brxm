import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromPromise';
import { Injectable } from '@angular/core';

import { TemplateQuery } from './create-content.types';
import ContentService from '../../../../services/content.service';

@Injectable()
export class CreateContentService {
  constructor(private contentService: ContentService) {}

  getTemplateQuery(id): Observable<TemplateQuery> {
    const promise = this.contentService._send('GET', ['templatequery', id], null, true);
    return Observable.fromPromise(promise);
  }

  generateDocumentUrlByName(name: string, locale: string = ''): Observable<string> {
    const promise = this.contentService._send('POST', ['slugs'], name, true, { locale });
    return Observable.fromPromise(promise);
  }
}
