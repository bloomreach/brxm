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

import { Component, ElementRef, EventEmitter, Input, OnInit, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { CreateContentService } from '../create-content.service';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromEvent';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/filter';

@Component({
  selector: 'hippo-name-url-fields',
  templateUrl: 'name-url-fields.html'
})
export class NameUrlFieldsComponent implements OnInit, OnChanges {
  @ViewChild('form') form: HTMLFormElement;
  @ViewChild('nameInputElement') nameInputElement: ElementRef;
  @Input('nameField') nameField: string;
  @Input('urlField') urlField: string;
  @Output() nameFieldChange: EventEmitter<string> = new EventEmitter();
  @Output() urlFieldChange: EventEmitter<string> = new EventEmitter();
  @Input() locale: string;
  public isManualUrlMode = false;

  constructor(private createContentService: CreateContentService) {}

  ngOnInit() {
    Observable.fromEvent(this.nameInputElement.nativeElement, 'keyup')
      .filter(() => !this.isManualUrlMode)
      .debounceTime(1000)
      .subscribe(() => {
        this.setDocumentUrlByName();
        this.nameFieldChange.next(this.nameField);
        this.urlFieldChange.next(this.urlField);
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.hasOwnProperty('locale') && this.form.controls.name) {
      this.setDocumentUrlByName();
    }
  }

  setDocumentUrlByName() {
    this.createContentService.generateDocumentUrlByName(this.nameField, this.locale)
      .subscribe((slug) => this.urlField = slug);
  }

  setManualUrlEditMode(state: boolean) {
    if (state) {
      this.isManualUrlMode = true;
    } else {
      this.isManualUrlMode = false;
      this.setDocumentUrlByName();
    }
  }
}
