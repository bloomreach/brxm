/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import * as HstConstants from '../../../model/constants';
import { HeadContributions } from '../../../model/entities';

class PageStructureService {
  constructor(
    $log,
    $q,
    $rootScope,
    ChannelService,
    CommunicationService,
    FeedbackService,
    HstComponentService,
    HstService,
    MarkupService,
    ModelFactoryService,
    ProjectService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.CommunicationService = CommunicationService;
    this.FeedbackService = FeedbackService;
    this.HstComponentService = HstComponentService;
    this.HstService = HstService;
    this.MarkupService = MarkupService;
    this.ModelFactoryService = ModelFactoryService;
    this.ProjectService = ProjectService;

    this.headContributions = new Set();

    this.ModelFactoryService
      .register(HstConstants.TYPE_PROCESSED_HEAD_CONTRIBUTIONS, this._createHeadContributions.bind(this))
      .register(HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS, this._createHeadContributions.bind(this));
  }

  _createHeadContributions(json) {
    const headContributions = new HeadContributions(json);

    headContributions.getElements()
      .forEach(item => this.headContributions.add(item));

    return headContributions;
  }

  async parseElements(initial) {
    this._clear();

    await this.CommunicationService.ready();
    const comments = await this.CommunicationService.parseElements();

    this._page = this.ModelFactoryService.createPage(comments);
    this._notifyChangeListeners({ initial });
    this._updateChannel();
  }

  _updateChannel() {
    const channelIdFromService = this.ChannelService.getId();
    const channelIdFromPage = this._page && this._page.getMeta().getChannelId();

    if (!channelIdFromPage) {
      this.$log.info('There is no channel detected in the page metadata.');

      return;
    }

    if (channelIdFromService === channelIdFromPage) {
      return;
    }

    const contextPathFromPage = this._page.getMeta().getContextPath();
    const hostGroupFromPreviousChannel = this.ChannelService.getHostGroup();

    // Current channel is a branch, but new channel has no branch of that project
    // therefore load master
    const branchId = this.ProjectService.isBranch() && !this.ProjectService.hasBranchOfProject(channelIdFromPage)
      ? this.ProjectService.masterId
      : undefined;

    this.ChannelService.initializeChannel(
      channelIdFromPage,
      contextPathFromPage,
      hostGroupFromPreviousChannel,
      branchId,
    );
  }

  _clear() {
    this.headContributions.clear();
    delete this._page;
  }

  _notifyChangeListeners(data) {
    this.$rootScope.$emit('page:change', data);
  }

  getPage() {
    return this._page;
  }

  /**
   * Remove the component identified by given Id
   * @param componentId
   * @return the container of the removed component
   */
  removeComponentById(componentId) {
    const component = this._page && this._page.getComponentById(componentId);

    if (!component) {
      this.$log.debug(
        `Could not remove component with ID '${componentId}' because it does not exist in the page structure.`,
      );
      return this.$q.reject();
    }

    const oldContainer = component.getContainer();
    return this.HstComponentService.deleteComponent(oldContainer.getId(), componentId)
      .then(() => this.ChannelService.checkChanges().then(() => oldContainer),
        (errorResponse) => {
          const errorKey = errorResponse.error === 'ITEM_ALREADY_LOCKED'
            ? 'ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED'
            : 'ERROR_DELETE_COMPONENT';
          const params = errorResponse.parameterMap;
          params.component = component.getLabel();
          this.FeedbackService.showError(errorKey, params);

          return this.$q.reject();
        });
  }

  /**
   * Renders a component in the current page.
   * @param component     The component
   * @param propertiesMap Optional: the parameter names and values to use for rendering.
   *                      When omitted the persisted names and values are used.
   */
  async renderComponent(component, propertiesMap = {}) {
    if (!component) {
      return;
    }

    try {
      const { data: newMarkup } = await this.MarkupService.fetchComponentMarkup(component, propertiesMap);
      // re-fetch component because a parallel renderComponent call may have updated the component's markup
      const oldComponent = this._page && this._page.getComponentById(component.getId());
      const oldHeadContributionsSize = this.headContributions.size;
      const comments = await this.CommunicationService.updateComponent(component.getId(), newMarkup);
      const container = oldComponent.getContainer();
      let newComponent;

      try {
        newComponent = this.ModelFactoryService.createComponent(comments);
      } catch (e) {
        this.$log.error(e.message);
      }

      if (!newComponent) {
        container.removeComponent(oldComponent);
        this._notifyChangeListeners();

        return;
      }

      newComponent.setContainer(container);
      container.replaceComponent(oldComponent, newComponent);
      this._notifyChangeListeners();

      if ($.isEmptyObject(propertiesMap) && oldHeadContributionsSize !== this.headContributions.size) {
        this.$rootScope.$emit('page:new-head-contributions', newComponent);
      }
    } catch (response) {
      if (response.status === 404) {
        this.FeedbackService.showDismissible('FEEDBACK_NOT_FOUND_MESSAGE');
        throw new Error();
      }
    }
  }

  async renderContainer(container) {
    const markup = await this.MarkupService.fetchContainerMarkup(container);
    const oldHeadContributionsSize = this.headContributions.size;
    const comments = await this.CommunicationService.updateContainer(container.getId(), markup);
    let newContainer;

    try {
      newContainer = this.ModelFactoryService.createContainer(comments);
    } catch (e) {
      this.$log.error(e.message);
    }

    this._page.replaceContainer(container, newContainer);
    this._notifyChangeListeners();

    if (oldHeadContributionsSize !== this.headContributions.size) {
      this.$rootScope.$emit('page:new-head-contributions', newContainer);
    }

    return newContainer;
  }

  addComponentToContainer(catalogComponent, container, nextComponentId) {
    return this.HstService.addHstComponent(catalogComponent, container, nextComponentId)
      .then(
        (response) => {
          this.ChannelService.checkChanges();
          return {
            reloadRequired: response.reloadRequired,
            newComponentId: response.data.id,
          };
        },
        (errorResponse) => {
          const errorKey = errorResponse.data.error === 'ITEM_ALREADY_LOCKED'
            ? 'ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED'
            : 'ERROR_ADD_COMPONENT';
          const params = errorResponse.data.parameterMap;
          params.component = catalogComponent.name;
          this.FeedbackService.showError(errorKey, params);

          return this.$q.reject(errorResponse.message);
        },
      );
  }

  moveComponent(component, targetContainer, nextComponent) {
    // first update the page structure so the component is already 'moved' in the client-side state
    const sourceContainer = component.getContainer();
    sourceContainer.removeComponent(component);
    targetContainer.addComponentBefore(component, nextComponent);

    const changedContainers = [sourceContainer];
    if (sourceContainer.getId() !== targetContainer.getId()) {
      changedContainers.push(targetContainer);
    }

    // next, push the updated container representation(s) to the backend
    const backendCallPromises = changedContainers.map(container => this._storeContainer(container));

    // last, record a channel change. The caller is responsible for re-rendering the changed container(s)
    // so their meta-data is updated and we're sure they look right
    return this.$q.all(backendCallPromises)
      .then((responses) => {
        this.ChannelService.checkChanges();
        return {
          reloadRequired: responses.some(response => response.reloadRequired),
          changedContainers,
        };
      })
      .catch(() => this.FeedbackService.showError('ERROR_MOVE_COMPONENT_FAILED', {
        component: component.getLabel(),
      }));
  }

  _storeContainer(container) {
    return this.HstService.updateHstContainer(container.getId(), container.getHstRepresentation());
  }
}

export default PageStructureService;
