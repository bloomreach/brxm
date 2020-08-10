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
    DocumentWorkflowService,
    FeedbackService,
    PageService,
    PageStructureService,
  ) {
    'ngInject';

    super();

    this.$state = $state;
    this.PageStructureService = PageStructureService;

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

    function success(key, msg) {
      FeedbackService.showNotification(`${key}_SUCCESS`, { msg });
    }

    function failure(key, msg) {
      try {
        msg = JSON.parse(msg);
      // eslint-disable-next-line no-empty
      } catch (error) {}

      if (msg && msg.cancelled === true) {
        return;
      }

      FeedbackService.showError(`${key}_ERROR`, { msg });
    }

    function invokeWorkflow(onClick, translationKey) {
      return () => onClick(getDocumentId())
        .then(msg => success(translationKey, msg))
        .catch(msg => failure(translationKey, msg))
        .finally(() => PageService.load());
    }

    function addWorkflowAction(id, onClick, config = {}) {
      const translationKey = `TOOLBAR_MENU_XPAGE_${id.replace(/-/g, '_').toUpperCase()}`;
      menu.addAction(id, {
        isEnabled: () => isEnabled(id),
        isVisible: () => isVisible(id),
        onClick: invokeWorkflow(onClick, translationKey),
        translationKey,
        ...config,
      });
    }

    menu.addAction('versions', {
      onClick: () => this._showVersions(),
      translationKey: 'TOOLBAR_MENU_XPAGE_VERSIONS',
    });
    menu.addDivider();

    addWorkflowAction('unpublish', id => DocumentWorkflowService.unpublish(id), { iconSvg: 'unpublish-document' });
    addWorkflowAction('schedule-unpublish', id => DocumentWorkflowService.scheduleUnpublication(id));
    addWorkflowAction('request-unpublish', id => DocumentWorkflowService.requestUnpublication(id));
    addWorkflowAction('request-schedule-unpublish', id => DocumentWorkflowService.requestScheduleUnpublication(id));

    addWorkflowAction('publish', id => DocumentWorkflowService.publish(id), { iconSvg: 'publish-document' });
    addWorkflowAction('schedule-publish', id => DocumentWorkflowService.schedulePublication(id));
    addWorkflowAction('request-publish', id => DocumentWorkflowService.requestPublication(id));
    addWorkflowAction('request-schedule-publish', id => DocumentWorkflowService.requestSchedulePublication(id));
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
