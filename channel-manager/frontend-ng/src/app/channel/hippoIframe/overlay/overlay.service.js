/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
import plusSvg from '../../../../images/html/plus.svg';
import plusWhiteSvg from '../../../../images/html/plus-white.svg';
import searchSvg from '../../../../images/html/search.svg';
import searchWhiteSvg from '../../../../images/html/search-white.svg';

class OverlayService {
  constructor(
    $log,
    $rootScope,
    $translate,
    ChannelService,
    CmsService,
    CreateContentService,
    DomService,
    EditContentService,
    ExperimentStateService,
    FeedbackService,
    HippoIframeService,
    HstComponentService,
    MaskService,
    PageStructureService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.CreateContentService = CreateContentService;
    this.DomService = DomService;
    this.EditContentService = EditContentService;
    this.ExperimentStateService = ExperimentStateService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstComponentService = HstComponentService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;

    this.editMenuHandler = angular.noop;
    this.pathPickedHandler = angular.noop;

    this.isComponentsOverlayDisplayed = false;
    this.isContentOverlayDisplayed = false;

    PageStructureService.registerChangeListener(() => this.sync());
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  onEditMenu(callback) {
    this.editMenuHandler = callback;
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

  _getButtons(config) {
    const buttons = [];

    if (config.documentUuid) {
      const editContentButton = {
        mainIcon: contentLinkSvg,
        dialIcon: '', // edit button should never be a dial button
        callback: () => this._editContent(config.documentUuid),
        tooltip: this.$translate.instant('EDIT_CONTENT'),
      };
      buttons.push(editContentButton);
    }

    if (config.componentParameter) {
      const selectDocumentButton = {
        mainIcon: searchWhiteSvg,
        dialIcon: searchSvg,
        callback: () => this._pickPath(config),
        tooltip: this.$translate.instant('SELECT_DOCUMENT'),
      };
      buttons.push(selectDocumentButton);
    }

    if (config.templateQuery) {
      const createContentButton = {
        mainIcon: plusWhiteSvg,
        dialIcon: plusSvg,
        callback: () => this._createContent(config),
        tooltip: this.$translate.instant('CREATE_DOCUMENT'),
      };
      buttons.push(createContentButton);
    }

    return buttons;
  }

  _initManageContentConfig(structureElement) {
    // each property should be filled with the method that will extract the data from the HST comment
    // Passing the full config through privileges to adjust buttons for authors
    const documentUuid = structureElement.getUuid();
    const componentParameter = structureElement.getComponentParameter();
    const componentParameterBasePath =
      structureElement.isComponentParameterRelativePath() ? this.ChannelService.getChannel().contentRoot : '';

    const config = {
      componentParameter,
      componentParameterBasePath,
      componentPickerConfig: structureElement.getComponentPickerConfig(),
      componentValue: structureElement.getComponentValue(),
      containerItem: structureElement.getEnclosingElement(),
      defaultPath: structureElement.getDefaultPath(),
      documentUuid,
      rootPath: structureElement.getRootPath(),
      templateQuery: structureElement.getTemplateQuery(),
    };

    if (!this.ChannelService.isEditable()) {
      delete config.componentParameter;

      if (config.documentUuid) { // whenever uuid is available, only edit button for authors
        delete config.templateQuery;
        return config;
      }

      if (componentParameter) {
        return {};
      }
    }

    if (componentParameter
      && config.containerItem
      && config.containerItem.isLocked()
      && !config.containerItem.isLockedByCurrentUser()) {
      if (!documentUuid) {
        return {};
      }
      delete config.componentParameter;
      delete config.templateQuery;
    }

    if (config.componentParameter && !config.containerItem) {
      this.$log.warn(`Ignoring component parameter "${config.componentParameter}" of manage content button outside catalog item`);
      delete config.componentParameter;
    }

    return config;
  }

  _initManageContentLink(structureElement, overlayElement) {
    const config = this._initManageContentConfig(structureElement);

    if (Object.keys(config).length === 0) {
      // config is empty, no buttons to render
      return;
    }

    const buttons = this._getButtons(config);
    if (buttons.length === 0) {
      // no buttons to render
      return;
    }

    overlayElement
      .addClass('hippo-overlay-element-link hippo-bottom hippo-fab-dial-container')
      .addClass('is-left') // mouse never entered yet
      .append(`<button title="${buttons[0].tooltip}"
                 class="hippo-fab-btn qa-manage-content-link">${buttons[0].mainIcon}</button>`)
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

    this._addClickHandler(fabBtn, () => buttons[0].callback());

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
        optionButtonsContainer.html(this._createButtonsHtml(buttons.slice(1)));
        fabBtn.addClass('hippo-fab-btn-open');
        overlayElement.addClass('is-showing-options');
        return true;
      }
      return false;
    };
    const hideOptions = () => {
      fabBtn.removeClass('hippo-fab-btn-open');
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

    if (buttons.length > 1) {
      overlayElement.on('mouseenter', showOptionsIfLeft);
      overlayElement.on('mouseleave', hideOptionsAndLeave);
    }
  }

  _createButtonsHtml(buttons) {
    return buttons.map((button, index) => this._createButtonTemplate(button, index));
  }

  _createButtonTemplate(button, index) {
    return $(`<button title="${button.tooltip}">${button.dialIcon}</button>`)
      .addClass(`hippo-fab-option-btn hippo-fab-option-${index}`)
      .on('click', button.callback);
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
      this._editContent(structureElement.getUuid());
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

  _addClickHandler(jqueryElement, handler) {
    jqueryElement.click((event) => {
      event.stopPropagation();
      handler();
    });
  }

  _createContent(config) {
    this.CreateContentService.start(config);
  }

  _editContent(uuid) {
    this.EditContentService.startEditing(uuid);
    this.CmsService.reportUsageStatistic('CMSChannelsEditContent');
  }

  _pickPath(config) {
    const component = config.containerItem;
    const componentId = component.getId();
    const componentVariant = component.getRenderVariant();
    const componentName = component.getLabel();
    const parameterName = config.componentParameter;
    const parameterValue = config.componentValue;
    const parameterBasePath = config.componentParameterBasePath;
    const pickerConfig = config.componentPickerConfig;

    this.CmsService.reportUsageStatistic('PickContentButton');
    this.HstComponentService.pickPath(componentId, componentVariant, parameterName, parameterValue, pickerConfig, parameterBasePath)
      .then(() => {
        this.PageStructureService.renderComponent(component.getId());
        this.FeedbackService.showNotification('NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });
      })
      .catch(() => {
        this.FeedbackService.showError('ERROR_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });

        // probably the container got locked by another user, so reload the page to show new locked containers
        this.HippoIframeService.reload();
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
