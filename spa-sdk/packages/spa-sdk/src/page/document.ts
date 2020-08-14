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
import { LinkFactory } from './link-factory';
import { Link } from './link';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollectionModel, MetaCollection } from './meta-collection';

export const DocumentModelToken = Symbol.for('DocumentModelToken');

export const TYPE_DOCUMENT = 'document';

type DocumentLinks = 'site';

interface DocumentDataModel {
  id: string;
  localeString?: string;
  name: string;
  [property: string]: any;
}

/**
 * Model of a document item.
 */
export interface DocumentModel {
  data: DocumentDataModel;
  links: Record<DocumentLinks, Link>;
  meta?: MetaCollectionModel;
  type: typeof TYPE_DOCUMENT;
}

/**
 * Document used on the page.
 */
export interface Document {
  /**
   * @return The document id.
   */
  getId(): string;

  /**
   * @return The document locale.
   */
  getLocale(): string | undefined;

  /**
   * @return The document meta-data collection.
   */
  getMeta(): MetaCollection;

  /**
   * @return The document name.
   */
  getName(): string;

  /**
   * @return The document data.
   */
  getData(): DocumentDataModel;
  getData<T extends Record<string, any>>(): T & DocumentDataModel;

  /**
   * @return The link to the content.
   */
  getUrl(): string | undefined;
}

@injectable()
export class DocumentImpl implements Document {
  protected meta: MetaCollection;

  constructor(
    @inject(DocumentModelToken) protected model: DocumentModel,
    @inject(LinkFactory) private linkFactory: LinkFactory,
    @inject(MetaCollectionFactory) metaFactory: MetaCollectionFactory,
  ) {
    this.meta = metaFactory(this.model.meta ?? {});
  }

  getId() {
    return this.model.data.id;
  }

  getLocale() {
    return this.model.data.localeString;
  }

  getMeta() {
    return this.meta;
  }

  getName() {
    return this.model.data.name;
  }

  getData() {
    return this.model.data;
  }

  getUrl() {
    return this.linkFactory.create(this.model.links.site);
  }
}

/**
 * Checks whether a value is a document.
 * @param value The value to check.
 */
export function isDocument(value: any): value is Document {
  return value instanceof DocumentImpl;
}
