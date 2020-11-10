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

export default class XPageMenuService extends MenuService {
  constructor(
    $log,
    $translate,
    ChannelService,
    DocumentWorkflowService,
    EditComponentService,
    EditContentService,
    FeedbackService,
    HippoIframeService,
    HstService,
    PageService,
    PageStructureService,
    PageToolsService,
    SiteMapService,
  ) {
    'ngInject';

    super();

    this.$log = $log;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.DocumentWorkflowService = DocumentWorkflowService;
    this.EditComponentService = EditComponentService;
    this.EditContentService = EditContentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstService = HstService;
    this.PageService = PageService;
    this.PageStructureService = PageStructureService;
    this.PageToolsService = PageToolsService;
    this.SiteMapService = SiteMapService;

    this._getRequestTranslationKey = this._getRequestTranslationKey.bind(this);
    this._initialize();
    this._addWorkflowActions();
  }

  _initialize() {
    this._menu = this
      .defineMenu('xpage', {
        isVisible: () => this.PageService.hasActions('xpage'),
        translationKey: 'TOOLBAR_BUTTON_XPAGE',
      })
      .addAction('tools', {
        isVisible: () => this.PageToolsService.hasExtensions(),
        onClick: () => this.PageToolsService.showPageTools(),
        translationKey: 'TOOLBAR_MENU_XPAGE_TOOLS',
      })
      .addAction('content', {
        onClick: () => this._showContent(),
        translationKey: 'TOOLBAR_MENU_XPAGE_CONTENT',
      })
      .addAction('versions', {
        onClick: () => this._showVersions(),
        translationKey: 'TOOLBAR_MENU_XPAGE_VERSIONS',
      });
  }

  _addWorkflowActions() {
    this._menu.addDivider({
      isVisible: () => this.PageService.hasSomeAction('xpage',
        'cancel',
        'accept',
        'reject',
        'rejected'),
    });

    this._addWorkflowAction('rejected', id => this.DocumentWorkflowService.showRequestRejected(id), {
      iconName: 'mdi-comment-remove-outline',
    });

    this._addWorkflowAction('cancel', id => this.DocumentWorkflowService.cancelRequest(id), {
      iconName: 'mdi-comment-processing-outline',
      translationKeyFunction: this._getRequestTranslationKey,
    });

    this._addWorkflowAction('accept', id => this.DocumentWorkflowService.acceptRequest(id), {
      iconName: 'mdi-check',
      translationKeyFunction: this._getRequestTranslationKey,
    });

    this._addWorkflowAction('reject', id => this.DocumentWorkflowService.rejectRequest(id), {
      iconName: 'mdi-close',
      translationKeyFunction: this._getRequestTranslationKey,
    });

    this._menu.addDivider({
      isVisible: () => this.PageService.hasSomeAction('xpage',
        'unpublish',
        'schedule-unpublish',
        'request-unpublish',
        'publish',
        'schedule-publish',
        'request-publish',
        'request-schedule-publish'),
    });

    this._addWorkflowAction('unpublish', id => this.DocumentWorkflowService.unpublish(id), {
      iconName: 'mdi-minus-circle',
    });

    this._addWorkflowAction('schedule-unpublish', id => this.DocumentWorkflowService.scheduleUnpublication(id), {
      isEnabled: () => this._isEnabled('schedule-unpublish') && !this._isEditingCurrentPage(),
    });

    this._addWorkflowAction('request-unpublish', id => this.DocumentWorkflowService.requestUnpublication(id), {
      iconName: 'mdi-minus-circle',
      isEnabled: () => this._isEnabled('request-unpublish') && !this._isEditingCurrentPage(),
    });

    this._addWorkflowAction(
      'request-schedule-unpublish',
      id => this.DocumentWorkflowService.requestScheduleUnpublication(id),
      { isEnabled: () => this._isEnabled('request-schedule-unpublish') && !this._isEditingCurrentPage() },
    );

    this._addWorkflowAction('publish', id => this.DocumentWorkflowService.publish(id), {
      iconName: 'mdi-check-circle',
    });

    this._addWorkflowAction('schedule-publish', id => this.DocumentWorkflowService.schedulePublication(id), {
      isEnabled: () => this._isEnabled('schedule-publish') && !this._isEditingCurrentPage(),
    });

    this._addWorkflowAction(
      'request-publish',
      id => this.DocumentWorkflowService.requestPublication(id),
      {
        iconName: 'mdi-check-circle',
        isEnabled: () => this._isEnabled('request-publish') && !this._isEditingCurrentPage(),
      },
    );

    this._addWorkflowAction(
      'request-schedule-publish',
      id => this.DocumentWorkflowService.requestSchedulePublication(id),
      { isEnabled: () => this._isEnabled('request-schedule-publish') && !this._isEditingCurrentPage() },
    );

    this._menu.addDivider({
      isVisible: () => this.PageService.hasSomeAction('xpage',
        'copy',
        'move',
        'delete'),
    });

    this._addWorkflowAction(
      'move',
      id => this.DocumentWorkflowService.move(id).then(documentId => this._navigateToDocument(documentId)),
    );

    this._addWorkflowAction(
      'copy',
      id => this.DocumentWorkflowService.copy(id).then(documentId => this._navigateToDocument(documentId)),
      { iconName: 'mdi-content-copy' },
    );

    this._addWorkflowAction(
      'delete',
      id => this.DocumentWorkflowService.delete(id)
        .then(() => this.SiteMapService.load(this.ChannelService.getSiteMapId()))
        .then(() => {
          const siteMap = this.SiteMapService.get();
          const firstPage = siteMap[0];
          return this.HippoIframeService.load(firstPage.renderPathInfo);
        }),
      { iconName: 'mdi-delete' },
    );
  }

  _addWorkflowAction(id, onClick, config = {}) {
    const translationKey = `TOOLBAR_MENU_XPAGE_${id.replace(/-/g, '_').toUpperCase()}`;
    const onFailure = (msg) => {
      const menuItem = this._menu.findAction(id);
      return this._failure((menuItem && menuItem.translationKey) || translationKey, msg);
    };
    this._menu.addAction(id, {
      isEnabled: () => this._isEnabled(id),
      isVisible: () => this._isVisible(id),
      onClick: this._invokeWorkflow.bind(this, onClick, onFailure),
      translationKey,
      ...config,
    });
  }

  async _invokeWorkflow(onClick, onFailure) {
    try {
      await this.EditComponentService.stopEditing();
    } catch (error) {
      return;
    }

    const documentId = this._getDocumentId();
    const isEditing = this.EditContentService.isEditing(documentId);

    try {
      if (isEditing && !this.EditContentService.isEditorPristine()) {
        await this.EditContentService.reloadEditor();
      }
    } catch (error) {
      // The save/discard dialog was cancelled
      return;
    }

    try {
      await onClick(documentId);

      if (isEditing) {
        await this.EditContentService.reloadEditor();
      }

      this.HippoIframeService.reload();
    } catch (message) {
      if (message !== 'CANCELLED') {
        onFailure(message);
      }
    }
  }

  _getRequestTranslationKey(key) {
    const workflow = this.PageService.getState('workflow') || {};
    const { requests = [] } = workflow;
    const request = requests.find(r => r.type !== 'rejected');
    if (request) {
      return `${key}_REQUEST_${request.type.toUpperCase()}`;
    }

    const scheduledRequest = this.PageService.getState('scheduledRequest');
    if (scheduledRequest !== null) {
      return `${key}_SCHEDULED_${scheduledRequest.type.toUpperCase()}`;
    }

    return key;
  }

  _failure(key, reason = 'ERROR_WORKFLOW_GENERIC_REASON') {
    try {
      if (JSON.parse(reason).cancelled === true) {
        return;
      }
      // eslint-disable-next-line no-empty
    } catch (error) {}

    reason = reason.startsWith('ERROR_') ? this.$translate.instant(reason) : reason;
    this.$log.error(`Failed to execute workflow "${key}" on document[${this._getDocumentId()}]: ${reason}`);
    this.FeedbackService.showError(`${key}_ERROR`, {
      reason,
      documentName: this._getDocumentName(),
    });

    this.HippoIframeService.reload();
  }

  _isEnabled(action) {
    return this.PageService.isActionEnabled('xpage', action);
  }

  _isVisible(action) {
    return this.PageService.hasAction('xpage', action);
  }

  _getDocumentId() {
    return this.PageService.getState('xpage').id;
  }

  _getDocumentName() {
    return this.PageService.getState('xpage').name;
  }

  _isEditingCurrentPage() {
    return this.EditContentService.isEditing(this._getDocumentId());
  }

  _showVersions() {
    this.EditContentService.startEditing(this._getDocumentId(), 'hippo-cm.channel.edit-page.versions');
  }

  _showContent() {
    this.EditContentService.startEditing(this._getDocumentId(), 'hippo-cm.channel.edit-page.content');
  }

  async _navigateToDocument(documentId) {
    try {
      const { data: { renderPathInfo } } = await this.HstService.doGet(documentId, 'representation');
      const pageMeta = this.PageStructureService.getPage().getMeta();
      if (pageMeta.getPathInfo() !== renderPathInfo) {
        this.HippoIframeService.load(renderPathInfo);
      }

      const siteMapId = this.ChannelService.getSiteMapId();
      this.SiteMapService.load(siteMapId); // reload sitemap (left side panel)
    } catch (e) {
      this.$log.error(`Failed to navigate to document '${documentId}'`, e);
    }
  }
}
