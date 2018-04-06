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
  constructor($log, $translate, CmsService, DialogService, DragDropService, PageStructureService) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DialogService = DialogService;
    this.DragDropService = DragDropService;
    this.PageStructureService = PageStructureService;
  }

  deleteComponent(componentId) {
    const selectedComponent = this.PageStructureService.getComponentById(componentId);
    if (!selectedComponent) {
      this.$log.warn(`Cannot delete unknown component with id '${componentId}'`);
      return;
    }
    this._confirmDelete(selectedComponent).then(
      this._doDelete(componentId),
      () => this.PageStructureService.showComponentProperties(selectedComponent),
    );
  }

  _doDelete(componentId) {
    return () => this.PageStructureService.removeComponentById(componentId)
      .then(({ oldContainer, newContainer }) => this.DragDropService.replaceContainer(oldContainer, newContainer))
      .finally(() => this.CmsService.publish('destroy-component-properties-window'));
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
}

export default ContainerService;
