/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
import { ComponentFactory } from './component-factory09';
import { ComponentMeta, Component } from './component';
import { ComponentModel } from './component09';
import { ContainerItemModel } from './container-item09';
import { ContainerModel } from './container09';
import { ContentFactory } from './content-factory09';
import { ContentModel, Content } from './content09';
import { LinkFactory } from './link-factory';
import { LinkRewriter, LinkRewriterService } from './link-rewriter';
import { Link, TYPE_LINK_INTERNAL } from './link';
import { EventBusService, EventBus, PageUpdateEvent } from '../events';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollectionModel } from './meta-collection';
import { PageModelToken, PageModel as PageModel10, Page } from './page';
import { Reference, isReference } from './reference';

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
  _meta: PageRootMeta;
}

/**
 * Model of a page.
 */
export interface PageModel {
  _links: PageModel10['links'];
  _meta: PageModel10['meta'];
  content?: { [reference: string]: ContentModel };
  page: (ComponentModel | ContainerItemModel | ContainerModel) & PageRootModel;
}

@injectable()
export class PageImpl implements Page {
  protected content: Map<string, Content>;

  protected root: Component;

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

    this.root = componentFactory.create(model.page);
    this.content = new Map(
      Object.entries(model.content || {}).map(
        ([alias, model]) => [alias, this.contentFactory(model)],
      ),
    );
  }

  protected onPageUpdate(event: PageUpdateEvent) {
    Object.entries((event.page as PageModel).content || {}).forEach(
      ([alias, model]) => this.content.set(alias, this.contentFactory(model)),
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

  getDocument<T>(): T | undefined {
    throw new Error('The page document is not supported by this version of the Page Model API.');
  }

  getMeta(meta: MetaCollectionModel) {
    return this.metaFactory(meta);
  }

  getTitle() {
    return this.model.page._meta.pageTitle;
  }

  getUrl(link?: Link): string | undefined;
  getUrl(path: string): string;
  getUrl(link?: Link | string) {
    return this.linkFactory.create(link as Link ?? { ...this.model._links.site, type: TYPE_LINK_INTERNAL });
  }

  getVersion() {
    return this.model._meta.version;
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
