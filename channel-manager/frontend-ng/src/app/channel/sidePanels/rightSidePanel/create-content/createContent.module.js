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

import CreateContentService from './createContent.service';
import documentLocationFieldComponent from './documentLocation/documentLocationField.component';
import nameUrlFieldsComponent from './nameUrlFields/nameUrlFields.component';
import createContentStep1Component from './step1/step1.component';
import createContentStep2Component from './step2/step2.component';


const createContentModule = angular
  .module('hippo-cm.channel.createContentModule', [])
  .component('documentLocationField', documentLocationFieldComponent)
  .component('createContentStep1', createContentStep1Component)
  .component('createContentStep2', createContentStep2Component)
  .component('nameUrlFields', nameUrlFieldsComponent)
  .service('CreateContentService', CreateContentService);

export default createContentModule.name;
