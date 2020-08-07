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

import { injectable } from 'inversify';
import { ComponentModel, ComponentType, Component } from './component';
import { SimpleFactory } from './factory';
import { PageModel } from './page';
import { resolve } from './reference';

type ComponentBuilder = (model: ComponentModel, children: Component[]) => Component;

/**
 * A component factory producing components based on a type.
 */
@injectable()
export class ComponentFactory extends SimpleFactory<ComponentType, ComponentBuilder> {
  /**
   * Produces a component based on the page model.
   * @param page The page model.
   */
  create(page: PageModel) {
    const heap = [page.root];
    const pool = new Map<ComponentModel, Component>();

    // tslint:disable-next-line: no-increment-decrement
    for (let i = 0; i < heap.length; i++) {
      heap.push(...resolve<ComponentModel>(page, heap[i])?.children ?? []);
    }

    return heap.reverse().reduce<Component | undefined>(
      (previous, reference) => {
        const model = resolve<ComponentModel>(page, reference)!;
        const children = model?.children?.map(child => pool.get(resolve<ComponentModel>(page, child)!)!) ?? [];
        const component = this.buildComponent(model, children);

        pool.set(model, component);

        return component;
      },
      undefined,
    );
  }

  private buildComponent(model: ComponentModel, children: Component[]) {
    const builder = this.mapping.get(model.type);
    if (!builder) {
      throw new Error(`Unsupported component type: '${model.type}'.`);
    }

    return builder(model, children);
  }
}
