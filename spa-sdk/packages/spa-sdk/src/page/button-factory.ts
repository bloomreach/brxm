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

import { injectable, inject } from 'inversify';
import { Builder, SimpleFactory } from './factory';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection, MetaCollectionModel, isMetaCollection } from './meta-collection';

type ButtonBuilder = Builder<any[], MetaCollection | MetaCollectionModel>;

@injectable()
export class ButtonFactory extends SimpleFactory<string, ButtonBuilder> {
  constructor(@inject(MetaCollectionFactory) private metaCollectionFactory: MetaCollectionFactory) {
    super();
  }

  create(type: string, ...params: unknown[]) {
    if (!this.mapping.has(type)) {
      throw new Error(`Unsupported button type: '${type}'.`);
    }

    const meta = this.mapping.get(type)!(...params);

    return isMetaCollection(meta) ? meta : this.metaCollectionFactory(meta);
  }
}
