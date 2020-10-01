/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class ComponentVariantsService {
  setCurrentVariant(variant) {
    this.currentVariant = variant;
  }

  getCurrentVariant() {
    return this.currentVariant;
  }

  extractExpressions(variant) {
    let persona = '';
    const characteristics = [];

    variant.expressions.forEach(({ id, type }) => {
      if (type === 'persona') {
        persona = id;
      } else {
        const parts = id.split('/');
        const characteristic = parts[0];
        const targetGroupId = parts[1];

        characteristics.push({
          [characteristic]: targetGroupId,
        });
      }
    });

    return {
      persona,
      characteristics,
    };
  }
}

export default ComponentVariantsService;
