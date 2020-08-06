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

import { ContainerModule } from 'inversify';
import xmldom from 'xmldom';

import { ComponentFactory } from './component-factory';
import {
  ComponentChildrenToken,
  ComponentImpl,
  ComponentModelToken,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER,
  TYPE_COMPONENT_CONTAINER_ITEM,
} from './component';
import { ContainerImpl } from './container';
import { ContainerItemImpl } from './container-item';
import { ContentFactory } from './content-factory';
import { ContentImpl, ContentModelToken, ContentModel } from './content';
import { DomParserService, LinkRewriterImpl, LinkRewriterService, XmlSerializerService } from './link-rewriter';
import { LinkFactory } from './link-factory';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollectionImpl, MetaCollectionModelToken, MetaCollectionModel } from './meta-collection';
import { MetaCommentImpl } from './meta-comment';
import { MetaFactory } from './meta-factory';
import { PageFactory } from './page-factory';
import { PageImpl, PageModelToken, PageModel } from './page';
import { TYPE_LINK_INTERNAL } from './link';
import { TYPE_META_COMMENT } from './meta';
import { UrlBuilderService, UrlBuilder } from '../url';

export function PageModule() {
  return new ContainerModule((bind) => {
    bind(LinkRewriterService).to(LinkRewriterImpl).inSingletonScope();
    bind(DomParserService).toConstantValue(new xmldom.DOMParser());
    bind(XmlSerializerService).toConstantValue(new xmldom.XMLSerializer());

    bind(LinkFactory).toSelf().inSingletonScope().onActivation(({ container }, factory) => {
      const url = container.get<UrlBuilder>(UrlBuilderService);

      return factory.register(TYPE_LINK_INTERNAL, url.getSpaUrl.bind(url));
    });

    bind(MetaCollectionFactory).toFactory(({ container }) => (model: MetaCollectionModel) => {
      const scope = container.createChild();
      scope.bind(MetaCollectionImpl).toSelf();
      scope.bind(MetaCollectionModelToken).toConstantValue(model);

      return scope.get(MetaCollectionImpl);
    });

    bind(MetaFactory).toSelf().inSingletonScope().onActivation((context, factory) => factory
      .register(TYPE_META_COMMENT, (model, position) => new MetaCommentImpl(model, position)),
    );

    bind(ContentFactory).toFactory(({ container }) => (model: ContentModel) => {
      const scope = container.createChild();
      scope.bind(ContentImpl).toSelf();
      scope.bind(ContentModelToken).toConstantValue(model);

      return scope.get(ContentImpl);
    });

    bind(ComponentFactory).toSelf().inSingletonScope().onActivation(({ container }, factory) => factory
      .register(TYPE_COMPONENT, (model, children) => {
        const scope = container.createChild();
        scope.bind(ComponentImpl).toSelf();
        scope.bind(ComponentModelToken).toConstantValue(model);
        scope.bind(ComponentChildrenToken).toConstantValue(children);

        return scope.get(ComponentImpl);
      })
      .register(TYPE_COMPONENT_CONTAINER, (model, children) => {
        const scope = container.createChild();
        scope.bind(ContainerImpl).toSelf();
        scope.bind(ComponentModelToken).toConstantValue(model);
        scope.bind(ComponentChildrenToken).toConstantValue(children);

        return scope.get(ContainerImpl);
      })
      .register(TYPE_COMPONENT_CONTAINER_ITEM, (model) => {
        const scope = container.createChild();
        scope.bind(ContainerItemImpl).toSelf();
        scope.bind(ComponentModelToken).toConstantValue(model);

        return scope.get(ContainerItemImpl);
      }),
    );

    bind(PageFactory).toFactory(({ container }) => (model: PageModel) => {
      const scope = container.createChild();
      scope.bind(PageImpl).toSelf();
      scope.bind(PageModelToken).toConstantValue(model);

      return scope.get(PageImpl);
    });
  });
}

export {
  Component,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER,
  TYPE_COMPONENT_CONTAINER_ITEM,
  isComponent,
} from './component';
export { ContainerItem, isContainerItem } from './container-item';
export {
  Container,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
  isContainer,
} from './container';
export { Content } from './content';
export { Link, TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL, TYPE_LINK_RESOURCE, isLink } from './link';
export { Menu } from './menu';
export { MetaCollection } from './meta-collection';
export { MetaComment, isMetaComment } from './meta-comment';
export { Meta, META_POSITION_BEGIN, META_POSITION_END, isMeta } from './meta';
export { PageFactory } from './page-factory';
export { PageModel, Page, isPage } from './page';
export { Reference, isReference } from './reference';
export { Visitor, Visit } from './relevance';
