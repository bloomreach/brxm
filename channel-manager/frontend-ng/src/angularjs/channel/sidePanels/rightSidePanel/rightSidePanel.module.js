/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import rightSidePanelComponent from './rightSidePanel.component';
import documentFieldsComponent from './documentFields.component';
import primitiveFieldComponent from './primitiveField.component';
import choiceFieldComponent from './choiceField.component';
import compoundFieldComponent from './compoundField.component';
import collapse from './collapse.directive';

const rightSidePanelModule = angular
  .module('hippo-cm.channel.rightSidePanelModule', [])
  .component('rightSidePanel', rightSidePanelComponent)
  .component('documentFields', documentFieldsComponent)
  .component('primitiveField', primitiveFieldComponent)
  .component('choiceField', choiceFieldComponent)
  .component('compoundField', compoundFieldComponent)
  .directive('collapse', collapse);

export default rightSidePanelModule.name;

