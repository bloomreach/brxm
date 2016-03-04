/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
export class ChannelCtrl {

  constructor(ChannelService, MountService, PageMetaDataService) {
    'ngInject';

    this.MountService = MountService;
    this.PageMetaDataService = PageMetaDataService;

    this.iframeUrl = ChannelService.getUrl();
    this.isEditMode = false;
    this.isCreatingPreview = false;
  }

  toggleEditMode() {
    if (!this.isEditMode && !this.PageMetaDataService.hasPreviewConfiguration()) {
      this._createPreviewConfiguration();
    } else {
      this.isEditMode = !this.isEditMode;
    }
  }

  _createPreviewConfiguration() {
    const currentMountId = this.PageMetaDataService.getMountId();
    this.isCreatingPreview = true;
    this.MountService.createPreviewConfiguration(currentMountId)
      .then(() => {
        this.isEditMode = true;
      })
      // TODO: handle error response
      .finally(() => {
        this.isCreatingPreview = false;
      });
  }

}
