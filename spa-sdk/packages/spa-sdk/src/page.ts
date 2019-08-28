/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, ComponentParameters, Container, ContainerItem, ContainerType, Page } from './api';

export function createPage(model: any): Page {
  return new PageImpl(model && model.page);
}

class PageImpl implements Page {

  readonly title?: string;
  readonly root: ComponentImpl | null;

  constructor(model: any) {
    if (model) {
      this.title = model._meta && model._meta.pageTitle;
      this.root = createComponent(model);
    } else {
      this.root = null;
    }
  }

  getComponent(...componentNames: string[]): Component | null {
    if (this.root === null) {
      return null;
    }
    return this.root.getComponent(...componentNames);
  }

  getTitle(): string | undefined {
    return this.title;
  }
}

function createComponent(model: any): ComponentImpl | null {
  switch (model && model.type) {
    case 'CONTAINER_COMPONENT':
      return new ContainerImpl(model);
    case 'CONTAINER_ITEM_COMPONENT':
      return new ContainerItemImpl(model);
    default:
      return new ComponentImpl(model);
  }
}

class ComponentImpl implements Component {

  readonly name: string;
  readonly parameters: ComponentParameters;
  readonly children: ComponentImpl[];

  constructor(model: any) {
    this.name = (model && model.name) || '';
    this.parameters = (model && model._meta && model._meta.params) || {};
    this.children = (model.components || []).map(createComponent);
  }

  getName(): string {
    return this.name;
  }

  getParameters(): ComponentParameters {
    return this.parameters;
  }

  getComponent(...componentNames: string[]): Component | null {
    if (componentNames.length === 0) {
      return this;
    }

    const firstName = componentNames[0];
    const child = this.children.find(component => component.name === firstName);
    if (child) {
      const remainingNames = componentNames.slice(1);
      return child.getComponent(...remainingNames);
    }

    return null;
  }
}

class ContainerImpl extends ComponentImpl implements Container {

  readonly type: string; // TODO: change type to ContainerType;

  constructor(model: any) {
    super(model);
    this.type = model.xtype;
  }

  getChildren(): ContainerItem[] {
    return this.children as unknown as ContainerItem[];
  }

  getType(): string {
    return this.type;
  }
}

class ContainerItemImpl extends ComponentImpl implements ContainerItem {

  readonly type: string;
  readonly hidden: boolean;
  readonly parameters: ComponentParameters;

  constructor(model: any) {
    super(model);

    this.type = model && model.label;
    this.hidden = super.getParameters()['com.onehippo.cms7.targeting.TargetingParameterUtil.hide'] === 'on';
    this.parameters = (model && model._meta && model._meta.paramsInfo) || {};
  }

  getType(): string {
    return this.type;
  }

  isHidden(): boolean {
    return this.hidden;
  }

  getParameters(): ComponentParameters {
    return this.parameters;
  }
}
