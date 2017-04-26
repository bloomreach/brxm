/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import MutationSummary from 'mutation-summary';
import contentLinkSvg from '../../../../images/html/edit-document.svg';
import flaskSvg from '../../../../images/html/flask.svg';
import lockSvg from '../../../../images/html/lock.svg';
import menuLinkSvg from '../../../../images/html/edit-menu.svg';
import dropSvg from '../../../../images/html/add.svg';
import disabledSvg from '../../../../images/html/not-allowed.svg';

class OverlayService {
  constructor(
    $log,
    $rootScope,
    $translate,
    CmsService,
    DomService,
    ExperimentStateService,
    HippoIframeService,
    MaskService,
    PageStructureService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DomService = DomService;
    this.ExperimentStateService = ExperimentStateService;
    this.HippoIframeService = HippoIframeService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;

    this.editMenuHandler = angular.noop;
    this.editContentHandler = angular.noop;
    this.overlays = {};

    PageStructureService.registerChangeListener(() => this.sync());
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  onEditMenu(callback) {
    this.editMenuHandler = callback;
  }

  onEditContent(callback) {
    this.editContentHandler = callback;
  }

  _onLoad() {
    this.iframeWindow = this.iframeJQueryElement[0].contentWindow;

    this._initOverlay();

    this.observer = new MutationSummary({
      callback: () => this.sync(),
      rootNode: this.iframeWindow.document,
      queries: [{ all: true }],
    });

    const win = $(this.iframeWindow);
    win.on('unload', () => this._onUnload());
    win.on('resize', () => this.sync());
  }

  _onUnload() {
    this.$rootScope.$apply(() => {
      this.observer.disconnect();
      delete this.overlay;
    });
  }

  _initOverlay() {
    this.overlay = $('<div class="hippo-overlay"></div>');
    $(this.iframeWindow.document.body).append(this.overlay);
    this._updateModeClass();

    this.overlay.mousedown((event) => {
      // let right-click trigger context-menu instead of starting dragging
      event.preventDefault();

      // we already dispatch a mousedown event on the same location, so don't propagate this one to avoid that
      // dragula receives one mousedown event too many
      event.stopPropagation();

      this._onOverlayMouseDown(event);
    });
  }

  enableAddMode() {
    this.isInAddMode = true;
    this.overlay.addClass('hippo-overlay-add-mode');
    this.overlay.on('click', () => {
      this.$rootScope.$apply(() => {
        this._resetMask();
      });
    });
  }

  _resetMask() {
    this.disableAddMode();
    this.offContainerClick();
    this.MaskService.unmask();
    this.MaskService.removeClickHandler();
    this.HippoIframeService.lowerIframeBeneathMask();
  }

  disableAddMode() {
    this.isInAddMode = false;
    this.overlay.removeClass('hippo-overlay-add-mode');
    this.overlay.off('click');
  }

  setComponentsOverlay(state) {
    this.overlays.components = state;
    this._updateModeClass();
  }

  setContentOverlay(state) {
    this.overlays.content = state;
    console.log('set content overlay');
  }

  _updateModeClass() {
    if (this.iframeWindow) {
      const html = $(this.iframeWindow.document.documentElement);
      console.log(html);
      html.toggleClass('hippo-mode-edit', this.overlays.components);
      // don't call sync() explicitly: the DOM mutation will trigger it automatically
    }
  }

  _isEditMode() {
    return this.mode === 'edit';
  }

  sync() {
    if (this.overlay) {
      const currentOverlayElements = new Set();

      this._forAllStructureElements((structureElement) => {
        this._syncElement(structureElement);

        const overlayElement = structureElement.getOverlayElement()[0];
        currentOverlayElements.add(overlayElement);
      });

      this._tidyOverlay(currentOverlayElements);
    }
  }

  attachComponentMouseDown(callback) {
    this.componentMouseDownCallback = callback;
  }

  detachComponentMouseDown() {
    this.componentMouseDownCallback = null;
  }

  _onOverlayMouseDown(event) {
    const target = $(event.target);
    if (target.hasClass('hippo-overlay-element-component') && this.componentMouseDownCallback) {
      const component = this.PageStructureService.getComponentByOverlayElement(target);
      if (component) {
        this.componentMouseDownCallback(event, component);
      }
    }
  }

  _forAllStructureElements(callback) {
    this.PageStructureService.getContainers().forEach((container) => {
      callback(container);
      container.getComponents().forEach(callback);
    });
    this.PageStructureService.getEmbeddedLinks().forEach(callback);
  }

  _syncElement(structureElement) {
    const overlayElement = structureElement.getOverlayElement();

    if (overlayElement) {
      this._syncElements(structureElement, overlayElement);
    } else {
      this._addOverlayElement(structureElement);
    }
  }

  _addOverlayElement(structureElement) {
    const overlayElement = $(`
      <div class="hippo-overlay-element hippo-overlay-element-${structureElement.getType()}">
      </div>`);

    this._addLabel(structureElement, overlayElement);
    this._addMarkupAndBehavior(structureElement, overlayElement);

    structureElement.setOverlayElement(overlayElement);

    this._syncElements(structureElement, overlayElement);

    this.overlay.append(overlayElement);
  }

  onContainerClick(clickHandler) {
    this.PageStructureService.getContainers().forEach((container) => {
      const element = container.getOverlayElement();
      element.on('click', (event) => {
        clickHandler(event, container);
      });
    });
  }

  offContainerClick() {
    this.PageStructureService.getContainers().forEach((container) => {
      const element = container.getOverlayElement();
      element.off('click');
    });
  }

  _addLabel(structureElement, overlayElement) {
    if (structureElement.hasLabel()) {
      const labelElement = $(`
        <span class="hippo-overlay-label">
          <span class="hippo-overlay-label-text"></span>
        </span>
      `);

      const label = structureElement.getLabel();
      const escapedLabel = this._setLabelText(labelElement, label);
      labelElement.attr('data-qa-name', escapedLabel);

      overlayElement.append(labelElement);
    }
  }

  _setLabelText(labelElement, text) {
    const textElement = labelElement.children('.hippo-overlay-label-text');
    const escapedText = this.DomService.escapeHtml(text);
    // use html() with manual escaping instead of text() because the latter crashes Chrome during unit tests :-/
    textElement.html(escapedText);
    return escapedText;
  }

  _addMarkupAndBehavior(structureElement, overlayElement) {
    switch (structureElement.getType()) {
      case 'container':
        this._addDropIcon(structureElement, overlayElement);
        this._addLockIcon(structureElement, overlayElement);
        break;
      case 'content-link':
        this._addLinkMarkup(overlayElement, contentLinkSvg, 'EDIT_CONTENT', 'qa-content-link');
        this._addContentLinkClickHandler(structureElement, overlayElement);
        break;
      case 'menu-link':
        this._addLinkMarkup(overlayElement, menuLinkSvg, 'EDIT_MENU', 'qa-menu-link');
        this._addMenuLinkClickHandler(structureElement, overlayElement);
        break;
      default:
        break;
    }
  }

  _addDropIcon(container, overlayElement) {
    const iconMarkup = $('<div class="hippo-overlay-icon"></div>');

    if (container.isDisabled()) {
      iconMarkup.append(disabledSvg);
    } else {
      iconMarkup.append(dropSvg);
    }

    iconMarkup.appendTo(overlayElement);
  }

  _addLockIcon(container, overlayElement) {
    if (container.isDisabled()) {
      const lockedBy = this._getLockedByText(container);
      const lockMarkup = $(`<div class="hippo-overlay-lock" data-locked-by="${lockedBy}"></div>`);
      lockMarkup.append(lockSvg);
      lockMarkup.appendTo(overlayElement);
    }
  }

  _getLockedByText(container) {
    if (container.isInherited()) {
      return this.$translate.instant('CONTAINER_INHERITED');
    }
    const escapedLockedBy = this.DomService.escapeHtml(container.getLockedBy());
    return this.$translate.instant('CONTAINER_LOCKED_BY', { user: escapedLockedBy });
  }

  _addLinkMarkup(overlayElement, svg, titleKey, qaClass = '') {
    overlayElement.addClass(`hippo-overlay-element-link ${qaClass}`);
    overlayElement.attr('title', this.$translate.instant(titleKey));
    overlayElement.append(svg);
  }

  _addContentLinkClickHandler(structureElement, overlayElement) {
    this._linkButtonTransition(overlayElement);

    this._addClickHandler(overlayElement, () => {
      this.$rootScope.$apply(() => {
        this.editContentHandler(structureElement.getUuid());
      });
    });
  }

  _addMenuLinkClickHandler(structureElement, overlayElement) {
    this._linkButtonTransition(overlayElement);

    this._addClickHandler(overlayElement, () => {
      this.$rootScope.$apply(() => {
        this.editMenuHandler(structureElement.getUuid());
      });
    });
  }

  _addClickHandler(overlayElement, handler) {
    overlayElement.click((event) => {
      event.stopPropagation();
      handler();
    });
  }

  _linkButtonTransition(element) {
    element.on('mousedown', () => {
      element.addClass('hippo-overlay-element-link-clicked');
    });

    element.on('mouseup', () => {
      element.removeClass('hippo-overlay-element-link-clicked');
    });
  }

  _syncElements(structureElement, overlayElement) {
    const boxElement = structureElement.prepareBoxElement();
    boxElement.addClass('hippo-overlay-box');

    overlayElement.toggleClass('hippo-overlay-element-visible', this._isElementVisible(structureElement, boxElement));

    switch (structureElement.getType()) {
      case 'component':
        this._syncLabel(structureElement, overlayElement);
        break;
      case 'container': {
        const isEmptyInDom = structureElement.isEmptyInDom();
        boxElement.toggleClass('hippo-overlay-box-container-filled', !isEmptyInDom);
        overlayElement.toggleClass('hippo-overlay-element-container-empty', isEmptyInDom);
        overlayElement.toggleClass('hippo-overlay-element-container-disabled', structureElement.isDisabled());
        break;
      }
      default:
        break;
    }

    this._syncPosition(overlayElement, boxElement);
  }

  _isElementVisible(structureElement, boxElement) {
    switch (structureElement.getType()) {
      case 'component':
        return this.overlays.component && !this.isInAddMode;
      case 'container':
        return this.overlays.component;
      case 'content-link':
        return !this.overlays.component && this.DomService.isVisible(boxElement);
      case 'menu-link':
        return this.overlays.component && !this.isInAddMode && this.DomService.isVisible(boxElement);
      default:
        return this.overlays.component && !this.isInAddMode;
    }
  }

  _syncLabel(component, overlayElement) {
    const labelElement = overlayElement.children('.hippo-overlay-label');
    const iconElement = labelElement.children('svg');

    if (this.ExperimentStateService.hasExperiment(component)) {
      labelElement.addClass('hippo-overlay-label-experiment');

      const experimentId = this.ExperimentStateService.getExperimentId(component);
      labelElement.attr('data-qa-experiment-id', experimentId);

      if (iconElement.length === 0) {
        labelElement.prepend(flaskSvg);
      }

      const experimentState = this.ExperimentStateService.getExperimentStateLabel(component);
      this._setLabelText(labelElement, experimentState);
    } else {
      labelElement.removeClass('hippo-overlay-label-experiment');
      labelElement.removeAttr('data-qa-experiment-id');
      iconElement.remove();
      this._setLabelText(labelElement, component.getLabel());
    }
  }

  _syncPosition(overlayElement, boxElement) {
    const rect = boxElement[0].getBoundingClientRect();

    let top = rect.top;
    let left = rect.left;
    const width = rect.width;
    const height = rect.height;

    // Include scroll position since coordinates are relative to page but rect is relative to viewport.
    // IE11 does not support window.scrollX and window.scrollY, so use window.pageXOffset and window.pageYOffset
    left += this.iframeWindow.pageXOffset;
    top += this.iframeWindow.pageYOffset;

    overlayElement.css('top', `${top}px`);
    overlayElement.css('left', `${left}px`);
    overlayElement.css('width', `${width}px`);
    overlayElement.css('height', `${height}px`);
  }

  _tidyOverlay(elementsToKeep) {
    const overlayElements = this.overlay.children();

    // to improve performance, only iterate when there are elements to remove
    if (overlayElements.length > elementsToKeep.size) {
      overlayElements.each((index, element) => {
        if (!elementsToKeep.has(element)) {
          $(element).remove();
        }
      });
    }
  }
}

export default OverlayService;
