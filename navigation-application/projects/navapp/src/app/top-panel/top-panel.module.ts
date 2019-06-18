/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SharedModule } from '../shared';

import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { TopPanelComponent } from './components/top-panel.component';
import { BreadcrumbsService } from './services/breadcrumbs.service';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
  ],
  declarations: [
    TopPanelComponent,
    BreadcrumbsComponent,
  ],
  providers: [
    BreadcrumbsService,
  ],
  exports: [
    TopPanelComponent,
    BreadcrumbsComponent,
  ],
})
export class TopPanelModule {}
