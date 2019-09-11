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

import { Component, ComponentMeta, ComponentModel } from './component';

/**
 * Meta-data of a page root component.
 */
export interface RootMeta extends ComponentMeta {
  pageTitle?: string;
}

/**
 * Model of a page root component.
 */
export interface RootModel extends ComponentModel {
  _meta?: RootMeta;
}

/**
 * Meta-data of a page.
 */
export interface PageMeta {
  preview?: boolean;
}

/**
 * Model of a page.
 */
export interface PageModel {
  _meta?: PageMeta;
  page: RootModel;
}

/**
 * The current page to render.
 */
export interface Page {
  /**
   * Gets a component in the page (e.g. getComponent('main', 'right')).
   * Without any arguments it returns the root component.
   *
   * @param componentNames the names of the component and its parents.
   * @return The component, or `undefined` if no such component exists.
   */
  getComponent<T extends Component>(...componentNames: string[]): T | undefined;

  /**
   * Returns the title of the page, if configured.
   * @returns the title of the page, or `undefined` if not configured.
   */
  getTitle(): string | undefined;
}

export class Page implements Page {
  constructor(private model: PageModel, protected root: Component) {}

  getComponent(...componentNames: string[]) {
    return this.root.getComponent(...componentNames);
  }

  getTitle(): string | undefined {
    return this.model.page._meta && this.model.page._meta.pageTitle;
  }
}
