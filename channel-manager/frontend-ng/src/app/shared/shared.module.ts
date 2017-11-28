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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { FlexLayoutModule, BREAKPOINTS } from '@angular/flex-layout';
import { MaterialModule } from './material/material.module';

import { CmsServiceProvider } from './services/cms.service.provider';
import { ContentServiceProvider } from './services/content.service.provider';
import { DialogServiceProvider } from './services/dialog.service.provider';
import { FeedbackServiceProvider } from './services/feedback.service.provider';
import { FieldServiceProvider } from './services/field.service.provider';
import { TranslateModule } from '@ngx-translate/core';

// Curently the impl of flex-layout break-points clashes with $rootScope.$apply()
// when opening the right-side panel. Since we don't need the breakpoints atm,
// we pass an empty array instead of the default value to prevent break-point events.
export const EmptyBreakPointsProvider = {
  provide: BREAKPOINTS,
  useValue: [],
};

@NgModule({
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    FlexLayoutModule,
    MaterialModule,
    TranslateModule
  ],
  exports: [
    BrowserAnimationsModule,
    BrowserModule,
    FlexLayoutModule,
    MaterialModule,
    TranslateModule
  ],
  providers: [
    CmsServiceProvider,
    ContentServiceProvider,
    EmptyBreakPointsProvider,
    DialogServiceProvider,
    FeedbackServiceProvider,
    FieldServiceProvider,
  ]
})
export class SharedModule {}
