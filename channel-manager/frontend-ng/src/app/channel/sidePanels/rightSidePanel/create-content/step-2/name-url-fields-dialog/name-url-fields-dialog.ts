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
import { Inject } from '@angular/core';
import { Component } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import './name-url-fields-dialog.scss';

@Component({
  selector: 'hippo-name-url-fields-dialog',
  templateUrl: 'name-url-fields-dialog.html',
})
export class NameUrlFieldsDialogComponent {
  constructor(public dialogRef: MatDialogRef<NameUrlFieldsDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: any) {}
  title: string = this.data.title;
  name: string = this.data.name;
  url: string = this.data.url;

  submitDialog(resolve: boolean = false): void {
    if (resolve) {
      return this.dialogRef.close({
        name: this.name,
        url: this.url,
      });
    }
    return this.dialogRef.close(resolve);
  }
}
