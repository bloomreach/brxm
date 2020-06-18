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
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PerfectScrollbarModule } from 'ngx-perfect-scrollbar';

import { SharedModule } from '../shared/shared.module';

import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { SiteSelectionComponent } from './components/site-selection/site-selection.component';
import { TopPanelComponent } from './components/top-panel.component';
import { BreadcrumbsService } from './services/breadcrumbs.service';
import { RightSidePanelService } from './services/right-side-panel.service';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    SharedModule,
    PerfectScrollbarModule,
    FormsModule,
  ],
  declarations: [
    TopPanelComponent,
    BreadcrumbsComponent,
    SiteSelectionComponent,
  ],
  providers: [BreadcrumbsService, RightSidePanelService],
  exports: [TopPanelComponent, SiteSelectionComponent],
})
export class TopPanelModule {}
