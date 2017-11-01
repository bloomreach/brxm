import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromPromise';
import { Injectable } from '@angular/core';

import { TemplateQuery } from './create-content';
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

  createDraft(documentDetails) {
    return new Promise((resolve, reject) => {
      this.contentService.createDraft('64ab4648-0c20-40d2-9f18-d7a394f0334b')
        .then(doc => {
          this.doc = doc;
          resolve(doc);
        }).catch(e => reject(e));
    });
  }
}
