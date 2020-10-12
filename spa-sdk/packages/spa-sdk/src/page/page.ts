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

import { inject, injectable } from 'inversify';
import { ComponentFactory } from './component-factory';
import { ComponentMeta, ComponentModel, Component } from './component';
import { ContainerItemModel } from './container-item';
import { ContainerModel } from './container';
import { ContentFactory } from './content-factory';
import { ContentModel } from './content';
import { Content } from './content09';
import { EventBusService, EventBus, PageUpdateEvent } from '../events';
import { LinkFactory } from './link-factory';
import { LinkRewriter, LinkRewriterService } from './link-rewriter';
import { Link, isLink } from './link';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollectionModel, MetaCollection } from './meta-collection';
import { Reference, isReference, resolve } from './reference';
import { Visitor, Visit } from './relevance';
import { isAbsoluteUrl, resolveUrl } from '../url';

export const PageModelToken = Symbol.for('PageModelToken');

type PageLinks = 'self' | 'site';

/**
 * Meta-data of a page root component.
 */
interface PageRootMeta extends ComponentMeta {
  pageTitle?: string;
}

/**
 * Model of a page root component.
 */
interface PageRootModel {
  meta: PageRootMeta;
}

/**
 * Meta-data of a page.
 */
interface PageMeta {
  /**
   * The current Page Model version.
   */
  version?: string;

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
 */
export interface PageModel {
  document?: Reference;
  links: Record<PageLinks, Link>;
  meta: PageMeta;
  page: Record<string,  (ComponentModel | ContainerItemModel | ContainerModel) & PageRootModel | ContentModel>;
  root: Reference;
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
   * Gets a custom content item used on the page.
   * @param reference The reference to the content. It can be an object containing
   * an [RFC-6901](https://tools.ietf.org/html/rfc6901) JSON Pointer.
   */
  getContent<T>(reference: Reference | string): T | undefined;

  /**
   * Gets the page root document.
   * This option is available only along with the Experience Pages feature.
   */
  getDocument<T>(): T | undefined;

  /**
   * Generates a meta-data collection from the provided meta-data model.
   * @param meta The meta-data collection model as returned by the page-model-api.
   */
  getMeta(meta: MetaCollectionModel): MetaCollection;

  /**
   * @return The title of the page, or `undefined` if not configured.
   */
  getTitle(): string | undefined;

  /**
   * Generates a URL for a link object.
   * - If the link object type is internal, then it will prepend `spaBaseUrl` or `baseUrl`.
   *   In case when the link starts with the same path as in `cmsBaseUrl`, this part will be removed.
   *   For example, for link `/site/_cmsinternal/spa/about` with configuration options
   *   `cmsBaseUrl = "http://localhost:8080/site/_cmsinternal/spa"` and `spaBaseUrl = "http://example.com"`
   *   it will generate `http://example.com/about`.
   * - If the link object type is unknown, then it will return `undefined`.
   * - If the link parameter is omitted, then the link to the current page will be returned.
   * - In other cases, the link will be returned as-is.
   * @param link The link object to generate URL.
   */
  getUrl(link?: Link): string | undefined;

  /**
   * Generates an SPA URL for the path.
   * - If it is a relative path and `cmsBaseUrl` is present, then it will prepend `spaBaseUrl`.
   * - If it is an absolute path and `cmsBaseUrl` is present,
   *   then the behavior will be similar to internal link generation.
   * - If it is a relative path and `endpoint` is present,
   *   then it will resolve this link relative to the current page URL.
   * - If it is an absolute path and `endpoint` is present,
   *   then it will resolve this link relative to the `baseUrl` option.
   * @param path The path to generate URL.
   */
  getUrl(path: string): string;

  /**
   * @return The Page Model version.
   */
  getVersion(): string | undefined;

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
  toJSON(): object;
}

@injectable()
export class PageImpl implements Page {
  protected content = new WeakMap<object, unknown>();

  protected root?: Component;

  constructor(
    @inject(PageModelToken) protected model: PageModel,
    @inject(ComponentFactory) componentFactory: ComponentFactory,
    @inject(ContentFactory) private contentFactory: ContentFactory,
    @inject(EventBusService) private eventBus: EventBus,
    @inject(LinkFactory) private linkFactory: LinkFactory,
    @inject(LinkRewriterService) private linkRewriter: LinkRewriter,
    @inject(MetaCollectionFactory) private metaFactory: MetaCollectionFactory,
  ) {
    this.eventBus.on('page.update', this.onPageUpdate.bind(this));

    this.root = componentFactory.create(model);
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    Object.assign(this.model.page, event.page.page);
  }

  getComponent<T extends Component>(): T;
  getComponent<T extends Component>(...componentNames: string[]): T | undefined;
  getComponent(...componentNames: string[]) {
    return this.root?.getComponent(...componentNames);
  }

  getContent<T>(reference: Reference | string): T | undefined;
  getContent(reference: Reference | string): unknown | undefined {
    const model = resolve<ContentModel>(
      this.model,
      isReference(reference) ? reference : { $ref: `/page/${reference}` },
    );

    if (!model) {
      return undefined;
    }

    if (!this.content.has(model)) {
      this.content.set(model, this.contentFactory.create(model));
    }

    return this.content.get(model);
  }

  getDocument<T>(): T | undefined {
    return this.model.document && this.getContent(this.model.document);
  }

  getMeta(meta: MetaCollectionModel) {
    return this.metaFactory(meta);
  }

  getTitle() {
    return resolve<PageRootModel>(this.model, this.model.root)?.meta?.pageTitle;
  }

  getUrl(link?: Link): string | undefined;
  getUrl(path: string): string;
  getUrl(link?: Link | string) {
    if (typeof link === 'undefined' || isLink(link) || isAbsoluteUrl(link)) {
      return this.linkFactory.create(link as Link ?? this.model.links.site ?? '');
    }

    return resolveUrl(link, this.linkFactory.create(this.model.links.site) ?? '');
  }

  getVersion() {
    return this.model.meta.version;
  }

  getVisitor() {
    return this.model.meta.visitor;
  }

  getVisit() {
    return this.model.meta.visit;
  }

  isPreview() {
    return !!this.model.meta.preview;
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
