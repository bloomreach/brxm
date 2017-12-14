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
import { Inject, OnInit, ViewChild } from '@angular/core';
import { Component } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import './name-url-fields-dialog.scss';
import { NameUrlFieldsComponent } from '../../name-url-fields/name-url-fields.component';

@Component({
  selector: 'hippo-name-url-fields-dialog',
  templateUrl: 'name-url-fields-dialog.html',
})
export class NameUrlFieldsDialogComponent {
  private initialValues: { name: string, url: string };
  title: string = this.data.title;
  name: string = this.data.name;
  url: string = this.data.url;
  locale: string = this.data.locale;
  isUrlBusyUpdating: boolean = false;
  @ViewChild(NameUrlFieldsComponent) nameUrlFields: NameUrlFieldsComponent;

  constructor(
    public dialogRef: MatDialogRef<NameUrlFieldsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {}

  ngOnInit() {
    this.initialValues = {
      name: this.data.name,
      url: this.data.url
    }
  }

  onUrlUpdate(value: boolean) {
    this.isUrlBusyUpdating = value;
  }

  change() {
    return this.dialogRef.close({
      name: this.nameUrlFields.nameField,
      url: this.nameUrlFields.urlField,
    });
  }

  cancel() {
    return this.dialogRef.close(this.initialValues);
  }
}
