/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */

export class ComponentsService {
  constructor(MountService) {
    'ngInject';

    this.MountService = MountService;
  }

  getComponents() {
    return this.MountService.getComponentsToolkit().then((components) => {
      components.sort((a, b) => a.label.localeCompare(b.label));
      return components;
    });
  }

  setCatalogElement(component, element) {
    component.element = element;
  }

  getComponentByElement(element) {
    return this.components.find((component) => component.element === element);
  }

}
