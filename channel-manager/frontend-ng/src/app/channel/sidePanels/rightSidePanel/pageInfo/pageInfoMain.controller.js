/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import './pageInfoMain.scss';

class PageInfoMainCtrl {
  constructor(ExtensionService, PageInfoService) {
    'ngInject';

    this.ExtensionService = ExtensionService;
    this.PageInfoService = PageInfoService;
  }

  $onInit() {
    this.extensions = this.ExtensionService.getExtensions('page');
  }

  get selectedExtensionId() {
    return this.PageInfoService.selectedExtensionId;
  }

  set selectedExtensionId(extensionId) {
    this.PageInfoService.selectedExtensionId = extensionId;
  }

  close() {
    this.PageInfoService.closePageInfo();
  }
}

export default PageInfoMainCtrl;
