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

import { Component, OnInit, EventEmitter, Input, Output } from '@angular/core';
import './step-1.scss';
import { NgForm } from '@angular/forms';

import { CreateContentService } from '../create-content.service';
import { CreateContentOptions, DocumentTypeInfo } from '../create-content';
import FeedbackService from '../../../../../services/feedback.service';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './step-1.html'
})
export class CreateContentComponent implements OnInit {
  @Input() document: any;
  @Input() options: CreateContentOptions;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();
  @Output() onFullWidth: EventEmitter<any> = new EventEmitter();

  documentType: string;
  documentTypes: Array<DocumentTypeInfo> = [];
  isFullWidth: boolean;
  title = 'Create new content';

  constructor(private createContentService: CreateContentService, private feedbackService: FeedbackService) { }

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

  setFullWidth(state) {
    this.isFullWidth = state;
    this.onFullWidth.emit(state);
  }

  close() {
    this.onClose.emit();
  }

  submit(form: NgForm) {
    console.log(form);
    // this.onContinue.emit(form.value);
  }

  private onLoadDocumentTypes(documentTypes) {
    this.documentTypes = documentTypes;
    if (documentTypes.length === 1) {
      this.documentType = documentTypes[0].id;
    }
  }

  private onErrorLoadingTemplateQuery(error) {
    if (error.data && error.data.reason) {
      const errorKey = `ERROR_${error.data.reason}`;
      this.feedbackService.showError(errorKey, error.data.params);
    } else {
      console.error('Unknown error loading template query', error);
    }
  }
}
