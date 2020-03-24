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

import { Typed } from 'emittery';
import { ComponentMeta, ComponentModel, Component } from './component';
import { ContainerItemModel } from './container-item';
import { ContainerModel } from './container';
import { ContentModel, Content } from './content';
import { Factory } from './factory';
import { LinkRewriter } from './link-rewriter';
import { Link, TYPE_LINK_INTERNAL } from './link';
import { Events, PageUpdateEvent } from '../events';
import { MetaCollectionModel, Meta } from './meta';
import { Reference, isReference } from './reference';
import { Visitor, Visit } from './relevance';

/**
 * @hidden
 */
type PageLinks = 'self' | 'site';

/**
 * Meta-data of a page root component.
 * @hidden
 */
interface PageRootMeta extends ComponentMeta {
  pageTitle?: string;
}

/**
 * Model of a page root component.
 * @hidden
 */
interface PageRootModel {
  _meta: PageRootMeta;
}

/**
 * Meta-data of a page.
 * @hidden
 */
interface PageMeta {
  /**
   * Meta-data about the current visitor. Available when the Relevance Module is enabled.
   * @see https://documentation.bloomreach.com/library/enterprise/enterprise-features/targeting/targeting.html
   */
  visitor?: Visitor;

  /**
   * Meta-data about the current visit. Available when the Relevance Module is enabled.
   * @see https://documentation.bloomreach.com/library/enterprise/enterprise-features/targeting/targeting.html
   */
  visit?: Visit;

  /**
   * Preview mode flag.
   */
  preview?: boolean;
}

/**
 * Model of a page.
 * @hidden
 */
export interface PageModel {
  _links?: Record<PageLinks, Link>;
  _meta: PageMeta;
  content?: { [reference: string]: ContentModel };
  page: (ComponentModel | ContainerItemModel | ContainerModel) & PageRootModel;
}

/**
 * The current page to render.
 */
export interface Page {
  /**
   * Gets a root component in the page.
   * @return The root component.
   */
  getComponent<T extends Component>(): T;

  /**
   * Gets a component in the page (e.g. `getComponent('main', 'right')`).
   * @param componentNames The names of the component and its parents.
   * @return The component, or `undefined` if no such component exists.
   */
  getComponent<T extends Component>(...componentNames: string[]): T | undefined;

  /**
   * Gets a content item used on the page.
   * @param reference The reference to the content. It can be an object containing
   * an [RFC-6901](https://tools.ietf.org/html/rfc6901) JSON Pointer.
   */
  getContent(reference: Reference | string): Content | undefined;

  /**
   * Generates a meta-data from the provided meta-data model.
   * @param meta the meta-collection as returned by the page-model-api
   */
  getMeta(meta: MetaCollectionModel): Meta[];

  /**
   * @return The title of the page, or `undefined` if not configured.
   */
  getTitle(): string | undefined;

  /**
   * Generates a URL for a link object.
   * - If the link object type is internal, then it will prepend `spaBaseUrl`.
   *   In case when the link starts with the same path as in `cmsBaseUrl`, this part will be removed.
   *   For example, for link `/site/_cmsinternal/spa/about` with configuration options
   *   `cmsBaseUrl = "http://localhost:8080/site/_cmsinternal/spa"` and `spaBaseUrl = "http://example.com"`
   *   it will generate `http://example.com/about`.
   * - If the link parameter is omitted, then the link to the current page will be returned.
   * - In other cases, the link will be returned as-is.
   * @param link The link object to generate URL.
   */
  getUrl(link?: Link): string;

  /**
   * Generates an SPA URL for the path.
   * - If it is a relative path, then it will prepend `spaBaseUrl`.
   * - If it is an absolute path, then the behavior will be similar to internal link generation.
   * @param path The path to generate URL.
   */
  getUrl(path: string): string;

  /**
   * @return The current visitor data.
   */
  getVisitor(): Visitor | undefined;

  /**
   * @return The current visit data.
   */
  getVisit(): Visit | undefined;

  /**
   * @returns Whether the page is in the preview mode.
   */
  isPreview(): boolean;

  /**
   * Rewrite links to pages and resources in the HTML content.
   * This method looks up for `a` tags with `data-type` and `href` attributes and `img` tags with `src` attribute.
   * Links will be updated according to the configuration used to initialize the page.
   * @param content The HTML content to rewrite links.
   * @param type The content type.
   */
  rewriteLinks(content: string, type?: SupportedType): string;

  /**
   * Synchronizes the CMS integration state.
   */
  sync(): void;

  /**
   * @return A plain JavaScript object of the page model.
   */
  toJSON(): PageModel;
}

export class PageImpl implements Page {
  protected content: Map<string, Content>;

  constructor(
    protected model: PageModel,
    protected root: Component,
    private contentFactory: Factory<[ContentModel], Content>,
    private eventBus: Typed<Events>,
    private linkFactory: Factory<[Link | string], string>,
    private linkRewriter: LinkRewriter,
    private metaFactory: Factory<[MetaCollectionModel], Meta[]>,
  ) {
    eventBus.on('page.update', this.onPageUpdate.bind(this));

    this.content = new Map(
      Object.entries(model.content || {}).map(
        ([alias, model]) => [alias, contentFactory.create(model)],
      ),
    );
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    Object.entries(event.page.content || {}).forEach(
      ([alias, model]) => this.content.set(alias, this.contentFactory.create(model)),
    );
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
      ? PageImpl.getContentReference(reference)
      : reference;

    return this.content.get(contentReference);
  }

  getMeta(meta: MetaCollectionModel) {
    return this.metaFactory.create(meta);
  }

  getTitle() {
    return this.model.page._meta.pageTitle;
  }

  getUrl(link?: Link | string) {
    return this.linkFactory.create(link || (
      this.model._links
        ? { ...this.model._links.site, type: TYPE_LINK_INTERNAL }
        : ''
    ));
  }

  getVisitor() {
    return this.model._meta.visitor;
  }

  getVisit() {
    return this.model._meta.visit;
  }

  isPreview() {
    return !!this.model._meta.preview;
  }

  rewriteLinks(content: string, type: SupportedType = 'text/html') {
    return this.linkRewriter.rewrite(content, type);
  }

  sync() {
    this.eventBus.emit('page.ready', {});
  }

  toJSON() {
    return this.model;
  }
}

/**
 * Checks whether a value is a page.
 * @param value The value to check.
 */
export function isPage(value: any): value is Page {
  return value instanceof PageImpl;
}
