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

import './editContentMain.scss';
import config from './editContent.config';
import addToProjectComponent from './addToProject/addToProject.component';
import editContentService from './editContent.service';
import editContentIconCtrl from './editContentIcon.controller';
import editContentMainCtrl from './editContentMain.controller';
import editContentToolsCtrl from './editContentTools.controller';

const editContentModule = angular
  .module('hippo-cm.channel.rightSidePanel.editContentModule', [])
  .config(config)
  .service('EditContentService', editContentService)
  .component('addToProject', addToProjectComponent)
  .controller('editContentIconCtrl', editContentIconCtrl)
  .controller('editContentMainCtrl', editContentMainCtrl)
  .controller('editContentToolsCtrl', editContentToolsCtrl);

export default editContentModule.name;
