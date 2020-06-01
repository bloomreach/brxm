/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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
import contentLinkSvg from '../../../../images/html/edit-document.svg?sprite';
import flaskSvg from '../../../../images/html/flask.svg?sprite';
import lockSvg from '../../../../images/html/lock.svg?sprite';
import menuLinkSvg from '../../../../images/html/edit-menu.svg?sprite';
import dropSvg from '../../../../images/html/add.svg?sprite';
import disabledSvg from '../../../../images/html/not-allowed.svg?sprite';
import plusSvg from '../../../../images/html/plus.svg?sprite';
import searchSvg from '../../../../images/html/search.svg?sprite';

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
    HippoIframeService,
    MaskService,
    PageStructureService,
    SvgService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.CreateContentService = CreateContentService;
    this.DomService = DomService;
    this.EditContentService = EditContentService;
    this.ExperimentStateService = ExperimentStateService;
    this.HippoIframeService = HippoIframeService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;
    this.SvgService = SvgService;

    this.editMenuHandler = angular.noop;
    this.selectDocumentHandler = angular.noop;

    this.isComponentsOverlayDisplayed = false;
    this.isContentOverlayDisplayed = false;

    this._translate = (key, params) => $translate.instant(key, params, undefined, false, 'escape');

    PageStructureService.registerChangeListener(() => this.sync());
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  onEditMenu(callback) {
    this.editMenuHandler = callback;
  }

  onSelectDocument(callback) {
    const previousHandler = this.selectDocumentHandler;
    this.selectDocumentHandler = callback;
    return previousHandler;
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
      delete this.iframeWindow;
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
    if (!this.overlay) {
      return;
    }

    const html = $(this.iframeWindow.document.documentElement);
    html.toggleClass('hippo-show-components', this.isComponentsOverlayDisplayed);
    html.toggleClass('hippo-show-content', this.isContentOverlayDisplayed);
    // don't call sync() explicitly: the DOM mutation will trigger it automatically
  }

  selectComponent(componentId) {
    this._selectedComponentId = componentId;
    this.sync();
  }

  deselectComponent() {
    delete this._selectedComponentId;
    this.sync();
  }

  _isSelectedComponentElement(element) {
    return element.type === 'component' && element.metaData.uuid === this._selectedComponentId;
  }

  sync() {
    if (!this.overlay) {
      return;
    }

    const currentOverlayElements = this._getAllStructureElements()
      .reduce((overlayElements, element) => {
        this._syncElement(element);

        return overlayElements.add(element.getOverlayElement()[0]);
      }, new Set());

    this._tidyOverlay(currentOverlayElements);
  }

  clear() {
    if (this.overlay) {
      this.overlay.empty();
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

  _getAllStructureElements() {
    return this.PageStructureService.getContainers().reduce((result, container) => {
      result.push(container, ...container.getComponents());

      return result;
    }, [])
      .concat(this.PageStructureService.getEmbeddedLinks());
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

  _getSvg(svg) {
    return this.SvgService.getSvg(this.iframeWindow, svg);
  }

  _addDropIcon(container, overlayElement) {
    angular.element('<div class="hippo-overlay-icon"></div>')
      .append(this._getSvg(container.isDisabled()
        ? disabledSvg
        : dropSvg))
      .appendTo(overlayElement);
  }

  _addLockIcon(container, overlayElement) {
    if (container.isDisabled()) {
      const lockedBy = this._getLockedByText(container);
      angular.element(`<div class="hippo-overlay-lock" data-locked-by="${lockedBy}"></div>`)
        .append(this._getSvg(lockSvg))
        .appendTo(overlayElement);
    }
  }

  _getLockedByText(container) {
    if (container.isInherited()) {
      return this._translate('CONTAINER_INHERITED');
    }

    return this._translate('CONTAINER_LOCKED_BY', { user: container.getLockedBy() });
  }

  _addLinkMarkup(overlayElement, svg, titleKey, qaClass = '') {
    overlayElement.addClass(`hippo-overlay-element-link hippo-overlay-element-link-button ${qaClass}`);
    overlayElement.attr('title', this._translate(titleKey));
    overlayElement.append(this._getSvg(svg));
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

    const mainButton = buttons[0];
    const optionButtons = buttons.slice(1);

    const mainButtonElement = this._createMainButton(mainButton, config);
    const optionsButtonsElement = $('<div class="hippo-fab-options"></div>');

    overlayElement
      .addClass('hippo-overlay-element-link')
      .append(mainButtonElement)
      .append(optionsButtonsElement);

    const openOptions = () => {
      if (optionsButtonsElement.children().length === 0) {
        optionsButtonsElement.html(this._createOptionButtons(optionButtons));
      }
      const openAbove = this._openOptionsAboveMainButton(structureElement, optionButtons.length);
      optionsButtonsElement.toggleClass('hippo-fab-options-above-main-button', openAbove);
    };

    const closeOptions = () => {
      optionsButtonsElement.empty();
    };

    if (buttons.length > 1) {
      overlayElement.on('mouseenter', openOptions);
      overlayElement.on('mouseleave', closeOptions);
    }
  }

  _initManageContentConfig(structureElement) {
    // each property should be filled with the method that will extract the data from the HST comment
    // Passing the full config through privileges to adjust buttons for authors
    const documentUuid = structureElement.getUuid();
    const parameterName = structureElement.getParameterName();
    const parameterBasePath = structureElement.isParameterValueRelativePath()
      ? this.ChannelService.getChannel().contentRoot
      : '';

    const config = {
      containerItem: structureElement.getEnclosingElement(),
      defaultPath: structureElement.getDefaultPath(),
      documentTemplateQuery: structureElement.getDocumentTemplateQuery(),
      folderTemplateQuery: structureElement.getFolderTemplateQuery(),
      documentUuid,
      parameterBasePath,
      parameterName,
      parameterValue: structureElement.getParameterValue(),
      pickerConfig: structureElement.getPickerConfig(),
      rootPath: structureElement.getRootPath(),
    };

    if (!this.ChannelService.isEditable()) {
      delete config.parameterName;

      if (config.documentUuid) { // whenever uuid is available, only edit button for authors
        delete config.documentTemplateQuery;
        delete config.folderTemplateQuery;
        return config;
      }

      if (parameterName) {
        return {};
      }
    }

    if (parameterName
      && config.containerItem
      && config.containerItem.isLocked()
      && !config.containerItem.isLockedByCurrentUser()) {
      config.isLockedByOtherUser = true;
    }

    if (config.parameterName && !config.containerItem) {
      this.$log.warn(
        `Ignoring component parameter "${config.parameterName}" of manage content button outside catalog item`,
      );
      delete config.parameterName;
    }

    return config;
  }

  _getButtons(config) {
    const buttons = [];

    if (config.documentUuid) {
      const editContentButton = {
        id: 'edit-content',
        mainIcon: contentLinkSvg,
        optionIcon: '', // edit button should never be a option button
        callback: () => this._editContent(config.documentUuid),
        tooltip: this._translate('EDIT_CONTENT'),
      };
      buttons.push(editContentButton);
    }

    if (config.parameterName) {
      const selectDocumentButton = {
        id: 'select-document',
        mainIcon: searchSvg,
        optionIcon: searchSvg,
        callback: () => this._selectDocument(config),
        tooltip: config.isLockedByOtherUser
          ? this._translate('SELECT_DOCUMENT_LOCKED')
          : this._translate('SELECT_DOCUMENT'),
        isDisabled: config.isLockedByOtherUser,
      };
      buttons.push(selectDocumentButton);
    }

    if (config.documentTemplateQuery) {
      const createContentButton = {
        id: 'create-content',
        mainIcon: plusSvg,
        optionIcon: plusSvg,
        callback: () => this._createContent(config),
        tooltip: config.isLockedByOtherUser
          ? this._translate('CREATE_DOCUMENT_LOCKED')
          : this._translate('CREATE_DOCUMENT'),
        isDisabled: config.isLockedByOtherUser,
      };
      buttons.push(createContentButton);
    }

    return buttons;
  }

  _createMainButton(button, manageContentConfig) {
    const mainButton = $(`<button title="${button.tooltip}"></button>`)
      .append(this._getSvg(button.mainIcon));

    mainButton.addClass(`hippo-fab-main hippo-fab-main-${button.id} qa-manage-content-link`);

    if (button.isDisabled) {
      mainButton.addClass('hippo-fab-main-disabled');
    } else {
      this._addClickHandler(mainButton, button.callback);
    }

    if (manageContentConfig.documentUuid) {
      mainButton.addClass('qa-edit-content');
    }
    if (manageContentConfig.documentTemplateQuery) {
      mainButton.addClass('qa-add-content');
    }
    if (manageContentConfig.parameterName) {
      mainButton.addClass('qa-manage-parameters');
    }

    return mainButton;
  }

  _createOptionButtons(buttons) {
    return buttons.map((button, index) => this._createOptionButton(button, index));
  }

  _createOptionButton(button, index) {
    const optionButton = $(`<button title="${button.tooltip}"></button>`)
      .append(this._getSvg(button.optionIcon));

    optionButton.addClass(`hippo-fab-option hippo-fab-option-${button.id} hippo-fab-option-${index}`);

    if (button.isDisabled) {
      optionButton.addClass('hippo-fab-option-disabled');
    } else {
      optionButton.on('click', button.callback);
    }

    return optionButton;
  }

  _openOptionsAboveMainButton(manageContentStructureElement, optionsCount) {
    const boxElement = manageContentStructureElement.prepareBoxElement();
    const element = this._getElementPosition(boxElement);
    const viewport = this._getViewportPosition();

    const optionsHeight = optionsCount * (32 + 5); // 32px button height plus 5px margin between buttons

    const enoughRoomBelow = () => (element.top + element.height + optionsHeight) <= viewport.bottom;
    const enoughRoomAbove = () => (element.top - optionsHeight) >= viewport.top;

    return !enoughRoomBelow() && enoughRoomAbove();
  }

  _getElementPosition(boxElement) {
    const rect = boxElement[0].getBoundingClientRect();
    return {
      top: rect.top + this.iframeWindow.scrollY,
      left: rect.left + this.iframeWindow.scrollX,
      width: rect.width,
      height: rect.height,
    };
  }

  _getViewportPosition() {
    const top = $(this.iframeWindow).scrollTop(); // The position you see at top of scrollbar
    const viewHeight = $(this.iframeWindow).height();
    const bottom = top + viewHeight;

    return {
      top,
      bottom,
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
      this.$rootScope.$evalAsync(() => {
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

  _selectDocument(config) {
    this.selectDocumentHandler(
      config.containerItem,
      config.parameterName,
      config.parameterValue,
      config.pickerConfig,
      config.parameterBasePath,
    );
    this.CmsService.reportUsageStatistic('PickContentButton');
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
        overlayElement.toggleClass('hippo-overlay-element-component-active',
          this._isSelectedComponentElement(structureElement));
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
        labelElement.prepend(this._getSvg(flaskSvg));
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
    const position = this._getElementPosition(boxElement);

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
