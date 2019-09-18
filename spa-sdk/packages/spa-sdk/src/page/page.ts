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

import { Typed } from 'emittery';
import { Component, ComponentMeta, ComponentModel } from './component';
import { ContentModel, Content } from './content';
import { ContentMap } from './content-map';
import { Events, PageUpdateEvent } from '../events';
import { Reference, isReference } from './reference';

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
  content?: { [reference: string]: ContentModel };
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
  getComponent<T extends Component>(): T;
  getComponent<T extends Component>(...componentNames: string[]): T | undefined;

  /**
   * Gets a content item used in the page.
   * @param reference The reference to the content. It can be an object containing
   * an [RFC-6901](https://tools.ietf.org/html/rfc6901) JSON Pointer.
   */
  getContent(reference: Reference | string): Content;

  /**
   * Returns the title of the page, if configured.
   * @returns the title of the page, or `undefined` if not configured.
   */
  getTitle(): string | undefined;
}

export class Page implements Page {
  constructor(
    protected model: PageModel,
    protected root: Component,
    protected content: ContentMap,
    eventBus: Typed<Events>,
  ) {
    eventBus.on('page.update', this.onPageUpdate.bind(this));
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    event.page.content.forEach((content, reference) => this.content.set(reference, content));
  }

  private static getContentReference(reference: Reference) {
    return  reference.$ref.split('/', 3)[2] || '';
  }

  getComponent<T extends Component>(): T;
  getComponent<T extends Component>(...componentNames: string[]): T | undefined;
  getComponent(...componentNames: string[]) {
    return this.root.getComponent(...componentNames);
  }

  getContent(reference: Reference | string) {
    const contentReference = isReference(reference)
      ? Page.getContentReference(reference)
      : reference;

    return this.content.get(contentReference);
  }

  getTitle() {
    return this.model.page._meta && this.model.page._meta.pageTitle;
  }
}
