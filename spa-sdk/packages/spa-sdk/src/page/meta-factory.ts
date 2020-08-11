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
import { SimpleFactory } from './factory';
import { MetaModel, MetaPosition, MetaType, Meta } from './meta';

type MetaBuilder = (model: MetaModel, position: MetaPosition) => Meta;

/**
 * The factory to produce meta-data collection from the page model meta-data.
 */
@injectable()
export class MetaFactory extends SimpleFactory<MetaType, MetaBuilder> {
  create(meta: MetaModel, position: MetaPosition) {
    const builder = this.mapping.get(meta.type);
    if (!builder) {
      throw new Error(`Unsupported meta type: '${meta.type}'.`);
    }

    return builder(meta, position);
  }
}
