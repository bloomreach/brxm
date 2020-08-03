/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import MenuService from '../menu.service';

class XPageMenuService extends MenuService {
  constructor(
    $translate,
    $state,
    DialogService,
    PageService,
    PageStructureService,
  ) {
    'ngInject';

    super();

    this.$translate = $translate;
    this.$state = $state;
    this.DialogService = DialogService;
    this.PageStructureService = PageStructureService;

    function isEnabled(action) {
      return PageService.isActionEnabled('xpage', action);
    }

    function isVisible(action) {
      return PageService.hasAction('xpage', action);
    }

    const menu = this.defineMenu('xpage', {
      isVisible: () => PageService.hasActions('xpage'),
      translationKey: 'TOOLBAR_BUTTON_XPAGE',
    });

    menu
      .addAction('versions', {
        onClick: () => this._showVersions(),
        translationKey: 'TOOLBAR_MENU_XPAGE_VERSIONS',
      })
      .addAction('new', {
        isEnabled: () => isEnabled('new'),
        isVisible: () => isVisible('new'),
        onClick: () => this._showDialog('new'),
        translationKey: 'TOOLBAR_MENU_XPAGE_NEW',
      })
      .addAction('move', {
        isEnabled: () => isEnabled('move'),
        isVisible: () => isVisible('move'),
        onClick: () => this._showDialog('move'),
        translationKey: 'TOOLBAR_MENU_XPAGE_MOVE',
      })
      .addAction('delete', {
        isEnabled: () => isEnabled('delete'),
        isVisible: () => isVisible('delete'),
        onClick: () => this._showDialog('delete'),
        translationKey: 'TOOLBAR_MENU_XPAGE_DELETE',
      });
  }

  _showDialog(msg) {
    const confirm = this.DialogService.confirm()
      .textContent(msg)
      .ok(this.$translate.instant('OK'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _showVersions() {
    const documentId = this.PageStructureService
      .getPage()
      .getMeta()
      .getUnpublishedVariantId();

    this.$state.go('hippo-cm.channel.edit-content', {
      documentId,
      showVersionsInfo: true,
    });
  }
}

export default XPageMenuService;
