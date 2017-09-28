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
import { downgradeComponent } from '@angular/upgrade/static';
import { ImageLinkComponent } from './imageLinkField/imageLink.component.ts';
import documentFieldsComponent from './documentFields/documentFields.component';
import primitiveFieldComponent from './primitiveField/primitiveField.component';
import choiceFieldComponent from './choiceField/choiceField.component';
import compoundFieldComponent from './compoundField/compoundField.component';
import ckeditorModule from './ckeditor/ckeditor.ng1.module';
import collapse from './collapse/collapse.directive';
import fieldService from './field.service';


const fieldsModule = angular
  .module('hippo-cm.channel.fieldsModule', [
    ckeditorModule,
  ])
  .component('documentFields', documentFieldsComponent)
  .component('primitiveField', primitiveFieldComponent)
  .component('choiceField', choiceFieldComponent)
  .component('compoundField', compoundFieldComponent)
  .directive('collapse', collapse)
  .service('FieldService', fieldService)
  .directive('imageLink', downgradeComponent({ component: ImageLinkComponent }));

export default fieldsModule.name;
