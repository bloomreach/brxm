/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import './fields.scss';

import choiceFieldComponent from './choiceField/choiceField.component';
import ckeditorModule from './ckeditor/ckeditor.module';
import collapse from './collapse/collapse.directive';
import compoundFieldComponent from './compoundField/compoundField.component';
import documentFieldsComponent from './documentFields/documentFields.component';
import fieldService from './field.service';
import imageLinkComponent from './imageLink/imageLink.component';
import nodeLinkComponent from './nodeLink/nodeLink.component';
import primitiveFieldComponent from './primitiveField/primitiveField.component';

const fieldsModule = angular
  .module('hippo-cm.channel.rightSidePanel.contentEditor.fields', [
    ckeditorModule,
  ])
  .component('choiceField', choiceFieldComponent)
  .component('compoundField', compoundFieldComponent)
  .component('documentFields', documentFieldsComponent)
  .component('imageLink', imageLinkComponent)
  .component('nodeLink', nodeLinkComponent)
  .component('primitiveField', primitiveFieldComponent)
  .directive('collapse', collapse)
  .service('FieldService', fieldService);

export default fieldsModule.name;
