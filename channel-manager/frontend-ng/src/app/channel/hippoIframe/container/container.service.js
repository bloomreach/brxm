/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
    $q,
    $rootScope,
    $translate,
    CmsService,
    DialogService,
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
    this.HippoIframeService = HippoIframeService;
    this.PageStructureService = PageStructureService;
    this.SpaService = SpaService;
  }

  async addComponent(catalogComponent, container, nextComponentId) {
    try {
      const { reloadRequired, newComponentId } = await this.PageStructureService
        .addComponentToContainer(catalogComponent, container, nextComponentId);

      if (!this._reloadSpa()) {
        if (reloadRequired) {
          await this.HippoIframeService.reload();
        } else {
          await this.PageStructureService.renderContainer(container);
        }
      }

      return newComponentId;
    } catch (error) {
      this.HippoIframeService.reload();

      throw error;
    }
  }

  _reloadSpa() {
    if (!this.SpaService.isSpa()) {
      return false;
    }

    // we don't provide fine-grained reloading of containers ATM, so reload the whole SPA instead
    this.HippoIframeService.reload();

    return true;
  }

  async moveComponent(component, container, nextComponent) {
    const {
      reloadRequired,
      changedContainers,
    } = await this.PageStructureService.moveComponent(component, container, nextComponent);

    if (this._reloadSpa()) {
      return;
    }

    if (reloadRequired) {
      await this.HippoIframeService.reload();
    } else {
      await this.$q.all(changedContainers.map(
        changedContainer => this.PageStructureService.renderContainer(changedContainer),
      ));
    }

    this.$rootScope.$emit('component:moved');
  }

  async deleteComponent(component) {
    await this._confirmDelete(component);

    this._doDelete(component.getId());
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
    return this.PageStructureService.removeComponentById(componentId)
      .then(container => this._reloadSpa() || this.PageStructureService.renderContainer(container))
      .catch(() => this.HippoIframeService.reload())
      .finally(() => this.CmsService.publish('destroy-component-properties-window'));
  }

  async renderComponent(component, properties) {
    if (this.SpaService.isSpa()) {
      try {
        await this.SpaService.renderComponent(component, properties);

        return;
      } catch (error) {
        this.$log.error(error);

        throw error;
      }
    }

    try {
      await this.PageStructureService.renderComponent(component, properties);
    } catch (error) {
      // component being edited is removed (by someone else), reload the page
      this.HippoIframeService.reload();

      throw error;
    }
  }
}

export default ContainerService;
