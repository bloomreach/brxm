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

import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { Observable } from 'rxjs/rx';
import { CreateContentService } from '../create-content.service';

@Component({
  selector: 'hippo-name-url-fields',
  templateUrl: 'name-url-fields.html'
})

export class NameUrlFieldsComponent implements OnInit {
  @ViewChild('form') form: HTMLFormElement;
  @ViewChild('nameInputElement') nameInputElement: ElementRef;
  @Input('nameField') nameField: string;
  @Input('urlField') urlField: string;
  @Output() nameFieldChange: EventEmitter<string> = new EventEmitter();
  @Output() urlFieldChange: EventEmitter<string> = new EventEmitter();
  public urlEditMode: { state: boolean, oldValue: string } = { state: false, oldValue: '' };

  constructor(private createContentService: CreateContentService) {}

  ngOnInit() {
    Observable.fromEvent(this.nameInputElement.nativeElement, 'keyup')
      .debounceTime(1000)
      .subscribe(() => {
        this.setDocumentUrlByName(this.nameField);
        this.nameFieldChange.next(this.nameField);
        this.urlFieldChange.next(this.urlField);
      });
  }

  setDocumentUrlByName(name: string) {
    this.createContentService.generateDocumentUrlByName(name)
      .subscribe((slug) => this.urlField = slug);
  }

  setDocumentUrlEditable(state: boolean) {
    if (state) {
      this.urlEditMode.oldValue = this.urlField;
    }
    this.urlEditMode.state = state;
  }

  cancelUrlEditing() {
    this.urlField = this.urlEditMode.oldValue;
    this.setDocumentUrlEditable(false);
  }
}
