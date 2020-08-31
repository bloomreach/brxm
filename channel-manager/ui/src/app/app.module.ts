/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { NotificationBarModule } from './notification-bar/notification-bar.module';
import { NG1_CHANNEL_SERVICE } from './services/ng1/channel.ng1service';
import { NG1_CONFIG_SERVICE } from './services/ng1/config.ng1.service';
import { NG1_CONTENT_SERVICE } from './services/ng1/content.ng1.service';
import { NG1_IFRAME_SERVICE } from './services/ng1/iframe.ng1service';
import { NG1_PAGE_STRUCTURE_SERVICE } from './services/ng1/page-structure.ng1.service';
import { NG1_PAGE_SERVICE } from './services/ng1/page.ng1.service';
import { NG1_PROJECT_SERVICE } from './services/ng1/project.ng1.service';
import { NG1_ROOT_SCOPE } from './services/ng1/root-scope.service';
import { NG1_WORKFLOW_SERVICE } from './services/ng1/workflow.ng1.service';
import { SharedModule } from './shared/shared.module';
import { VersionsModule } from './versions/versions.module';

@NgModule({
  imports: [
    SharedModule,
    NotificationBarModule,
    VersionsModule,
  ],
  providers: [
    { provide: NG1_CONFIG_SERVICE, useValue: window.angular.element(document.body).injector().get('ConfigService') },
    { provide: NG1_IFRAME_SERVICE, useValue: window.angular.element(document.body).injector().get('HippoIframeService') },
    { provide: NG1_CHANNEL_SERVICE, useValue: window.angular.element(document.body).injector().get('ChannelService') },
    { provide: NG1_CONTENT_SERVICE, useValue: window.angular.element(document.body).injector().get('ContentService') },
    { provide: NG1_WORKFLOW_SERVICE, useValue: window.angular.element(document.body).injector().get('WorkflowService') },
    { provide: NG1_PROJECT_SERVICE, useValue: window.angular.element(document.body).injector().get('ProjectService') },
    { provide: NG1_PAGE_STRUCTURE_SERVICE, useValue: window.angular.element(document.body).injector().get('PageStructureService') },
    { provide: NG1_PAGE_SERVICE, useValue: window.angular.element(document.body).injector().get('PageService') },
    { provide: NG1_ROOT_SCOPE, useValue: window.angular.element(document.body).injector().get('$rootScope') },
  ],
})
export class AppModule {
  ngDoBootstrap(): void { }
}
