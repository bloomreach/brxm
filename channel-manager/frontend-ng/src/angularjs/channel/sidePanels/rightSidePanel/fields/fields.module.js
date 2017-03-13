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
import documentFieldsComponent from './documentFields.component';
import primitiveFieldComponent from './primitiveField.component';
import choiceFieldComponent from './choiceField.component';
import compoundFieldComponent from './compoundField.component';
import ckeditorComponent from './ckeditor.component';
import ckeditorService from './ckeditor.service';
import collapse from './collapse.directive';

const fieldsModule = angular
  .module('hippo-cm.channel.fieldsModule', [])
  .component('documentFields', documentFieldsComponent)
  .component('primitiveField', primitiveFieldComponent)
  .component('choiceField', choiceFieldComponent)
  .component('compoundField', compoundFieldComponent)
  .component('ckeditor', ckeditorComponent)
  .directive('collapse', collapse)
  .service('CKEditorService', ckeditorService);

export default fieldsModule.name;
