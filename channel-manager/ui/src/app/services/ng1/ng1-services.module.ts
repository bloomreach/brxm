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

import { NG1_CHANNEL_SERVICE } from './channel.ng1.service';
import { NG1_CMS_SERVICE } from './cms.ng1.service';
import { NG1_COMPONENT_EDITOR_SERVICE } from './component-editor.ng1.service';
import { NG1_CONFIG_SERVICE } from './config.ng1.service';
import { NG1_CONTENT_SERVICE } from './content.ng1.service';
import { NG1_IFRAME_SERVICE } from './iframe.ng1.service';
import { NG1_PAGE_STRUCTURE_SERVICE } from './page-structure.ng1.service';
import { NG1_PAGE_SERVICE } from './page.ng1.service';
import { NG1_PROJECT_SERVICE } from './project.ng1.service';
import { NG1_ROOT_SCOPE } from './root-scope.ng1.service';
import { NG1_SITE_MAP_SERVICE } from './site-map.ng1.service';
import { NG1_STATE_SERVICE } from './state.ng1.service';
import { NG1_TARGETING_SERVICE } from './targeting.ng1.service';
import { NG1_WORKFLOW_SERVICE } from './workflow.ng1.service';

@NgModule({
  providers: [
    { provide: NG1_CHANNEL_SERVICE, useValue: window.angular.element(document.body).injector().get('ChannelService') },
    { provide: NG1_CONFIG_SERVICE, useValue: window.angular.element(document.body).injector().get('ConfigService') },
    { provide: NG1_CONTENT_SERVICE, useValue: window.angular.element(document.body).injector().get('ContentService') },
    { provide: NG1_IFRAME_SERVICE, useValue: window.angular.element(document.body).injector().get('HippoIframeService') },
    { provide: NG1_PAGE_STRUCTURE_SERVICE, useValue: window.angular.element(document.body).injector().get('PageStructureService') },
    { provide: NG1_PAGE_SERVICE, useValue: window.angular.element(document.body).injector().get('PageService') },
    { provide: NG1_PROJECT_SERVICE, useValue: window.angular.element(document.body).injector().get('ProjectService') },
    { provide: NG1_TARGETING_SERVICE, useValue: window.angular.element(document.body).injector().get('TargetingService') },
    { provide: NG1_WORKFLOW_SERVICE, useValue: window.angular.element(document.body).injector().get('WorkflowService') },
    { provide: NG1_ROOT_SCOPE, useValue: window.angular.element(document.body).injector().get('$rootScope') },
    { provide: NG1_COMPONENT_EDITOR_SERVICE, useValue: window.angular.element(document.body).injector().get('ComponentEditor') },
    { provide: NG1_STATE_SERVICE, useValue: window.angular.element(document.body).injector().get('$state') },
    { provide: NG1_CMS_SERVICE, useValue: window.angular.element(document.body).injector().get('CmsService') },
    { provide: NG1_SITE_MAP_SERVICE, useValue: window.angular.element(document.body).injector().get('SiteMapService') },
  ],
})
export class Ng1ServicesModule {
}
