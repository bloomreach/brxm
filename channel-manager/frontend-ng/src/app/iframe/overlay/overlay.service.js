/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import { waitForImagesToLoad } from '../utils/dom.utils';

import contentLinkSvg from '../../../images/html/edit-document.svg?sprite';
import flaskSvg from '../../../images/html/flask.svg?sprite';
import lockSvg from '../../../images/html/lock.svg?sprite';
import menuLinkSvg from '../../../images/html/edit-menu.svg?sprite';
import dropSvg from '../../../images/html/add.svg?sprite';
import disabledSvg from '../../../images/html/not-allowed.svg?sprite';
import plusSvg from '../../../images/html/plus.svg?sprite';
import searchSvg from '../../../images/html/search.svg?sprite';
import iframeCss from '../../../styles/string/hippo-iframe.scss?url';
import directionalDropSvg from '../../../images/html/directional-drop.svg?sprite';

export default class OverlayService {
  constructor(
    $document,
    $log,
    $q,
    $rootScope,
    $translate,
    $window,
    CommunicationService,
    DragDropService,
    DomService,
    PageStructureService,
    SvgService,
  ) {
    'ngInject';

    this.$document = $document;
    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$window = $window;
    this.CommunicationService = CommunicationService;
    this.DragDropService = DragDropService;
    this.DomService = DomService;
    this.PageStructureService = PageStructureService;
    this.SvgService = SvgService;

    this.isComponentsOverlayDisplayed = false;
    this.isContentOverlayDisplayed = false;
    this._isEditSharedContainers = false;

    this._onOverlayMouseDown = this._onOverlayMouseDown.bind(this);
    this._onOverlayClick = this._onOverlayClick.bind(this);
    this._onPageChange = this._onPageChange.bind(this);
    this._translate = (key, params) => $translate.instant(key, params, undefined, false, 'escape');
  }

  async initialize() {
    this.$rootScope.$on('overlay:sync', () => this.sync());
    this.$rootScope.$on('page:change', this._onPageChange);

    await this.$q(this.$document.ready);

    this._observer = new MutationSummary({
      callback: () => this.sync(),
      rootNode: this.$document[0],
      queries: [{ all: true }],
    });
    this.$window.addEventListener('resize', () => this.sync());
  }

  async _onPageChange() {
    if (!this._cssPromise) {
      const url = await this.CommunicationService.getAssetUrl(iframeCss);
      this._cssPromise = this.DomService.addCssLinks(this.$window, [url]);
    }

    this._isEditable = await this.CommunicationService.isEditable();
    this._isEditSharedContainers = await this.CommunicationService.isEditSharedContainers();

    await this._cssPromise;

    if (!this._overlay || !this.$document.find('body > .hippo-overlay').length) {
      this._overlay = angular.element('<div>', { class: 'hippo-overlay' })
        .on('click', this._onOverlayClick)
        .on('mousedown', this._onOverlayMouseDown)
        .appendTo(this.$document.find('body'));
      this._updateOverlayClasses();
    }

    const images = this.$document.find('img, [type="image"]');
    await waitForImagesToLoad(images, () => this.sync(), () => this.sync());
  }

  _onOverlayClick(event) {
    if (!this.isInAddMode) {
      return;
    }

    const component = this.PageStructureService.getComponentByOverlayElement(event.target);
    if (component) {
      // eslint-disable-next-line consistent-return
      return this._onComponentClick(event, component);
    }

    const container = this.PageStructureService.getContainerByOverlayElement(event.target);
    if (container) {
      // eslint-disable-next-line consistent-return
      return this._onContainerClick(event, container);
    }

    this.toggleAddMode(false);
  }

  _onComponentClick(event, component) {
    const container = component.getContainer();
    if (container.isDisabled()) {
      return;
    }

    const components = container.getComponents();
    const componentIndex = components.findIndex(item => item.getId() === component.getId());
    const shouldPlaceBefore = event.target.classList.contains('hippo-overlay-element-component-drop-area-before');
    const nextComponent = shouldPlaceBefore
      ? components[componentIndex]
      : components[componentIndex + 1];

    this._addModeDeferred.resolve({
      container: container.getId(),
      nextComponent: nextComponent && nextComponent.getId(),
    });
    delete this._addModeDeferred;

    this.toggleAddMode(false);
  }

  _onContainerClick(event, container) {
    if (container.isDisabled()) {
      return;
    }

    if (container.isShared() !== this._isEditSharedContainers) {
      return;
    }

    this._addModeDeferred.resolve({ container: container.getId() });
    delete this._addModeDeferred;

    this.toggleAddMode(false);
  }

  _onOverlayMouseDown(event) {
    // let right-click trigger context-menu instead of starting dragging
    event.preventDefault();

    // we already dispatch a mousedown event on the same location, so don't propagate this one to avoid that
    // dragula receives one mousedown event too many
    event.stopPropagation();

    const target = angular.element(event.target);
    if (!target.hasClass('hippo-overlay-element-component')
      || !this.isComponentsOverlayDisplayed
      || !this.DragDropService.isEnabled()
    ) {
      return;
    }

    const component = this.PageStructureService.getComponentByOverlayElement(target);
    if (component) {
      this.DragDropService.startDragOrClick(event, component);
    }
  }

  async toggleAddMode(value) {
    this.isInAddMode = !!value;
    this._overlay.toggleClass('hippo-overlay-add-mode', value);

    if (this._addModeDeferred) {
      this._addModeDeferred.reject();
      delete this._addModeDeferred;
    }

    if (!value) {
      return;
    }

    this._addModeDeferred = this.$q.defer();

    // eslint-disable-next-line consistent-return
    return this._addModeDeferred.promise;
  }

  toggleComponentsOverlay(value) {
    this.isComponentsOverlayDisplayed = value;
    this._updateOverlayClasses();

    if (value) {
      this.DragDropService.enable();
    } else {
      this.DragDropService.disable();
    }
  }

  toggleContentsOverlay(value) {
    this.isContentOverlayDisplayed = value;
    this._updateOverlayClasses();
  }

  _updateOverlayClasses() {
    if (!this._overlay) {
      return;
    }

    const html = angular.element(this.$document[0].documentElement);
    html.toggleClass('hippo-show-components', this.isComponentsOverlayDisplayed);
    html.toggleClass('hippo-show-content', this.isContentOverlayDisplayed);
    // don't call sync() explicitly: the DOM mutation will trigger it automatically
  }

  selectComponent(componentId) {
    this._selectedComponentId = componentId;
    this.sync();
  }

  _isSelectedComponentElement(element) {
    return element.getType() === 'component' && element.getId() === this._selectedComponentId;
  }

  sync() {
    if (!this._overlay) {
      return;
    }

    const overlays = new Set();

    this._getAllStructureElements()
      .forEach((element) => {
        this._syncElement(element);

        overlays.add(element.getOverlayElement()[0]);
        if (element.hasGeneratedBoxElement()) {
          overlays.add(element.getBoxElement()[0]);
        }
      });

    if (this._overlays) {
      this._overlays.forEach(element => element && !overlays.has(element) && element.remove());
    }

    this._overlays = overlays;
  }

  _getAllStructureElements() {
    const links = this.PageStructureService.getEmbeddedLinks();
    const page = this.PageStructureService.getPage();

    if (!page) {
      return [...links];
    }

    return page.getContainers().reduce((result, container) => {
      result.push(container, ...container.getComponents());

      return result;
    }, [])
      .concat(links);
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
    const overlayElement = angular.element(`
      <div class="hippo-overlay-element hippo-overlay-element-${structureElement.getType()}">
      </div>`);

    this._addLabel(structureElement, overlayElement);
    this._addMarkupAndBehavior(structureElement, overlayElement);

    structureElement.setOverlayElement(overlayElement);

    this._syncElements(structureElement, overlayElement);

    this._overlay.append(overlayElement);
  }

  _addLabel(structureElement, overlayElement) {
    if (structureElement.hasLabel()) {
      const labelElement = angular.element(`
        <span class="hippo-overlay-label">
          <span class="hippo-overlay-label-text"></span>
        </span>
      `);

      const label = structureElement.getLabel();
      this._setLabelText(labelElement, structureElement.isShared()
        ? `${label} ${this._translate('SHARED_SUFFIX')}`
        : label);
      labelElement.attr('data-qa-name', structureElement.isShared() ? `${label}-shared` : label);

      overlayElement.append(labelElement);
    }
  }

  _setLabelText(labelElement, text) {
    const textElement = labelElement.children('.hippo-overlay-label-text');
    if (textElement.text() !== text) {
      textElement.text(text);
    }
  }

  _addMarkupAndBehavior(structureElement, overlayElement) {
    switch (structureElement.getType()) {
      case 'container':
        this._addContainerMarkup(structureElement, overlayElement);
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
      case 'component':
        this._addComponentMarkup(structureElement, overlayElement);
        break;
      default:
        break;
    }
  }

  _addContainerMarkup(structureElement, overlayElement) {
    this._addDropIcon(structureElement, overlayElement);
    this._addLockIcon(structureElement, overlayElement);
    this._addEditSharedContainersButtons(structureElement, overlayElement);
  }

  _addComponentMarkup(structureElement, overlayElement) {
    if (structureElement.getContainer().isDisabled()) {
      return;
    }

    const dropAreaBefore = this._createDropArea('before', structureElement);
    const dropAreaAfter = this._createDropArea('after', structureElement);
    const direction = structureElement.getContainer().getDragDirection();

    angular.element('<div>')
      .addClass('hippo-overlay-element-component-drop-area')
      .addClass(`hippo-overlay-element-component-direction-${direction}`)
      .append(dropAreaBefore)
      .append(dropAreaAfter)
      .appendTo(overlayElement);
  }

  _createDropArea(placement, structureElement) {
    return angular.element('<div>')
      .addClass(`hippo-overlay-element-component-drop-area-${placement}`)
      .append(this._createComponentDropIcons(structureElement.container));
  }

  _createComponentDropIcons(container) {
    return angular.element('<div>')
      .addClass('hippo-overlay-element-component-drop-area-icons')
      .append(this.SvgService.getSvg(container.isDisabled()
        ? disabledSvg
        : directionalDropSvg));
  }

  _addDropIcon(container, overlayElement) {
    angular.element('<div class="hippo-overlay-icon"></div>')
      .append(this.SvgService.getSvg(container.isDisabled()
        ? disabledSvg
        : dropSvg))
      .appendTo(overlayElement);
  }

  _addLockIcon(container, overlayElement) {
    if (container.isDisabled()) {
      const lockedBy = this._getLockedByText(container);
      angular.element(`<div class="hippo-overlay-lock" data-locked-by="${lockedBy}"></div>`)
        .append(this.SvgService.getSvg(lockSvg))
        .appendTo(overlayElement);
    }
  }

  _getLockedByText(container) {
    if (container.isInherited()) {
      return this._translate('CONTAINER_INHERITED');
    }

    return this._translate('CONTAINER_LOCKED_BY', { user: container.getLockedBy() });
  }

  _addEditSharedContainersButtons(container, overlayElement) {
    const editSharedContainer = angular.element('<div>')
      .addClass('hippo-overlay-shared')
      .appendTo(overlayElement);

    if (container.isEmpty()) {
      editSharedContainer.append(this._createEditSharedContainersButton(container));
    } else {
      const top = angular.element('<div>')
        .addClass('hippo-overlay-shared-top')
        .append(this._createEditSharedContainersButton(container));

      const bottom = angular.element('<div>')
        .addClass('hippo-overlay-shared-bottom')
        .append(this._createEditSharedContainersButton(container));

      editSharedContainer
        .append(top)
        .append(bottom);
    }
  }

  _createEditSharedContainersButton(container) {
    const title = this._translate(container.isShared() ? 'TOGGLE_SHARED_CONTAINERS' : 'TOGGLE_PAGE_CONTAINERS');
    return angular.element(`<button class="hippo-overlay-shared-button" title="${title}">${title}</button>`)
      .addClass(container.isShared() ? 'qa-toggle-shared-containers-button' : 'qa-toggle-page-containers-button')
      .on('click', () => {
        this._isEditSharedContainers = !this._isEditSharedContainers;
        this.CommunicationService.emit('page:edit-shared-containers', this._isEditSharedContainers);
        this.sync();
      });
  }

  _addLinkMarkup(overlayElement, svg, titleKey, qaClass = '') {
    overlayElement.addClass(`hippo-overlay-element-link hippo-overlay-element-link-button ${qaClass}`);
    overlayElement.attr('title', this._translate(titleKey));
    overlayElement.append(this.SvgService.getSvg(svg));
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
    const optionsButtonsElement = angular.element('<div class="hippo-fab-options"></div>');

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
    const parameterName = structureElement.getParameterName();
    const containerItem = structureElement.getComponent();

    const config = {
      containerItemId: containerItem && containerItem.getId(),
      defaultPath: structureElement.getDefaultPath(),
      documentTemplateQuery: structureElement.getDocumentTemplateQuery(),
      folderTemplateQuery: structureElement.getFolderTemplateQuery(),
      documentUuid: structureElement.getId(),
      isParameterValueRelativePath: structureElement.isParameterValueRelativePath(),
      parameterName,
      parameterValue: structureElement.getParameterValue(),
      pickerConfig: structureElement.getPickerConfig(),
      rootPath: structureElement.getRootPath(),
    };

    if (!this._isEditable) {
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
      && containerItem
      && containerItem.isLocked()
      && !containerItem.isLockedByCurrentUser()) {
      config.isLockedByOtherUser = true;
    }

    if (config.parameterName && !containerItem) {
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
    const mainButton = angular.element(`<button title="${button.tooltip}"></button>`)
      .append(this.SvgService.getSvg(button.mainIcon));

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
    const optionButton = angular.element(`<button title="${button.tooltip}"></button>`)
      .append(this.SvgService.getSvg(button.optionIcon));

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
      top: rect.top + this.$window.scrollY,
      left: rect.left + this.$window.scrollX,
      width: rect.width,
      height: rect.height,
    };
  }

  _getViewportPosition() {
    const { scrollY: top, innerHeight } = this.$window;
    const bottom = top + innerHeight;

    return { top, bottom };
  }

  _addContentLinkClickHandler(structureElement, overlayElement) {
    this._linkButtonTransition(overlayElement);

    this._addClickHandler(overlayElement, () => {
      this._editContent(structureElement.getId());
    });
  }

  _addMenuLinkClickHandler(structureElement, overlayElement) {
    this._linkButtonTransition(overlayElement);

    this._addClickHandler(
      overlayElement,
      () => this.CommunicationService.emit('menu:edit', structureElement.getId()),
    );
  }

  _addClickHandler(jqueryElement, handler) {
    jqueryElement.click((event) => {
      event.stopPropagation();
      handler();
    });
  }

  _createContent(config) {
    this.CommunicationService.emit('document:create', config);
  }

  _editContent(uuid) {
    this.CommunicationService.emit('document:edit', uuid);
  }

  _selectDocument(config) {
    this.CommunicationService.emit('document:select', config);
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
        overlayElement.toggleClass('hippo-overlay-element-container-readonly', structureElement.isShared()
          ? !this._isEditSharedContainers
          : this._isEditSharedContainers);
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
        if (!this.isComponentsOverlayDisplayed) {
          return false;
        }
        return structureElement.isShared()
          ? this._isEditSharedContainers
          : !this._isEditSharedContainers;
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

    if (component.hasExperiment()) {
      labelElement.addClass('hippo-overlay-label-experiment');

      const experimentId = component.getExperimentId();
      labelElement.attr('data-qa-experiment-id', experimentId);

      if (iconElement.length === 0) {
        labelElement.prepend(this.SvgService.getSvg(flaskSvg));
      }

      const experimentState = component.getExperimentStateLabel();
      this._setLabelText(labelElement, experimentState && this._translate(experimentState));
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
}
