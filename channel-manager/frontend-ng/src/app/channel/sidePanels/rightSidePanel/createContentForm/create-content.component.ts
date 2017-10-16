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

import { Component, OnInit, EventEmitter, Input, Output, AfterViewInit } from '@angular/core';
import './create-content.scss';
import { NgForm } from '@angular/forms';

import { CreateContentService, DocumentTypeInfo } from '../../../../services/create-content.service';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './create-content.html'
})
export class CreateContentComponent implements AfterViewInit {
  @Input() document: any;
  @Input() options: any;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();

  documentType: string;
  documentTypes: Array<DocumentTypeInfo> = [];

  constructor(private createContentService: CreateContentService) { }

  ngAfterViewInit() {
    // this.urlInputSubscription = Observable.fromEvent(this.input.nativeElement, 'keyup')
    //   .debounceTime(1000)
    //   .subscribe(e => this.validateUrl(this.input.nativeElement.value));
    this.createContentService.getDocumentTypesFromTemplateQuery(this.options.templateQuery)
      .subscribe((documentTypes) => {
        this.documentTypes = documentTypes;
        if (documentTypes.length === 1) {
          this.documentType = documentTypes[0].id;
        }
      });
  }

  close() {
    this.onClose.emit();
  }

  submit(form: NgForm) {
    console.log(form);
    // this.onContinue.emit(form.value);
  }
}
