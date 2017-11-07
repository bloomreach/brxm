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

import rightSidePanelComponent from './rightSidePanel.component';
import editContentComponent from './editContent/editContent.component';
import fieldsEditorComponent from './fieldsEditor/fieldsEditor.ng1.component';

import fieldsModule from './fields/fields.ng1.module';
import resizeHandleModule from './resizeHandle/resizeHandle.ng1.module';

import { CreateContentComponent } from './create-content/step-1/step-1.component.ts';
import { CreateContentStep2Component } from './create-content/step-2/step-2.component.ts';
import { NameUrlFieldsComponent } from './create-content/name-url-fields/name-url-fields.component.ts';
import { CreateContentService } from './create-content/create-content.service.ts';

const rightSidePanelModule = angular
  .module('hippo-cm.channel.rightSidePanelModule', [
    fieldsModule,
    resizeHandleModule,
  ])
  .component('rightSidePanel', rightSidePanelComponent)
  .component('hippoEditContent', editContentComponent)
  .component('fieldsEditor', fieldsEditorComponent)
  .directive('hippoNameUrlFields', downgradeComponent({ component: NameUrlFieldsComponent }))
  .directive('hippoCreateContentStep1', downgradeComponent({ component: CreateContentComponent }))
  .directive('hippoCreateContentStep2', downgradeComponent({ component: CreateContentStep2Component }))
  .service('CreateContentService', downgradeInjectable(CreateContentService));

export default rightSidePanelModule.name;

