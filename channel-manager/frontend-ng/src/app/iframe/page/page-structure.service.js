/*
 * Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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

import { EndMarker, HeadContributions, PageMeta } from '../../model/entities';
import * as HstConstants from '../../model/constants';
import {
  Component,
  Container,
  ManageContentLink,
  MenuLink,
} from './entities';

export default class PageStructureService {
  constructor(
    $document,
    $q,
    $rootScope,
    CommunicationService,
    HstCommentsProcessorService,
    ModelFactoryService,
  ) {
    'ngInject';

    this.$document = $document;
    this.$q = $q;
    this.$rootScope = $rootScope;

    this.CommunicationService = CommunicationService;
    this.HstCommentsProcessorService = HstCommentsProcessorService;
    this.ModelFactoryService = ModelFactoryService;

    this.embeddedLinks = [];

    this.ModelFactoryService
      .transform(({ json }) => json)
      .register(HstConstants.TYPE_COMPONENT, this._createComponent.bind(this))
      .register(HstConstants.TYPE_CONTAINER, this._createContainer.bind(this))
      .register(HstConstants.TYPE_EDIT_MENU_LINK, this._createMenuLink.bind(this))
      .register(HstConstants.TYPE_MANAGE_CONTENT_LINK, this._createManageContentLink.bind(this))
      .register(HstConstants.TYPE_PROCESSED_HEAD_CONTRIBUTIONS, ({ json }) => new HeadContributions(json))
      .register(HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS, ({ json }) => new HeadContributions(json))
      .register(HstConstants.END_MARKER, ({ json }) => new EndMarker(json))
      .register(HstConstants.TYPE_PAGE_META, ({ json }) => new PageMeta(json));

    this._emitSyncOverlay = this._emitSyncOverlay.bind(this);
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

  _createMenuLink({ element: startComment, json }) {
    const menuLink = new MenuLink(json);

    menuLink.setStartComment(angular.element(startComment));
    menuLink.setEndComment(angular.element(startComment));
    this.embeddedLinks.push(menuLink);

    return menuLink;
  }

  _createManageContentLink({ element: startComment, json }) {
    const manageContentLink = new ManageContentLink(json);

    manageContentLink.setStartComment(angular.element(startComment));
    manageContentLink.setEndComment(angular.element(startComment));
    this.embeddedLinks.push(manageContentLink);

    return manageContentLink;
  }

  parseElements() {
    this._clear();
    const comments = Array.from(this.HstCommentsProcessorService.run(this.$document[0]));

    this._page = this.ModelFactoryService.createPage(comments);
    this._notifyChangeListeners();

    return comments.map(({ json }) => json);
  }

  _clear() {
    this.embeddedLinks.splice(0);
    delete this._page;
  }

  _notifyChangeListeners() {
    this.$rootScope.$emit('page:change');
  }

  getEmbeddedLinks() {
    return this.embeddedLinks;
  }

  getPage() {
    return this._page;
  }

  getComponentByOverlayElement(overlayElement) {
    if (!this._page) {
      return;
    }

    // eslint-disable-next-line consistent-return
    return this._page.getContainers()
      .map(container => container.getComponents())
      .flat()
      .filter(component => component.hasOverlayElement())
      .find(component => component.getOverlayElement().is(overlayElement)
        || angular.element.contains(component.getOverlayElement()[0], overlayElement));
  }

  getContainerByOverlayElement(overlayElement) {
    if (!this._page) {
      return;
    }

    const containers = this._page.getContainers();

    // eslint-disable-next-line consistent-return
    return containers
      .filter(container => container.hasOverlayElement())
      .find(container => container.getOverlayElement().is(overlayElement));
  }

  getContainerByIframeElement(containerIFrameElement) {
    if (!this._page) {
      return;
    }

    // eslint-disable-next-line consistent-return
    return this._page.getContainers()
      .find(container => container.getBoxElement().is(containerIFrameElement));
  }

  /**
   * Update the component with the new markup
   */
  async updateComponent(componentId, newMarkup) {
    const oldComponent = this._page && this._page.getComponentById(componentId);
    if (!oldComponent) {
      return [];
    }

    const container = oldComponent.getContainer();
    const $newMarkup = $(newMarkup);

    this._removeEmbeddedLinksInComponent(oldComponent);
    await this.$q(resolve => oldComponent.replaceDOM($newMarkup, resolve, this._emitSyncOverlay));

    const comments = Array.from(this.HstCommentsProcessorService.processFragment($newMarkup));
    let newComponent;

    try {
      newComponent = this.ModelFactoryService.createComponent(comments);
      // eslint-disable-next-line no-empty
    } catch (e) {}

    if (newComponent) {
      newComponent.setContainer(container);
      container.replaceComponent(oldComponent, newComponent);
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newComponent.setOverlayElement(oldComponent.getOverlayElement());
    } else {
      container.removeComponent(oldComponent);
    }

    this._notifyChangeListeners();

    return comments.map(({ json }) => json);
  }

  async updateContainer(containerId, newMarkup) {
    const oldContainer = this._page && this._page.getContainerById(containerId);
    if (!oldContainer) {
      return [];
    }

    const $newMarkup = $(newMarkup);

    this._removeEmbeddedLinksInContainer(oldContainer);
    await this.$q(resolve => oldContainer.replaceDOM($newMarkup, resolve, this._emitSyncOverlay));

    const comments = Array.from(this.HstCommentsProcessorService.processFragment($newMarkup));
    let container;

    try {
      container = this.ModelFactoryService.createContainer(comments);
      // eslint-disable-next-line no-empty
    } catch (e) {}

    this._page.replaceContainer(oldContainer, container);
    this._notifyChangeListeners();

    return comments.map(({ json }) => json);
  }

  _removeEmbeddedLinksInContainer(container) {
    container.getComponents().forEach(component => this._removeEmbeddedLinksInComponent(component));
    this._removeEmbeddedLinksInComponent(container);
  }

  _removeEmbeddedLinksInComponent(component) {
    component.getLinks()
      .forEach((link) => {
        const index = this.embeddedLinks.indexOf(link);
        if (index > -1) {
          this.embeddedLinks.splice(index, 1);
        }
      });
  }

  moveComponent(component, targetContainer, targetContainerNextComponent) {
    // first update the page structure so the component is already 'moved' in the client-side state
    const sourceContainer = component.getContainer();
    sourceContainer.removeComponent(component);
    targetContainer.addComponentBefore(component, targetContainerNextComponent);

    this.CommunicationService.emit('component:move', {
      componentId: component.getId(),
      containerId: targetContainer.getId(),
      nextComponentId: targetContainerNextComponent && targetContainerNextComponent.getId(),
    });
  }

  _emitSyncOverlay() {
    this.$rootScope.$emit('overlay:sync');
  }
}
