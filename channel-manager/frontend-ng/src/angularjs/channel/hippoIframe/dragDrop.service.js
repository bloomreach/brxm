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

export class DragDropService {

  constructor($rootScope, $q, DomService, HstService, PageStructureService, ScalingService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$q = $q;
    this.DomService = DomService;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;
    this.ScalingService = ScalingService;

    this.dragging = false;
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
      this.dragging = false;
    }
  }

  enable(containers) {
    if (!this.dragulaPromise) {
      this.dragulaPromise = this._injectDragula(this.iframe);
    }

    return this.dragulaPromise.then(() => {
      const iframeContainerElements = containers.map((container) => container.getJQueryElement('iframe')[0]);

      this.drake = this.iframe.dragula(iframeContainerElements, {
        ignoreInputTextSelection: false,
      });
      this.drake.on('dragend', (el) => this._stopDrag(el));
      this.drake.on('drop', this._onDrop.bind(this));

      this._onComponentClick(containers, (component) => {
        this.dragging = false;
        component.getJQueryElement('iframe').removeClass('qa-dragula-component');
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
        component.getJQueryElement('iframe').on('mouseup', () => callback(component));
      });
    });
  }

  disable() {
    this._destroyDrake();
  }

  startDragOrClick($event, structureElement) {
    this.dragging = true;
    this._dispatchEventInIframe($event, structureElement);
  }

  isDragging() {
    return this.dragging;
  }

  _stopDrag(element) {
    $(element).removeClass('qa-dragula-component');
    this.dragging = false;
    this.$rootScope.$digest();
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
    const iframeElement = structureElement.getJQueryElement('iframe');
    iframeElement[0].dispatchEvent(iframeEvent);
    iframeElement.addClass('qa-dragula-component');
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
