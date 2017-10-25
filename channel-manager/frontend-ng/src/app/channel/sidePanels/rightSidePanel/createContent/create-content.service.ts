import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromPromise';
import { Injectable } from '@angular/core';

import { TemplateQuery } from './create-content';
import ContentService from '../../../../services/content.service';

@Injectable()
export class CreateContentService {

  constructor(private contentService: ContentService) {}
  documentType: string;

  getTemplateQuery(id): Observable<TemplateQuery> {
    const promise = this.contentService._send('GET', ['templatequery', id], null, true);
    return Observable.fromPromise(promise);
  }

  getNewDocument() {
    // TODO: Should return the newly created document in an object similar to the one returned when
    // calling ContentService.createDraft with uuid, draft will already be present and locked
    return null;
  }
}
