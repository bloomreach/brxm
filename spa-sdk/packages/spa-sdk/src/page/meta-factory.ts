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

import { MultipleTypeFactory } from './factory';
import { MetaCollectionModel, MetaModel, MetaPosition, MetaType, Meta, META_POSITION_BEGIN, META_POSITION_END } from './meta';

type MetaBuilder = (model: MetaModel, position: MetaPosition) => Meta;

/**
 * The factory to produce meta-data collection from the page model meta-data.
 */
export class MetaFactory extends MultipleTypeFactory<MetaType, MetaBuilder> {
  create(meta: MetaCollectionModel) {
    return [
      ...(meta.beginNodeSpan || []).map(this.buildMeta.bind(this, META_POSITION_BEGIN)),
      ...(meta.endNodeSpan || []).map(this.buildMeta.bind(this, META_POSITION_END)),
    ];
  }

  private buildMeta(position: MetaPosition, model: MetaModel) {
    const builder = this.mapping.get(model.type);
    if (!builder) {
      throw new Error(`Unsupported meta type: '${model.type}'.`);
    }

    return builder(model, position);
  }
}
