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


import ListingCtrl from './listing.controller';
import listingDirective from './listing.directive';
import PickerService from './picker.service';
import PickerCtrl from './picker.controller';
import uiTreeModule from '../tree/tree.ng1.module';

const pickerModule = angular
  .module('hippo-cm.channel.sitemenu.picker', [
    uiTreeModule.name,
  ])
  .controller('PickerCtrl', PickerCtrl)
  .controller('ListingCtrl', ListingCtrl)
  .service('PickerService', PickerService)
  .directive('listing', listingDirective);

export default pickerModule;
