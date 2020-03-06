/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

class ComponentCatalogController {
  constructor($translate, ComponentCatalogService, MaskService) {
    'ngInject';

    this.$translate = $translate;
    this.MaskService = MaskService;
    this.ComponentCatalogService = ComponentCatalogService;

    this.filteredFields = ['label'];
    this.filteredComponents = [];
  }

  onFilter(filteredComponents) {
    this.filteredComponents = filteredComponents;
  }

  async onSelect(component) {
    const { state } = this;

    this.state = true;
    try {
      await this.ComponentCatalogService.selectComponent(component);
    } catch (error) {
      this.state = state;
    }
  }

  isComponentSelected(component) {
    const selectedComponent = this.ComponentCatalogService.getSelectedComponent();
    return this.MaskService.isMasked && selectedComponent === component;
  }

  getComponentLabel(component) {
    if (this.isComponentSelected(component)) {
      return this.$translate.instant('ADDING_COMPONENT', { component: component.label });
    }
    return component.label;
  }
}

export default ComponentCatalogController;
