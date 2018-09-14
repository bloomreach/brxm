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

const COMPONENT_QA_CLASS = 'qa-dragula-component';
const MOUSEUP_EVENT_NAME = 'mouseup.dragDropService';
const MOUSELEAVE_EVENT_NAME = 'mouseleave.dragDropService';
const MIRROR_WRAPPER_SELECTOR = '.channel-dragula-mirror';

class DragDropService {
  constructor(
    $q,
    $rootScope,
    BrowserService,
    ChannelService,
    ConfigService,
    DomService,
    EditComponentService,
    PageStructureService,
    ScrollService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.BrowserService = BrowserService;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.EditComponentService = EditComponentService;
    this.PageStructureService = PageStructureService;
    this.ScrollService = ScrollService;
    this.HippoIframeService = HippoIframeService;

    this.draggingOrClicking = false;
    this.dropping = false;
    this.offDrop();

    PageStructureService.registerChangeListener(() => this._sync());
  }

  init(iframeJQueryElement, canvasJQueryElement, sheetJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.canvasJQueryElement = canvasJQueryElement;

    this.ScrollService.init(iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);
    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  onDrop(callback) {
    this.onDropCallback = callback;
  }

  offDrop() {
    this.onDropCallback = () => this.$q.resolve();
  }

  _sync() {
    if (this.drake) {
      this.drake.containers = this._getContainerBoxElements();
    }
  }

  _onLoad() {
    this.iframe = this.iframeJQueryElement[0].contentWindow;
    if (!this.iframe) {
      return;
    }
    try {
      $(this.iframe).one('unload', () => {
        this.ScrollService.disable();
        this._destroyDragula();
      });
    } catch (ignoredException) {
      // IE11: ignore an "access denied" exception when the site returns a 404 page and IE displays its own
      // "friendly HTTP error message" page instead (can be configured in IE and is enabled by default).
      // We cannot access anything on such custom pages.
    }
  }

  _destroyDragula() {
    this._destroyDrake();
    this.dragulaPromise = null;
  }

  _destroyDrake() {
    if (this.drake) {
      if (this.isDragging()) {
        this.drake.cancel(true);
      }
      this.drake.destroy();
      this.drake = null;
      this.dragulaOptions = null;
      this.draggingOrClicking = false;
      this.dropping = false;
    }
  }

  enable() {
    if (!this.dragulaPromise) {
      this.dragulaPromise = this._injectDragula(this.iframe);
    }

    return this.dragulaPromise.then(() => {
      const containerBoxElements = this._getContainerBoxElements();

      this.dragulaOptions = {
        ignoreInputTextSelection: false,
        mirrorContainer: $(MIRROR_WRAPPER_SELECTOR)[0],
        moves: (el, source) => !this.dropping && this._isContainerEnabled(source),
        accepts: (el, target) => this._isContainerEnabled(target),
        invalid: () => !this.draggingOrClicking,
        dragDelay: 300,
      };

      this.drake = this.iframe.dragula(containerBoxElements, this.dragulaOptions);
      this.drake.on('drag', (el, source) => this._onStartDrag(source));
      this.drake.on('cloned', (clone, original) => this._onMirrorCreated(clone, original));
      this.drake.on('over', (el, container) => this._updateDragDirection(container));
      this.drake.on('dragend', el => this._onStopDragOrClick(el));
      this.drake.on('drop', (el, target, source, sibling) => this._onDrop(el, target, source, sibling));
    });
  }

  disable() {
    this.ScrollService.disable();
    this._destroyDrake();
  }

  _injectDragula(iframe) {
    const appRootUrl = this.DomService.getAppRootUrl();
    const dragulaJs = `${appRootUrl}scripts/dragula.min.js?antiCache=${this.ConfigService.antiCache}`;

    if (!this._usesRequireJs(iframe)) {
      return this.DomService.addScript(iframe, dragulaJs);
    }

    const d = this.$q.defer();
    iframe.require([dragulaJs], (dragula) => {
      iframe.dragula = dragula;
      d.resolve();
    });

    return d.promise;
  }

  _usesRequireJs(iframe) {
    return angular.isFunction(iframe.require) && iframe.require === iframe.requirejs;
  }

  _isContainerEnabled(containerElement) {
    const container = this.PageStructureService.getContainerByIframeElement(containerElement);
    return container && !container.isDisabled();
  }

  _getContainerBoxElements() {
    return this.PageStructureService.getContainers()
      .filter(container => !container.isDisabled())
      .map(container => container.getBoxElement()[0]);
  }

  replaceContainer(oldContainer, newContainer) {
    return this.dragulaPromise.then(() => {
      const oldIndex = this.drake.containers.indexOf(oldContainer.getBoxElement()[0]);
      if (oldIndex >= 0 && newContainer) {
        this.drake.containers[oldIndex] = newContainer.getBoxElement()[0];
      }
    });
  }

  startDragOrClick($event, component) {
    this.draggingOrClicking = true;

    this._getIframeHtmlElement().addClass('hippo-overlay-permeable');
    this._dispatchMouseDownInIframe($event, component);

    const componentBoxElement = component.getBoxElement();
    componentBoxElement.on(MOUSEUP_EVENT_NAME, () => this._onComponentClick(component));
    componentBoxElement.on(MOUSELEAVE_EVENT_NAME, () => this._onComponentLeave(component));
    componentBoxElement.addClass(COMPONENT_QA_CLASS);
  }

  _onComponentClick(component) {
    if (!this.isDragging()) {
      this._onStopDragOrClick(component.getBoxElement());

      this.EditComponentService.startEditing(component);
      this._digestIfNeeded();
    }
  }

  _digestIfNeeded() {
    if (!this.$rootScope.$$phase) {
      this.$rootScope.$digest();
    }
  }

  _onComponentLeave(component) {
    if (!this.isDragging()) {
      this._onStopDragOrClick(component.getBoxElement());
    }
  }

  isDraggingOrClicking() {
    return this.draggingOrClicking;
  }

  isDragging() {
    return this.drake && this.drake.dragging;
  }

  _onStartDrag(containerElement) {
    this._getIframeHtmlElement().addClass('hippo-dragging');
    this.canvasJQueryElement.addClass('hippo-dragging');
    this._updateDragDirection(containerElement);

    this.ScrollService.enable();

    // make Angular evaluate isDragging() again
    this._digestIfNeeded();
  }

  _onMirrorCreated(mirrorElement, originalElement) {
    this.DomService.copyComputedStyleExcept(
      originalElement,
      mirrorElement,
      [
        'border-[a-z]*',
        'box-shadow',
        'height',
        'margin-[a-z]*',
        'overflow',
        'opacity',
        'pointer-events',
        'position',
        '[a-z\\-]*user-select',
        'width',
      ],
    );

    this.DomService.copyComputedStyleOfDescendantsExcept(
      originalElement,
      mirrorElement,
      [
        'opacity',
        'pointer-events',
        '[a-z\\-]*user-select',
      ],
    );

    const iframeOffset = this.iframeJQueryElement.offset();
    $(MIRROR_WRAPPER_SELECTOR).offset(iframeOffset);
  }

  _updateDragDirection(containerElement) {
    const container = this.PageStructureService.getContainerByIframeElement(containerElement);
    this.dragulaOptions.direction = container.getDragDirection();
  }

  _onStopDragOrClick(element) {
    this._getIframeHtmlElement().removeClass('hippo-dragging hippo-overlay-permeable');
    this.canvasJQueryElement.removeClass('hippo-dragging');

    this.ScrollService.disable();
    this.draggingOrClicking = false;

    $(element)
      .off(MOUSEUP_EVENT_NAME)
      .off(MOUSELEAVE_EVENT_NAME)
      .removeClass(COMPONENT_QA_CLASS);
  }

  _onDrop(movedElement, targetContainerElement, sourceContainerElement, targetNextComponentElement) {
    this.dropping = true;

    const sourceContainer = this.PageStructureService.getContainerByIframeElement(sourceContainerElement);
    const movedComponent = sourceContainer.getComponentByIframeElement(movedElement);
    const targetContainer = this.PageStructureService.getContainerByIframeElement(targetContainerElement);
    const targetNextComponent = targetContainer.getComponentByIframeElement(targetNextComponentElement);

    this.onDropCallback(movedComponent, targetContainer, targetNextComponent)
      .finally(() => {
        this.dropping = false;
      });
  }

  _dispatchMouseDownInIframe($event, component) {
    const [clientX, clientY] = this._shiftCoordinates($event);
    const iframeMouseDownEvent = this.DomService.createMouseDownEvent(this.iframe, clientX, clientY);
    const iframeElement = component.getBoxElement();
    iframeElement[0].dispatchEvent(iframeMouseDownEvent);
  }

  _getIframeHtmlElement() {
    return this.iframeJQueryElement.contents().find('html');
  }

  _shiftCoordinates($event) {
    const iframeOffset = this.iframeJQueryElement.offset();

    const shiftedX = $event.clientX - iframeOffset.left;
    const shiftedY = $event.clientY - iframeOffset.top;

    return [shiftedX, shiftedY];
  }
}

export default DragDropService;
