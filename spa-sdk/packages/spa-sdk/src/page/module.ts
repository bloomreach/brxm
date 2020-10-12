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

import { ContainerModule } from 'inversify';
import { DOMParser, XMLSerializer } from 'xmldom';
import { Typed } from 'emittery';

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
import { DocumentImpl, DocumentModelToken, TYPE_DOCUMENT } from './document';
import { DomParserService, LinkRewriterImpl, LinkRewriterService, XmlSerializerService } from './link-rewriter';
import { EventBusService } from './events';
import { ImageFactory, ImageImpl, ImageModelToken, ImageModel } from './image';
import { ImageSetImpl, ImageSetModelToken, TYPE_IMAGE_SET } from './image-set';
import { LinkFactory } from './link-factory';
import { MenuImpl, MenuModelToken, TYPE_MENU } from './menu';
import { MenuItemFactory, MenuItemImpl, MenuItemModelToken, MenuItemModel } from './menu-item';
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
    bind(EventBusService).toConstantValue(new Typed());
    bind(LinkRewriterService).to(LinkRewriterImpl).inSingletonScope();
    bind(DomParserService).toConstantValue(new DOMParser());
    bind(XmlSerializerService).toConstantValue(new XMLSerializer());

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

    bind(MenuItemFactory).toFactory(({ container }) => (model: MenuItemModel) => {
      const scope = container.createChild();
      scope.bind(MenuItemImpl).toSelf();
      scope.bind(MenuItemModelToken).toConstantValue(model);

      return scope.get(MenuItemImpl);
    });

    bind(ImageFactory).toFactory(({ container }) => (model: ImageModel) => {
      const scope = container.createChild();
      scope.bind(ImageImpl).toSelf();
      scope.bind(ImageModelToken).toConstantValue(model);

      return scope.get(ImageImpl);
    });

    bind(ContentFactory).toSelf().inSingletonScope().onActivation(({ container }, factory) => factory
      .register(TYPE_DOCUMENT, (model) => {
        const scope = container.createChild();
        scope.bind(DocumentImpl).toSelf();
        scope.bind(DocumentModelToken).toConstantValue(model);

        return scope.get(DocumentImpl);
      })
      .register(TYPE_IMAGE_SET, (model) => {
        const scope = container.createChild();
        scope.bind(ImageSetImpl).toSelf();
        scope.bind(ImageSetModelToken).toConstantValue(model);

        return scope.get(ImageSetImpl);
      })
      .register(TYPE_MENU, (model) => {
        const scope = container.createChild();
        scope.bind(MenuImpl).toSelf();
        scope.bind(MenuModelToken).toConstantValue(model);

        return scope.get(MenuImpl);
      }),
    );

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
