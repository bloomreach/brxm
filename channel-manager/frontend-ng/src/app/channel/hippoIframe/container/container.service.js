/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

const COMPONENT_MOVED_EVENT_NAME = 'component-moved';

class ContainerService {
  constructor(
    $log,
    $q,
    $rootScope,
    $translate,
    CmsService,
    DialogService,
    DragDropService,
    EditComponentService,
    Emittery,
    FeedbackService,
    HippoIframeService,
    PageStructureService,
    SpaService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DialogService = DialogService;
    this.DragDropService = DragDropService;
    this.EditComponentService = EditComponentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageStructureService = PageStructureService;
    this.SpaService = SpaService;

    this.emitter = new Emittery();
  }

  onComponentMoved(callback) {
    return this._on(COMPONENT_MOVED_EVENT_NAME, callback);
  }

  _on(eventName, callback) {
    return this.emitter.on(eventName, argument => this.$rootScope.$apply(() => callback(argument)));
  }

  async addComponent(catalogComponent, container) {
    try {
      const newComponentId = await this.PageStructureService.addComponentToContainer(catalogComponent, container);
      if (!this._reloadSpa()) {
        await this._renderNewComponent(newComponentId, container);
      }

      return newComponentId;
    } catch (error) {
      this.FeedbackService.showError('ERROR_ADD_COMPONENT', {
        component: catalogComponent.label,
      });

      throw error;
    }
  }

  _renderNewComponent(componentId, container) {
    return this.PageStructureService.renderNewComponentInContainer(componentId, container);
  }

  _reloadSpa() {
    if (!this.SpaService.detectedSpa()) {
      return false;
    }

    // we don't provide fine-grained reloading of containers ATM, so reload the whole SPA instead
    this.HippoIframeService.reload();

    return true;
  }

  moveComponent(component, targetContainer, targetContainerNextComponent) {
    return this.PageStructureService.moveComponent(component, targetContainer, targetContainerNextComponent)
      .then(changedContainers => this._reloadSpa() || this._renderContainers(changedContainers))
      .then(() => this.emitter.emit(COMPONENT_MOVED_EVENT_NAME, component));
  }

  _renderContainers(changedContainers) {
    return this.$q.all(changedContainers.map(container => this._renderContainer(container)));
  }

  _renderContainer(container) {
    return this.PageStructureService.renderContainer(container)
      .then(newContainer => this.DragDropService.replaceContainer(container, newContainer));
  }

  deleteComponent(componentId) {
    const component = this.PageStructureService.getComponentById(componentId);
    if (!component) {
      this.$log.warn(`Cannot delete unknown component with id '${componentId}'`);
      return;
    }
    this._confirmDelete(component).then(
      this._doDelete(componentId),
      () => this.EditComponentService.startEditing(component),
    );
  }

  _confirmDelete(selectedComponent) {
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_COMPONENT_MESSAGE', {
        component: selectedComponent.getLabel(),
      }))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _doDelete(componentId) {
    return () => this.PageStructureService.removeComponentById(componentId)
      .then(container => this._reloadSpa() || this._renderContainer(container))
      .finally(() => this.CmsService.publish('destroy-component-properties-window'));
  }
}

export default ContainerService;
