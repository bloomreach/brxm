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

import { Component, OnInit, EventEmitter, Input, Output, ViewChild, HostListener } from '@angular/core';
import './step-1.scss';

import { CreateContentService } from '../create-content.service';
import { CreateContentOptions, DocumentDetails, DocumentTypeInfo } from '../create-content.types';
import FeedbackService from '../../../../../services/feedback.service.js';
import { NameUrlFieldsComponent } from '../name-url-fields/name-url-fields.component';
import { DocumentLocationFieldComponent } from '../document-location/document-location-field.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'hippo-create-content-step-1',
  templateUrl: './step-1.html'
})
export class CreateContentComponent implements OnInit {
  locale: string;
  documentType: string;
  documentTypes: Array<DocumentTypeInfo> = [];
  isFullWidth: boolean;
  title = 'Create new content';

  @Input() options: CreateContentOptions;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();
  @Output() onFullWidth: EventEmitter<any> = new EventEmitter();
  @Output() onBeforeStateChange: EventEmitter<any> = new EventEmitter();
  @ViewChild('form') form: HTMLFormElement;
  @ViewChild(NameUrlFieldsComponent) nameUrlFields: NameUrlFieldsComponent;
  @ViewChild(DocumentLocationFieldComponent) documentLocationField: DocumentLocationFieldComponent;

  @HostListener('keydown', ['$event']) closeOnEsc(e) {
    if (e.which === 27) {
      this.close();
    }
  }

  constructor(private createContentService: CreateContentService, private feedbackService: FeedbackService,
              private translate: TranslateService) { }

  ngOnInit() {
    if (!this.options) {
      throw new Error('Input "options" is required');
    }

    if (!this.options.templateQuery) {
      throw new Error('Configuration option "templateQuery" is required');
    }

    this.createContentService
      .getTemplateQuery(this.options.templateQuery)
      .subscribe(
        (templateQuery) => this.onLoadDocumentTypes(templateQuery.documentTypes),
        (error) => this.onErrorLoadingTemplateQuery(error),
      );
  }

  setWidthState(state) {
    this.isFullWidth = state;
    this.onFullWidth.emit(state);
  }

  close() {
    this.onClose.emit();
  }

  submit() {
    const document: DocumentDetails = {
      name: this.nameUrlFields.nameField,
      slug: this.nameUrlFields.urlField,
      templateQuery: this.options.templateQuery,
      documentTypeId: this.documentType,
      rootPath: '/content/documents/hap/news',
      defaultPath: '2017/11',
    };
    this.createContentService
      .createDraft(document)
      .subscribe(
        (response) => this.onContinue.emit(),
        (error) => this.onErrorCreatingDraft(error),
      );
  }

  setLocale(locale: string) {
    this.locale = locale;
  }

  private onLoadDocumentTypes(documentTypes) {
    this.documentTypes = documentTypes;

    if (documentTypes.length === 1) {
      this.documentType = documentTypes[0].id;
    }
  }

  private onErrorLoadingTemplateQuery(error) {
    if (error.data && error.data.reason) {
      const errorKey = this.translate.instant(`ERROR_${error.data.reason}`);
      this.feedbackService.showError(errorKey, error.data.params);
    } else {
      console.error('Unknown error loading template query', error);
    }
  }

  private onErrorCreatingDraft(error) {
    if (error.data && error.data.reason) {
      const errorKey = this.translate.instant(`ERROR_${error.data.reason}`);
      this.feedbackService.showError(errorKey);
    } else {
      console.error('Unknown error creating new draft document', error);
    }
  }

  private resetBeforeStateChange() {
    this.onBeforeStateChange.emit();
  }
}
