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
import clearSvg from '../../../../images/html/clear.svg';
import plusSvg from '../../../../images/html/plus.svg';
import searchSvg from '../../../../images/html/search.svg';
import addContentSvg from '../../../../images/html/add-content.svg';

const PATH_PICKER_CALLBACK_ID = 'component-path-picker';

class OverlayService {
  constructor(
    $log,
    $rootScope,
    $translate,
    ChannelService,
    CmsService,
    DomService,
    ExperimentStateService,
    FeedbackService,
    HippoIframeService,
    HstService,
    MaskService,
    PageStructureService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.DomService = DomService;
    this.ExperimentStateService = ExperimentStateService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstService = HstService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;

    this.editMenuHandler = angular.noop;
    this.createContentHandler = angular.noop;
    this.editContentHandler = angular.noop;
    this.pathPickedHandler = angular.noop;

    this.isComponentsOverlayDisplayed = false;
    this.isContentOverlayDisplayed = true;

    PageStructureService.registerChangeListener(() => this.sync());
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.iframeJQueryElement.on('load', () => this._onLoad());

    this.CmsService.subscribe('path-picked', (callbackId, path) => {
      if (callbackId === PATH_PICKER_CALLBACK_ID) {
        this.pathPickedHandler(path);
        this.pathPickedHandler = angular.noop;
      }
    });
  }

  onEditMenu(callback) {
    this.editMenuHandler = callback;
  }

  onCreateContent(callback) {
    this.createContentHandler = callback;
  }

  onEditContent(callback) {
    this.editContentHandler = callback;
  }

  pickPath(config) {
    this.pathPickedHandler = path => this.onPathPicked(config, path);
    this.CmsService.publish(
      'show-path-picker',
      PATH_PICKER_CALLBACK_ID,
      config.componentValue,
      config.componentPickerConfig);
  }

  onPathPicked(config, path) {
    if (!config.containerItem) {
      this.FeedbackService.showError('ERROR_SET_COMPONENT_PARAMETER_NO_CONTAINER_ITEM');
      return;
    }

    path = path.startsWith('/') ? path : `/${path}`;
    if (config.componentPickerConfig.isRelativePath) {
      path = this._pathRelativeToChannelRoot(path);
    }

    const component = config.containerItem;
    this.HstService.doPutForm({ document: path }, component.getId(), 'hippo-default')
      .then(() => this.HippoIframeService.reload())
      .catch(() => this.FeedbackService.showError('ERROR_SET_COMPONENT_PARAMETER_PATH', { path }));
  }

  _pathRelativeToChannelRoot(path) {
    const channel = this.ChannelService.getChannel();
    path = path.substring(channel.contentRoot.length);
    return path.startsWith('/') ? path.substring(1) : path;
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
    this._updateOverlayClasses();

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

  showComponentsOverlay(isDisplayed) {
    this.isComponentsOverlayDisplayed = isDisplayed;
    this._updateOverlayClasses();
  }

  showContentOverlay(isDisplayed) {
    this.isContentOverlayDisplayed = isDisplayed;
    this._updateOverlayClasses();
  }

  _updateOverlayClasses() {
    if (this.iframeWindow) {
      const html = $(this.iframeWindow.document.documentElement);
      html.toggleClass('hippo-show-components', this.isComponentsOverlayDisplayed);
      html.toggleClass('hippo-show-content', this.isContentOverlayDisplayed);
      // don't call sync() explicitly: the DOM mutation will trigger it automatically
    }
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
      case 'manage-content-link':
        this._initManageContentLink(structureElement, overlayElement);
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
    overlayElement.addClass(`hippo-overlay-element-link hippo-overlay-element-link-button ${qaClass}`);
    overlayElement.attr('title', this.$translate.instant(titleKey));
    overlayElement.append(svg);
  }

  _getDialOptions(config) {
    // The order of the properties in variable optionButtons defines
    // the order in which they will be rendered in the dial widget.
    const optionButtons = {
      templateQuery: {
        svg: plusSvg,
        callback: () => {
          this.$rootScope.$apply(() => {
            this.createContentHandler(config);
          });
        },
        tooltip: this.$translate.instant('CREATE_DOCUMENT'),
      },
      componentParameter: {
        svg: searchSvg,
        callback: () => this.pickPath(config),
        tooltip: this.$translate.instant('SELECT_DOCUMENT'),
      },
    };

    const optionsSet = Object.assign({ buttons: [] },
      config.documentUuid ? this._getMainEditButtonConfig() : this._getMainCreateButtonConfig());

    Object.keys(optionButtons)
      .filter(key => config[key])
      .forEach(key => optionsSet.buttons.push(optionButtons[key]));

    return optionsSet;
  }

  _getMainCreateButtonConfig() {
    return {
      mainButtonIcon: addContentSvg,
      mainButtonTooltip: this.$translate.instant('CREATE_DOCUMENT'),
      mainButtonCloseIcon: clearSvg,
      mainButtonCloseTooltip: this.$translate.instant('CANCEL'),
    };
  }

  _getMainEditButtonConfig() {
    return {
      mainButtonIcon: contentLinkSvg,
      mainButtonTooltip: this.$translate.instant('EDIT_CONTENT'),
      mainButtonCloseIcon: contentLinkSvg,
      mainButtonCloseTooltip: this.$translate.instant('EDIT_CONTENT'),
    };
  }

  filterConfigByPrivileges(configObj) {
    if (this.ChannelService.isEditable()) {
      return configObj;
    }

    const config = angular.copy(configObj);
    delete config.componentParameter;
    if (configObj.documentUuid) { // whenever uuid is available, only edit button for authors
      delete config.templateQuery;
      return config;
    }
    if (!configObj.documentUuid && configObj.componentParameter) {
      return {};
    }

    return config; // when uuid doesn't exist, only templateQuery (create content option) is returned
  }

  _initManageContentLink(structureElement, overlayElement) {
    // each property should be filled with the method that will extract the data from the HST comment
    // Passing the full config through privileges to adjust buttons for authors
    const config = this.filterConfigByPrivileges({
      componentParameter: structureElement.getComponentParameter(),
      componentPickerConfig: structureElement.getComponentPickerConfig(),
      componentValue: structureElement.getComponentValue(),
      defaultPath: structureElement.getDefaultPath(),
      documentUuid: structureElement.getUuid(),
      rootPath: structureElement.getRootPath(),
      templateQuery: structureElement.getTemplateQuery(),
      containerItem: structureElement.getEnclosingElement(),
    });

    // if the config is empty, create no button
    if (angular.equals(config, {})) return;

    const optionsSet = this._getDialOptions(config);

    overlayElement
      .addClass('hippo-overlay-element-link hippo-bottom hippo-fab-dial-container')
      .addClass('is-left') // mouse never entered yet
      .append(`<button title="${optionsSet.mainButtonTooltip}"
                 class="hippo-fab-btn qa-manage-content-link">${optionsSet.mainButtonIcon}</button>`)
      .append('<div class="hippo-fab-dial-options"></div>');

    const fabBtn = overlayElement.find('.hippo-fab-btn');
    const optionButtonsContainer = overlayElement.find('.hippo-fab-dial-options');

    if (config.documentUuid) {
      fabBtn.addClass('qa-edit-content');
    }
    if (config.templateQuery) {
      fabBtn.addClass('qa-add-content');
    }
    if (config.componentParameter) {
      fabBtn.addClass('qa-manage-parameters');
    }

    const adjustOptionsPosition = () => {
      const boxElement = structureElement.prepareBoxElement();
      const position = this._getElementPositionObject(boxElement);

      if (position.scrollTop > (position.top - 80)) {
        overlayElement.addClass('hippo-bottom').removeClass('hippo-top');
      } else if (position.scrollBottom < (position.top + 130)) {
        overlayElement.addClass('hippo-top').removeClass('hippo-bottom');
      } else {
        overlayElement.addClass('hippo-bottom').removeClass('hippo-top');
      }
    };

    const showOptions = () => {
      adjustOptionsPosition();
      if (!overlayElement.hasClass('is-showing-options')) {
        optionButtonsContainer.html(this._createButtonsHtml(optionsSet.buttons));
        fabBtn.addClass('hippo-fab-btn-open');
        fabBtn.html(optionsSet.mainButtonCloseIcon);
        fabBtn.attr('title', optionsSet.mainButtonCloseTooltip);
        overlayElement.addClass('is-showing-options');
        return true;
      }
      return false;
    };
    const hideOptions = () => {
      fabBtn.removeClass('hippo-fab-btn-open');
      fabBtn.html(optionsSet.mainButtonIcon);
      fabBtn.attr('title', optionsSet.mainButtonTooltip);
      overlayElement.removeClass('is-showing-options');
    };
    const showOptionsIfLeft = () => {
      if (overlayElement.hasClass('is-left')) {
        showOptions();
      }
      overlayElement.removeClass('is-left');
    };
    const hideOptionsAndLeave = () => {
      hideOptions();
      overlayElement.addClass('is-left');
    };
    const fabButtonCallback = this.fabButtonCallback(config, optionsSet);
    if (fabButtonCallback) {
      fabBtn.on('click', () => fabButtonCallback(config.documentUuid));
    } else {
      overlayElement.on('click', () => showOptions() || hideOptions());
    }

    if (this.isHoverEnabled(config)) {
      overlayElement.on('mouseenter', showOptionsIfLeft);
      overlayElement.on('mouseleave', hideOptionsAndLeave);
    }
  }

  _createButtonsHtml(buttons) {
    return buttons.map((button, index) => this._createButtonTemplate(button, index));
  }

  _createButtonTemplate(button, index) {
    return $(`<button title="${button.tooltip}">${button.svg}</button>`)
      .addClass(`hippo-fab-option-btn hippo-fab-option-${index}`)
      .on('click', button.callback);
  }

  fabButtonCallback(config, optionsSet) {
    if (config.documentUuid) {
      return this.editContentHandler;
    }
    if (config.templateQuery && !config.componentParameter) {
      return optionsSet.buttons[0].callback;
    }
    return null;
  }

  isHoverEnabled(config) {
    if (config.documentUuid) {
      return config.templateQuery || config.componentParameter;
    }
    return config.templateQuery && config.componentParameter;
  }

  _getElementPositionObject(boxElement) {
    const rect = boxElement[0].getBoundingClientRect();

    let top = rect.top;
    let left = rect.left;
    const width = rect.width;
    const height = rect.height;

    // Include scroll position since coordinates are relative to page but rect is relative to viewport.
    // IE11 does not support window.scrollX and window.scrollY, so use window.pageXOffset and window.pageYOffset
    left += this.iframeWindow.pageXOffset;
    top += this.iframeWindow.pageYOffset;

    const scrollTop = $(this.iframeWindow).scrollTop(); // The position you see at top of scrollbar
    const viewHeight = $(this.iframeWindow).height();
    const scrollBottom = viewHeight + scrollTop;

    return {
      top,
      left,
      width,
      height,
      scrollTop,
      scrollBottom,
      viewHeight,
    };
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
        return this.isComponentsOverlayDisplayed && !this.isInAddMode;
      case 'container':
        return this.isComponentsOverlayDisplayed;
      case 'content-link':
        return this.isContentOverlayDisplayed && this.DomService.isVisible(boxElement);
      case 'manage-content-link':
        return this.isContentOverlayDisplayed && this.DomService.isVisible(boxElement);
      case 'menu-link':
        return this.isComponentsOverlayDisplayed && !this.isInAddMode && this.DomService.isVisible(boxElement);
      default:
        return this.isComponentsOverlayDisplayed && !this.isInAddMode;
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
    const position = this._getElementPositionObject(boxElement);

    overlayElement.css('top', `${position.top}px`);
    overlayElement.css('left', `${position.left}px`);
    overlayElement.css('width', `${position.width}px`);
    overlayElement.css('height', `${position.height}px`);
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
