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

import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { HintsComponent } from '../../../../shared/components/hints/hints.component';
import { CreateContentComponent } from './step-1/step-1.component';
import { CreateContentStep2Component } from './step-2/step-2.component';
import { SharedModule } from '../../../../shared/shared.module';
import { NameUrlFieldsComponent } from './name-url-fields/name-url-fields.component';
import { FieldsEditorDirective } from '../fieldsEditor/fields-editor.component';
import { SharedspaceToolbarDirective } from '../fields/ckeditor/sharedspace-toolbar/sharedspace-toolbar.component';
import { NameUrlFieldsDialogComponent } from './step-2/name-url-fields-dialog/name-url-fields-dialog';
import { DocumentLocationFieldComponent } from './document-location/document-location-field.component';
import { CapitalizePipe } from '../../../../shared/pipes/capitalize.pipe';
import { CreateContentServiceProvider } from './create-content.service.provider';

@NgModule({
  imports: [
    SharedModule,
    FormsModule
  ],
  declarations: [
    SharedspaceToolbarDirective,
    FieldsEditorDirective,
    CreateContentComponent,
    CreateContentStep2Component,
    HintsComponent,
    NameUrlFieldsComponent,
    NameUrlFieldsDialogComponent,
    DocumentLocationFieldComponent
  ],
  entryComponents: [
    CreateContentComponent,
    CreateContentStep2Component,
    HintsComponent,
    NameUrlFieldsComponent,
    NameUrlFieldsDialogComponent
  ],
  providers: [
    CreateContentServiceProvider
  ]
})
export class CreateContentModule {}
