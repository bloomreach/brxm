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
    $log,
    $state,
    DocumentWorkflowService,
    FeedbackService,
    PageService,
    PageToolsService,
    EditContentService,
  ) {
    'ngInject';

    super();

    function isEnabled(action) {
      return PageService.isActionEnabled('xpage', action);
    }

    function isVisible(action) {
      return PageService.hasAction('xpage', action);
    }

    function getDocumentId() {
      return PageService.getState('xpage').id;
    }

    function getDocumentName() {
      return PageService.getState('xpage').name;
    }

    function showVersions() {
      EditContentService.startEditing(getDocumentId(), 'hippo-cm.channel.edit-page.versions');
    }

    function showContent() {
      EditContentService.startEditing(getDocumentId(), 'hippo-cm.channel.edit-page.content');
    }

    const menu = this.defineMenu('xpage', {
      isVisible: () => PageService.hasActions('xpage'),
      translationKey: 'TOOLBAR_BUTTON_XPAGE',
    });

    function failure(key, msg) {
      try {
        msg = JSON.parse(msg);
      // eslint-disable-next-line no-empty
      } catch (error) {}

      if (msg && msg.cancelled === true) {
        return;
      }

      $log.error(`Failed to execute workflow "${key}" on document[${getDocumentId()}]: ${msg}`);
      FeedbackService.showError(`${key}_ERROR`, {
        msg,
        documentName: getDocumentName(),
      });
    }

    function invokeWorkflow(onClick, translationKey) {
      return () => onClick(getDocumentId())
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

    menu
      .addAction('tools', {
        isVisible: () => PageToolsService.hasExtensions(),
        onClick: () => PageToolsService.showPageTools(),
        translationKey: 'TOOLBAR_MENU_XPAGE_TOOLS',
      })
      .addAction('content', {
        onClick: () => showContent(),
        translationKey: 'TOOLBAR_MENU_XPAGE_CONTENT',
      })
      .addAction('versions', {
        onClick: () => showVersions(),
        translationKey: 'TOOLBAR_MENU_XPAGE_VERSIONS',
      })
      .addDivider({
        isVisible: () => PageService.hasSomeAction('xpage',
          'unpublish',
          'schedule-unpublish',
          'request-unpublish',
          'publish',
          'schedule-publish',
          'request-publish',
          'request-schedule-publish'),
      });

    addWorkflowAction('unpublish', id => DocumentWorkflowService.unpublish(id), {
      iconName: 'mdi-minus-circle',
    });
    addWorkflowAction('schedule-unpublish', id => DocumentWorkflowService.scheduleUnpublication(id));
    addWorkflowAction('request-unpublish', id => DocumentWorkflowService.requestUnpublication(id), {
      iconName: 'mdi-minus-circle',
    });
    addWorkflowAction('request-schedule-unpublish', id => DocumentWorkflowService.requestScheduleUnpublication(id));

    addWorkflowAction('publish', id => DocumentWorkflowService.publish(id), {
      iconName: 'mdi-check-circle',
    });
    addWorkflowAction('schedule-publish', id => DocumentWorkflowService.schedulePublication(id));
    addWorkflowAction('request-publish', id => DocumentWorkflowService.requestPublication(id), {
      iconName: 'mdi-check-circle',
    });
    addWorkflowAction('request-schedule-publish', id => DocumentWorkflowService.requestSchedulePublication(id));
  }
}

export default XPageMenuService;
