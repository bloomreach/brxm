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

import { injectable } from 'inversify';
import { Builder, SimpleFactory } from './factory';
import { ContentModel } from './content';

type ContentBuilder = Builder<[ContentModel], unknown>;

@injectable()
export class ContentFactory extends SimpleFactory<ContentModel['type'], ContentBuilder> {
  create(model: ContentModel) {
    if (!this.mapping.has(model.type)) {
      return model;
    }

    return this.mapping.get(model.type)!(model);
  }
}
