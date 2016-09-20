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

export class ChannelSidePanelService {
  constructor($mdSidenav, ScalingService) {
    'ngInject';

    this.$mdSidenav = $mdSidenav;
    this.ScalingService = ScalingService;
    this.panels = {
      left: {
        element: 'channel-left-side-panel',
      },
      right: {
        element: 'channel-right-side-panel',
      },
    };
  }

  initialize(side, jQueryElement) {
    this.panels[side].jQueryElement = jQueryElement;
  }

  toggle(side) {
    if (this.isOpen(side)) {
      this.close(side);
    } else {
      this.open(side);
    }
  }

  open(side) {
    if (!this.isOpen(side)) {
      this.$mdSidenav(this.panels[side].element).open();
      this.ScalingService.setPushWidth(side, this.panels[side].jQueryElement.width());
    }
  }

  isOpen(side) {
    return this.panels[side].jQueryElement && this.$mdSidenav(this.panels[side].element).isOpen();
  }

  close(side) {
    if (this.isOpen(side)) {
      this.$mdSidenav(this.panels[side].element).close();

      this.ScalingService.setPushWidth(side, 0);
    }
  }
}
