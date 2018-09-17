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

import componentEditorModule from './componentEditor/componentEditor.module';
import editComponentConfig from './editComponent.config';
import editComponentMainCtrl from './editComponentMain.controller';
import editComponentService from './editComponent.service';
import editComponentToolsCtrl from './editComponentTools.controller';

const editComponentModule = angular
  .module('hippo-cm.channel.rightSidePanel.editComponent', [
    componentEditorModule,
  ])
  .config(editComponentConfig)
  .service('EditComponentService', editComponentService)
  .controller('editComponentMainCtrl', editComponentMainCtrl)
  .controller('editComponentToolsCtrl', editComponentToolsCtrl);

export default editComponentModule.name;
