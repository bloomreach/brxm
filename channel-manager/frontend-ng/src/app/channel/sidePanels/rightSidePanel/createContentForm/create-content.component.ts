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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import './create-content.scss';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './create-content.html'
})
export class CreateContentComponent implements OnInit {
  docTypes: any;
  @Input() document: any;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();

  constructor() {}

  ngOnInit() {
    this.docTypes = ['Product', 'Event'];
  }

  close() {
    this.onClose.emit();
  }

  submit(form: NgForm) {
    this.onContinue.emit(form.value);
  }
}
