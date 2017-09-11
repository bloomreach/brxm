/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

function incrementPropertyFilter() {
  'ngInject';

  return (collection, propertyName, propertyValue, subCollection) => {
    const itemsWithProperty = [];

    function findPropertiesAndSubProperties(newCollection) {
      for (let i = 0; i < newCollection.length; i += 1) {
        const propName = newCollection[i][propertyName];
        const match = propName.match(/\((\d+)\)/);
        if (match) {
          itemsWithProperty.push(match[1]);
        } else if (propName.match(propertyValue)) {
          itemsWithProperty.push('0');
        }
        if (subCollection && newCollection[i][subCollection]) {
          findPropertiesAndSubProperties(newCollection[i][subCollection]);
        }
      }
    }
    findPropertiesAndSubProperties(collection);

    if (itemsWithProperty.length === 0) {
      return propertyValue;
    }

    const maxNum = Math.max.apply(null, itemsWithProperty);
    if (!propertyValue.match(/\((\d+)\)/)) {
      propertyValue = `${propertyValue} (${maxNum})`;
    }
    return propertyValue.replace(/\((\d+)\)/, () => `(${maxNum + 1})`);
  };
}

export default incrementPropertyFilter;
