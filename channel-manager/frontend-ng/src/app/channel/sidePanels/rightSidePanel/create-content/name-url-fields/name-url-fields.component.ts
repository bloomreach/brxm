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

import {
  Component, ElementRef, Input, OnInit, OnChanges, SimpleChanges, ViewChild, Output,
  EventEmitter
} from '@angular/core';
import { CreateContentService } from '../create-content.service';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/fromEvent';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/do';

@Component({
  selector: 'hippo-name-url-fields',
  templateUrl: 'name-url-fields.html'
})
export class NameUrlFieldsComponent implements OnInit, OnChanges {
  @ViewChild('form') form: HTMLFormElement;
  @ViewChild('nameInputElement') nameInputElement: ElementRef;
  @Input() locale: string;
  @Input() nameField: string;
  @Input() urlField: string;
  @Output() urlUpdate: EventEmitter<boolean> = new EventEmitter();
  public isManualUrlMode = false;

  constructor(private createContentService: CreateContentService) {}

  ngOnInit() {
    this.nameField = this.nameField || '';
    this.urlField = this.urlField || '';

    Observable.fromEvent(this.nameInputElement.nativeElement, 'keyup')
      .filter(() => !this.isManualUrlMode)
      .do(() => this.urlUpdate.emit(true))
      .debounceTime(1000)
      .subscribe(() => {
        this.setDocumentUrlByName();
        this.urlUpdate.emit(false);
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.hasOwnProperty('locale') && this.form.controls.name) {
      this.setDocumentUrlByName();
    }
  }

  setDocumentUrlByName() {
    const observable = this.createContentService.generateDocumentUrlByName(this.nameField, this.locale);
    observable.subscribe((slug) => {
      this.urlField = slug;
    });
    return observable;
  }

  validateFields() {
    const conditions = [
      this.nameField.length !== 0, // name empty
      this.urlField.length !== 0, // url empty
      /\S/.test(this.nameField), // name is only whitespace(s)
      /\S/.test(this.urlField) // url is only whitespaces
    ];
    return conditions.every((condition) => condition === true);
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
