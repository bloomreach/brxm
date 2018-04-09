/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class ContainerService {
  constructor(
    $log,
    $translate,
    CmsService,
    DialogService,
    DragDropService,
    FeedbackService,
    HippoIframeService,
    PageStructureService,
    SpaService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DialogService = DialogService;
    this.DragDropService = DragDropService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageStructureService = PageStructureService;
    this.SpaService = SpaService;
  }

  addComponent(catalogComponent, containerOverlayElement) {
    const container = this.PageStructureService.getContainerByOverlayElement(containerOverlayElement);

    this.PageStructureService.addComponentToContainer(catalogComponent, container)
      .then((newComponentId) => {
        if (this.SpaService.detectedSpa()) {
          // we don't provide fine-grained reloading of containers ATM, so reload the whole SPA instead
          this.HippoIframeService.reload();
          return true;
        }
        return this.PageStructureService.renderNewComponentInContainer(newComponentId, container);
      })
      .catch(() => {
        this.FeedbackService.showError('ERROR_ADD_COMPONENT', {
          component: catalogComponent.label,
        });
      });
  }

  deleteComponent(componentId) {
    const component = this.PageStructureService.getComponentById(componentId);
    if (!component) {
      this.$log.warn(`Cannot delete unknown component with id '${componentId}'`);
      return;
    }
    this._confirmDelete(component).then(
      this._doDelete(componentId),
      () => this.PageStructureService.showComponentProperties(component),
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
      .then((container) => {
        if (this.SpaService.detectedSpa()) {
          // we don't provide fine-grained reloading of containers ATM, so reload the whole SPA instead
          this.HippoIframeService.reload();
          return true;
        }
        return this.PageStructureService.updateContainer(container)
          .then(({ oldContainer, newContainer }) => this.DragDropService.replaceContainer(oldContainer, newContainer));
      })
      .finally(() => this.CmsService.publish('destroy-component-properties-window'));
  }
}

export default ContainerService;
