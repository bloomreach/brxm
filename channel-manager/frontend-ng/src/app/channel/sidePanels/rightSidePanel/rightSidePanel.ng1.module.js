/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import { downgradeComponent, downgradeInjectable } from '@angular/upgrade/static';
import { CreateContentComponent } from './createContent/create-content.component.ts';

import rightSidePanelComponent from './rightSidePanel.component';
import editContentComponent from './editContent/editContent.component';
import createContentStep2 from './createContentStep2/createContentStep2.component';
import { CreateContentService } from './createContent/create-content.service.ts';

import fieldsModule from './fields/fields.ng1.module';
import resizeHandleModule from './resizeHandle/resizeHandle.ng1.module';

const rightSidePanelModule = angular
  .module('hippo-cm.channel.rightSidePanelModule', [
    fieldsModule,
    resizeHandleModule,
  ])
  .component('rightSidePanel', rightSidePanelComponent)
  .component('hippoEditContent', editContentComponent)
  .directive('hippoCreateContent', downgradeComponent({ component: CreateContentComponent }))
  .service('CreateContentService', downgradeInjectable(CreateContentService))
  .component('hippoCreateContentStep2', createContentStep2);

export default rightSidePanelModule.name;

