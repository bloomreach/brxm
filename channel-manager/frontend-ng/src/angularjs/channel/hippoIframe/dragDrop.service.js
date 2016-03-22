/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class DragDropService {

  constructor($rootScope, $q, DomService, HstService, PageStructureService, ScalingService, ChannelService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$q = $q;
    this.DomService = DomService;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;
    this.ScalingService = ScalingService;
    this.ChannelService = ChannelService;

    this.draggingOrClicking = false;
  }

  init(iframeJQueryElement, baseJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.baseJQueryElement = baseJQueryElement;

    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  _onLoad() {
    this.iframe = this.iframeJQueryElement[0].contentWindow;
    $(this.iframe).one('beforeunload', () => this._destroyDragula());
  }

  _destroyDragula() {
    this._destroyDrake();
    this.dragulaPromise = null;
  }

  _destroyDrake() {
    if (this.drake) {
      this.drake.destroy();
      this.drake = null;
      this.draggingOrClicking = false;
    }
  }

  enable(containers) {
    if (!this.dragulaPromise) {
      this.dragulaPromise = this._injectDragula(this.iframe);
    }

    return this.dragulaPromise.then(() => {
      const iframeContainerElements = containers.map((container) => container.getBoxElement()[0]);

      this.drake = this.iframe.dragula(iframeContainerElements, {
        ignoreInputTextSelection: false,
        mirrorContainer: this.baseJQueryElement[0],
      });
      this.drake.on('drag', () => this._onStartDrag());
      this.drake.on('cloned', (clone, original) => this._onMirrorCreated(clone, original));
      this.drake.on('dragend', (el) => this._onStopDrag(el));
      this.drake.on('drop', (el, target, source, sibling) => this._onDrop(el, target, source, sibling));

      this._onComponentClick(containers, (component) => {
        this._onStopDrag(component.getBoxElement());
        this.PageStructureService.showComponentProperties(component);
      });
    });
  }

  _injectDragula(iframe) {
    const appRootUrl = this.DomService.getAppRootUrl();

    const dragulaCss = `${appRootUrl}styles/dragula.min.css`;
    this.DomService.addCss(iframe, dragulaCss);

    const dragulaJs = `${appRootUrl}scripts/dragula.min.js`;
    return this.DomService.addScript(iframe, dragulaJs);
  }

  _onComponentClick(containers, callback) {
    containers.forEach((container) => {
      container.getComponents().forEach((component) => {
        // Dragula will prevent mouseup events when dragging has not been started,
        // so there's only a mouseup event when the component is clicked.
        component.getBoxElement().on('mouseup', () => callback(component));
      });
    });
  }

  disable() {
    this._destroyDrake();
  }

  startDragOrClick($event, structureElement) {
    this.draggingOrClicking = true;
    this._dispatchEventInIframe($event, structureElement);
  }

  isDraggingOrClicking() {
    return this.draggingOrClicking;
  }

  isDragging() {
    return this.drake && this.drake.dragging;
  }

  _onStartDrag() {
    // make Angular evaluate isDragging() again
    this._digestIfNeeded();
  }

  _onMirrorCreated(mirrorElement, originalElement) {
    this.DomService.copyComputedStyleExcept(originalElement, mirrorElement, ['border-[a-z]*', 'box-shadow', 'margin-[a-z]*', 'overflow', 'opacity', 'pointer-events', 'position', '[a-z\\\-]*user-select']);
    this.DomService.copyComputedStyleOfDescendantsExcept(originalElement, mirrorElement, ['opacity', 'pointer-events', '[a-z\\\-]*user-select']);
  }

  _onStopDrag(element) {
    this.draggingOrClicking = false;
    this._digestIfNeeded();
    $(element).removeClass(COMPONENT_QA_CLASS);
  }

  _digestIfNeeded() {
    if (!this.$rootScope.$$phase) {
      this.$rootScope.$digest();
    }
  }

  _onDrop(movedElement, targetContainerElement, sourceContainerElement, targetNextComponentElement) {
    const sourceContainer = this.PageStructureService.getContainerByIframeElement(sourceContainerElement);
    const movedComponent = sourceContainer.getComponentByIframeElement(movedElement);
    const targetContainer = this.PageStructureService.getContainerByIframeElement(targetContainerElement);
    const targetNextComponent = targetContainer.getComponentByIframeElement(targetNextComponentElement);

    sourceContainer.removeComponent(movedComponent);
    targetContainer.addComponentBefore(movedComponent, targetNextComponent);

    this._updateContainer(sourceContainer);

    if (sourceContainer.getId() !== targetContainer.getId()) {
      this._updateContainer(targetContainer);
    }

    this.ChannelService.recordOwnChange();
  }

  _updateContainer(container) {
    this.HstService.doPost(container.getHstRepresentation(), container.getId(), 'update');
  }

  _dispatchEventInIframe($event, structureElement) {
    const [clientX, clientY] = this.ScalingService.getScaleFactor() === 1.0 ? this._shiftCoordinates($event) : this._shiftAndDescaleCoordinates($event);
    const iframeEvent = new MouseEvent($event.type, {
      view: this.iframe,
      bubbles: true,
      clientX,
      clientY,
    });
    const iframeElement = structureElement.getBoxElement();
    iframeElement[0].dispatchEvent(iframeEvent);
    iframeElement.addClass(COMPONENT_QA_CLASS);
  }

  _shiftCoordinates($event) {
    const iframeOffset = this.iframeJQueryElement.offset();

    const shiftedX = $event.clientX - iframeOffset.left;
    const shiftedY = $event.clientY - iframeOffset.top;

    return [shiftedX, shiftedY];
  }

  _shiftAndDescaleCoordinates($event) {
    const iframeOffset = this.iframeJQueryElement.offset();
    const baseOffset = this.baseJQueryElement.offset();
    const scale = this.ScalingService.getScaleFactor();

    // Shift horizontal using the base offset since the iframe offset is also scaled
    // and hence to far to the right.
    const shiftedX = $event.clientX - baseOffset.left;
    const shiftedY = $event.clientY - iframeOffset.top;

    // The user sees the scaled iframe, but the browser actually uses the unscaled coordinates,
    // so we have to transform the click in the scaled iframe to its 'descaled' position.
    const iframeWidth = this.iframeJQueryElement.width();
    const shiftedAndDescaledX = (shiftedX - (iframeWidth * (1 - scale))) / scale;
    const shiftedAndDescaledY = shiftedY / scale;

    return [shiftedAndDescaledX, shiftedAndDescaledY];
  }

}
