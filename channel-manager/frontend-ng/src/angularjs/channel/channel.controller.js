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

const SIDENAVS = ['components'];

export class ChannelCtrl {

  constructor($log, $scope, $mdSidenav, ChannelService, ScalingService, SessionService, ComponentAdderService) {
    'ngInject';

    this.$log = $log;
    this.$scope = $scope;
    this.$mdSidenav = $mdSidenav;
    this.ChannelService = ChannelService;
    this.ScalingService = ScalingService;
    this.SessionService = SessionService;

    this.iframeUrl = ChannelService.getUrl();
    this.isEditMode = false;
    this.isCreatingPreview = false;

    // reset service state to avoid weird scaling when controller is reloaded due to state change
    ScalingService.setPushWidth(0);

    ComponentAdderService.setContainerClass('catalog-dd-container');
    ComponentAdderService.setContainerItemClass('catalog-dd-container-item');
  }

  toggleEditMode() {
    if (!this.isEditMode && !this.ChannelService.hasPreviewConfiguration()) {
      this._createPreviewConfiguration();
    } else {
      this.isEditMode = !this.isEditMode;
    }
    this._closeSidenavs();
  }

  isEditable() {
    return this.SessionService.hasWriteAccess();
  }

  _closeSidenavs() {
    SIDENAVS.forEach((sidenav) => {
      if (this._isSidenavOpen(sidenav)) {
        this.$mdSidenav(sidenav).close();
      }
    });
    this.ScalingService.setPushWidth(0);
  }

  _createPreviewConfiguration() {
    this.isCreatingPreview = true;
    this.ChannelService.createPreviewConfiguration()
      .then(() => {
        this.isEditMode = true;
      })
      // TODO: handle error response
      .finally(() => {
        this.isCreatingPreview = false;
      });
  }

  toggleSidenav(name) {
    SIDENAVS.forEach((sidenav) => {
      if (sidenav !== name && this._isSidenavOpen(sidenav)) {
        this.$mdSidenav(sidenav).close();
      }
    });
    this.$mdSidenav(name).toggle();
    this.ScalingService.setPushWidth(this._isSidenavOpen(name) ? $('.md-sidenav-left').width() : 0);
  }

  getCatalog() {
    return this.ChannelService.getCatalog();
  }

  _isSidenavOpen(name) {
    return this.$mdSidenav(name).isOpen();
  }
}
