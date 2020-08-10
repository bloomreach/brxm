/*
 * Copyright 2020 Bloomreach
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
    $state,
    $translate,
    DialogService,
    DocumentWorkflowService,
    PageService,
    PageStructureService,
  ) {
    'ngInject';

    super();

    this.$state = $state;
    this.PageStructureService = PageStructureService;

    function alert(msg) {
      const dialog = DialogService.alert()
        .textContent(msg)
        .ok($translate.instant('OK'));

      return DialogService.show(dialog);
    }

    function isEnabled(action) {
      return PageService.isActionEnabled('xpage', action);
    }

    function isVisible(action) {
      return PageService.hasAction('xpage', action);
    }

    function getDocumentId() {
      return PageService.getState('xpage').id;
    }

    const menu = this.defineMenu('xpage', {
      isVisible: () => PageService.hasActions('xpage'),
      translationKey: 'TOOLBAR_BUTTON_XPAGE',
    });

    function addAction(id, onClick, config = {}) {
      const translationKey = `TOOLBAR_MENU_XPAGE_${id.replace(/-/g, '_').toUpperCase()}`;
      menu.addAction(id, {
        isEnabled: () => isEnabled(id),
        isVisible: () => isVisible(id),
        onClick: () => onClick(getDocumentId())
          .then(() => PageService.load())
          .catch((msg) => {
            PageService.load();
            alert(msg);
          }),
        translationKey,
        ...config,
      });
    }

    menu.addAction('versions', {
      onClick: () => this._showVersions(),
      translationKey: 'TOOLBAR_MENU_XPAGE_VERSIONS',
    });
    menu.addDivider();

    addAction('unpublish', id => DocumentWorkflowService.unpublish(id), { iconSvg: 'unpublish-document' });
    addAction('schedule-unpublish', id => DocumentWorkflowService.scheduleUnpublication(id));
    addAction('request-unpublish', id => DocumentWorkflowService.requestUnpublication(id));
    addAction('request-schedule-unpublish', id => DocumentWorkflowService.requestScheduleUnpublication(id));

    addAction('publish', id => DocumentWorkflowService.publish(id), { iconSvg: 'publish-document' });
    addAction('schedule-publish', id => DocumentWorkflowService.schedulePublication(id));
    addAction('request-publish', id => DocumentWorkflowService.requestPublication(id));
    addAction('request-schedule-publish', id => DocumentWorkflowService.requestSchedulePublication(id));
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
