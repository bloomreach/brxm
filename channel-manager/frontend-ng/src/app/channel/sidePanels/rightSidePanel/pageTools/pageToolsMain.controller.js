/*
 * Copyright 2018-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class pageToolsMainCtrl {
  constructor(PageToolsService) {
    'ngInject';

    this.PageToolsService = PageToolsService;
  }

  $onInit() {
    this.extensions = this.PageToolsService.getExtensions();
  }

  get selectedTab() {
    return this.extensions.findIndex(item => item.id === this.PageToolsService.selectedExtensionId);
  }

  set selectedTab(index) {
    this.PageToolsService.selectedExtensionId = this.extensions[index].id;
  }
}

export default pageToolsMainCtrl;
