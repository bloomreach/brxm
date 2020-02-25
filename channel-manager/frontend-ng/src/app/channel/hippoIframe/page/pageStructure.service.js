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

import { EndMarker, HeadContributions, PageMeta } from '../../../model/entities';
import * as HstConstants from '../../../model/constants';
import {
  Component,
  Container,
  ManageContentLink,
  MenuLink,
} from './entities';

class PageStructureService {
  constructor(
    $log,
    $q,
    $rootScope,
    ChannelService,
    FeedbackService,
    HstCommentsProcessorService,
    HstComponentService,
    HstService,
    MarkupService,
    ModelFactoryService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HstCommentsProcessorService = HstCommentsProcessorService;
    this.HstComponentService = HstComponentService;
    this.HstService = HstService;
    this.MarkupService = MarkupService;
    this.ModelFactoryService = ModelFactoryService;

    this.containers = [];
    this.embeddedLinks = [];
    this.headContributions = new Set();

    this.ModelFactoryService
      .transform(({ json }) => json)
      .register(HstConstants.TYPE_COMPONENT, this._createComponent.bind(this))
      .register(HstConstants.TYPE_CONTAINER, this._createContainer.bind(this))
      .register(HstConstants.TYPE_EDIT_MENU_LINK, this._createMenuLink.bind(this))
      .register(HstConstants.TYPE_MANAGE_CONTENT_LINK, this._createManageContentLink.bind(this))
      .register(HstConstants.TYPE_PROCESSED_HEAD_CONTRIBUTIONS, this._createHeadContributions.bind(this))
      .register(HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS, this._createHeadContributions.bind(this))
      .register(HstConstants.END_MARKER, ({ json }) => new EndMarker(json))
      .register(HstConstants.TYPE_PAGE_META, ({ json }) => new PageMeta(json));
  }

  _createComponent({ element: startComment, json }) {
    const component = new Component(json);
    const [boxElement, endComment] = this.HstCommentsProcessorService.locateComponent(component.getId(), startComment);

    component.setStartComment(angular.element(startComment));
    component.setEndComment(angular.element(endComment));
    component.setBoxElement(angular.element(boxElement));

    return component;
  }

  _createContainer({ element: startComment, json }) {
    const container = new Container(json);
    const [containerElement, endComment] = this.HstCommentsProcessorService.locateComponent(
      container.getId(),
      startComment,
    );
    const boxElement = container.isXTypeNoMarkup() ? startComment.parentNode : containerElement;

    container.setStartComment(angular.element(startComment));
    container.setEndComment(angular.element(endComment));
    container.setBoxElement(angular.element(boxElement));

    return container;
  }

  _createHeadContributions({ json }) {
    const headContributions = new HeadContributions(json);

    headContributions.getElements()
      .forEach(item => this.headContributions.add(item));

    return headContributions;
  }

  _createMenuLink({ element: startComment, json }) {
    const menuLink = new MenuLink(json);

    menuLink.setStartComment(angular.element(startComment));
    menuLink.setEndComment(angular.element(startComment));
    menuLink.prepareBoxElement();
    this.embeddedLinks.push(menuLink);

    return menuLink;
  }

  _createManageContentLink({ element: startComment, json }) {
    const manageContentLink = new ManageContentLink(json);

    manageContentLink.setStartComment(angular.element(startComment));
    manageContentLink.setEndComment(angular.element(startComment));
    manageContentLink.prepareBoxElement();
    this.embeddedLinks.push(manageContentLink);

    return manageContentLink;
  }

  parseElements(document) {
    const comments = Array.from(this.HstCommentsProcessorService.run(document));

    this._page = this.ModelFactoryService.createPage(comments);
    this._notifyChangeListeners();
  }

  clearParsedElements() {
    this.embeddedLinks.splice(0)
      .forEach(element => element.getBoxElement().remove());
    this.containers = [];
    this.headContributions.clear();
    delete this._page;

    this._notifyChangeListeners();
  }

  _notifyChangeListeners() {
    this.$rootScope.$emit('iframe:page:change');
  }

  getEmbeddedLinks() {
    return this.embeddedLinks;
  }

  getComponentById(id) {
    let component = null;
    this.containers.some((container) => {
      component = container.getComponent(id);
      return component;
    });
    return component;
  }

  hasContainer(container) {
    return this.containers.indexOf(container) !== -1;
  }

  getContainers() {
    return this.containers;
  }

  getContainerById(id) {
    return this.containers.find(item => item.getId() === id);
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
    const component = this.getComponentById(componentId);

    if (!component) {
      this.$log.debug(
        `Could not remove component with ID '${componentId}' because it does not exist in the page structure.`,
      );
      return this.$q.reject();
    }

    const oldContainer = component.getContainer();
    return this.HstComponentService.deleteComponent(oldContainer.getId(), componentId)
      .then(() => {
        this._onAfterRemoveComponent();
        return oldContainer;
      },
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

  _onAfterRemoveComponent() {
    this.ChannelService.recordOwnChange();
  }

  getComponentByOverlayElement(componentOverlayElement) {
    let component;
    this.containers.some((container) => {
      component = container.getComponents().find(c => c.getOverlayElement().is(componentOverlayElement));
      return !!component;
    });
    return component;
  }

  getContainerByIframeElement(containerIFrameElement) {
    return this.containers.find(container => container.getBoxElement().is(containerIFrameElement));
  }

  printParsedElements() {
    this.containers.forEach((container, index) => {
      this.$log.debug(`Container ${index}`, container);
      container.items.forEach((component, itemIndex) => {
        this.$log.debug(`  Component ${itemIndex}`, component);
      });
    });
  }

  /**
   * Renders a component in the current page.
   * @param component     The component
   * @param propertiesMap Optional: the parameter names and values to use for rendering.
   *                      When omitted the persisted names and values are used.
   */
  renderComponent(component, propertiesMap = {}) {
    if (component) {
      return this.MarkupService.fetchComponentMarkup(component, propertiesMap)
        .then((response) => {
          // re-fetch component because a parallel renderComponent call may have updated the component's markup
          component = this.getComponentById(component.getId());

          const newMarkup = response.data;
          const oldHeadContributionsSize = this.headContributions.size;
          const updatedComponent = this._updateComponent(component, newMarkup);
          this._notifyChangeListeners();

          if ($.isEmptyObject(propertiesMap) && oldHeadContributionsSize !== this.headContributions.size) {
            this.$rootScope.$emit('hippo-iframe:new-head-contributions', updatedComponent);
          }
        })
        .catch((response) => {
          if (response.status === 404) {
            this.FeedbackService.showDismissible('FEEDBACK_NOT_FOUND_MESSAGE');
            return this.$q.reject();
          }
          return this.$q.resolve();
        });
    }
    return this.$q.resolve();
  }

  /**
   * Update the component with the new markup
   */
  _updateComponent(oldComponent, newMarkup) {
    const $newMarkup = $(newMarkup);
    const container = oldComponent.getContainer();

    this._removeEmbeddedLinksInComponent(oldComponent);

    oldComponent.replaceDOM($newMarkup, () => {
      this._notifyChangeListeners();
    });

    const comments = Array.from(this.HstCommentsProcessorService.processFragment($newMarkup));
    let newComponent;

    try {
      newComponent = this.ModelFactoryService.createComponent(comments);
    } catch (e) {
      this.$log.error(e.message);
    }

    if (newComponent) {
      newComponent.setContainer(container);
      container.replaceComponent(oldComponent, newComponent);
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newComponent.setOverlayElement(oldComponent.getOverlayElement());
    } else {
      container.removeComponent(oldComponent);
    }

    return newComponent;
  }

  async renderContainer(container) {
    const markup = await this.MarkupService.fetchContainerMarkup(container);
    const oldHeadContributionsSize = this.headContributions.size;
    const newContainer = await this._updateContainer(container, markup);

    this._notifyChangeListeners();
    if (oldHeadContributionsSize !== this.headContributions.size) {
      this.$rootScope.$emit('hippo-iframe:new-head-contributions', newContainer);
    }

    return newContainer;
  }

  _updateContainer(oldContainer, newMarkup) {
    this._removeEmbeddedLinksInContainer(oldContainer);
    const $newMarkup = $(newMarkup);

    oldContainer.replaceDOM($newMarkup, () => {
      this._notifyChangeListeners();
    });

    const comments = Array.from(this.HstCommentsProcessorService.processFragment($newMarkup));
    let container;
    try {
      container = this.ModelFactoryService.createContainer(comments);
    } catch (e) {
      this.$log.error(e.message);
    }

    const newContainer = this.replaceContainer(oldContainer, container);

    return newContainer;
  }

  _removeEmbeddedLinksInContainer(container) {
    container.getComponents().forEach(component => this._removeEmbeddedLinksInComponent(component));

    this.embeddedLinks = this._getLinksNotEnclosedInElement(this.embeddedLinks, container);
  }

  _removeEmbeddedLinksInComponent(component) {
    this.embeddedLinks = this._getLinksNotEnclosedInElement(this.embeddedLinks, component);
  }

  _getLinksNotEnclosedInElement(links, component) {
    const remainingContentLinks = [];
    links.forEach((link) => {
      if (link.getComponent() !== component) {
        remainingContentLinks.push(link);
      }
    });
    return remainingContentLinks;
  }

  addComponentToContainer(catalogComponent, container, nextComponentId) {
    return this.HstService.addHstComponent(catalogComponent, container.getId(), nextComponentId)
      .then(
        (newComponentJson) => {
          this.ChannelService.recordOwnChange();
          // TODO: handle error when rendering container failed
          return newComponentJson.id;
        },
        (errorResponse) => {
          const errorKey = errorResponse.error === 'ITEM_ALREADY_LOCKED'
            ? 'ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED'
            : 'ERROR_ADD_COMPONENT';
          const params = errorResponse.parameterMap;
          params.component = catalogComponent.name;
          this.FeedbackService.showError(errorKey, params);

          return this.$q.reject();
        },
      );
  }

  moveComponent(component, targetContainer, targetContainerNextComponent) {
    // first update the page structure so the component is already 'moved' in the client-side state
    const sourceContainer = component.getContainer();
    sourceContainer.removeComponent(component);
    targetContainer.addComponentBefore(component, targetContainerNextComponent);

    const changedContainers = [sourceContainer];
    if (sourceContainer.getId() !== targetContainer.getId()) {
      changedContainers.push(targetContainer);
    }

    // next, push the updated container representation(s) to the backend
    const backendCallPromises = changedContainers.map(container => this._storeContainer(container));

    // last, record a channel change. The caller is responsible for re-rendering the changed container(s)
    // so their meta-data is updated and we're sure they look right
    return this.$q.all(backendCallPromises)
      .then(() => this.ChannelService.recordOwnChange())
      .then(() => changedContainers)
      .catch(() => this.FeedbackService.showError('ERROR_MOVE_COMPONENT_FAILED', {
        component: component.getLabel(),
      }));
  }

  _storeContainer(container) {
    return this.HstService.updateHstContainer(container.getId(), container.getHstRepresentation());
  }

  getContainerByOverlayElement(overlayElement) {
    return this.containers.find((container) => {
      const containerOverlay = container.getOverlayElement();
      return containerOverlay && (containerOverlay[0] === overlayElement);
    });
  }

  _replaceContainer(oldContainer, newContainer) {
    const index = this.containers.indexOf(oldContainer);
    if (index === -1) {
      this.$log.warn('Cannot find container', oldContainer);
      return null;
    }
    if (newContainer) {
      this.containers[index] = newContainer;
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newContainer.setOverlayElement(oldContainer.getOverlayElement());
    } else {
      this.containers.splice(index, 1);
    }
    return newContainer;
  }
}

export default PageStructureService;
