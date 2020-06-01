/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { ComponentModel, ComponentType, Component } from './component';
import { MultipleTypeFactory } from './factory';

interface Task {
  model: ComponentModel;
  children?: Component[];
  siblings?: Component[];
}

type ComponentBuilder = (model: ComponentModel, children: Component[]) => Component;

/**
 * A component factory producing components based on a type.
 */
export class ComponentFactory extends MultipleTypeFactory<ComponentType, ComponentBuilder> {
  /**
   * Produces a component based on the model.
   * @param model The component model.
   */
  create(model: ComponentModel) {
    let component: Component;
    const queue = [{ model } as Task];

    while (queue.length) {
      const head = queue.shift()!;
      if (!head.children && head.model.components?.length) {
        head.children = [];
        queue.unshift(
          ...head.model.components.map(model => ({ model, siblings: head.children })),
          head,
        );

        continue;
      }

      component = this.buildComponent(head.model, head.children ?? []);

      if (head.siblings) {
        head.siblings.push(component);
      }
    }

    return component!;
  }

  private buildComponent(model: ComponentModel, children: Component[]) {
    const builder = this.mapping.get(model.type);
    if (!builder) {
      throw new Error(`Unsupported component type: '${model.type}'.`);
    }

    return builder(model, children);
  }
}
