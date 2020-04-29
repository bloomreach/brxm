/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { NgSdkModule } from '@bloomreach/ng-sdk';

import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { IndexComponent, CMS_BASE_URL, SPA_BASE_URL } from './index/index.component';
import { IsExternalLinkPipe } from './is-external-link.pipe';
import { IsInternalLinkPipe } from './is-internal-link.pipe';
import { BannerComponent } from './banner/banner.component';
import { ContentComponent } from './content/content.component';
import { MenuComponent } from './menu/menu.component';
import { ParseUrlPipe } from './parse-url.pipe';

@NgModule({
  bootstrap: [AppComponent],
  declarations: [
    AppComponent,
    IndexComponent,
    BannerComponent,
    ContentComponent,
    IsExternalLinkPipe,
    IsInternalLinkPipe,
    MenuComponent,
    ParseUrlPipe,
  ],
  entryComponents: [
    BannerComponent,
    ContentComponent,
    MenuComponent,
  ],
  imports: [
    BrowserModule,
    NgSdkModule,
    AppRoutingModule,
  ],
  providers: [
    { provide: CMS_BASE_URL, useValue: environment.cmsBaseUrl },
    { provide: SPA_BASE_URL, useValue: environment.spaBaseUrl },
  ],
})
export class AppModule { }
