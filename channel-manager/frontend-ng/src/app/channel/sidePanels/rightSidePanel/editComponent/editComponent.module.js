/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import componentEditorService from './componentEditor.service';
import componentFieldsComponent from './componentFields/componentFields.component';
import editComponentConfig from './editComponent.config';
import editComponentMainComponent from './editComponentMain/editComponentMain.component';
import editComponentService from './editComponent.service';
import propertyGroupComponent from './propertyGroup/propertyGroup.component';
import componentVariantsComponent from './componentVariants/componentVariants.component';
import editComponentTabsComponent from './editComponentTabs/editComponentTabs.component';
import experimentsWrapperComponent from './experimentsWrapper/experimentsWrapper.component';

const editComponentModule = angular
  .module('hippo-cm.channel.rightSidePanel.editComponent', [])
  .config(editComponentConfig)
  .service('ComponentEditor', componentEditorService)
  .service('EditComponentService', editComponentService)
  .component('editComponentTabs', editComponentTabsComponent)
  .component('editComponentMain', editComponentMainComponent)
  .component('componentFields', componentFieldsComponent)
  .component('propertyGroup', propertyGroupComponent)
  .component('componentVariants', componentVariantsComponent)
  .component('experimentsWrapper', experimentsWrapperComponent);

export default editComponentModule.name;
