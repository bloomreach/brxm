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

import { MetaCollectionModel, MetaModel, MetaPosition, MetaType, Meta, META_POSITION_BEGIN, META_POSITION_END } from './meta';

type MetaBuilder<T extends MetaType> = (model: MetaModel<T>, position: MetaPosition) => Meta<T>;

/**
 * The factory to produce meta-data collection from the page model meta-data.
 */
export class MetaFactory {
  private mapping = new Map<MetaType, MetaBuilder<any>>();

  /**
   * Registers a meta builder for the specified type.
   * @param type The meta type.
   * @param builder The meta builder.
   */
  register<T extends MetaType>(
    type: T,
    builder: MetaBuilder<T>,
  ) {
    this.mapping.set(type, builder);

    return this;
  }

  /**
   * Produces a meta-data collection based on the model.
   * @param meta The meta-data from the page model.
   */
  create(meta?: MetaCollectionModel) {
    return [
      ...(meta && meta.beginNodeSpan || []).map(this.buildMeta.bind(this, META_POSITION_BEGIN)),
      ...(meta && meta.endNodeSpan || []).map(this.buildMeta.bind(this, META_POSITION_END)),
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
