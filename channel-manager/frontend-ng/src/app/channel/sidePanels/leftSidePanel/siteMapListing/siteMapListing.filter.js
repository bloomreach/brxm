/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

function siteMapListingFilter() {
  'ngInject';

  return (list, query, fields) => {
    if (!query) {
      return list;
    }

    query = query.toLowerCase().split(' ');

    if (!angular.isArray(fields)) {
      fields = [fields.toString()];
    }
    return list.filter(item => query.every(needle => fields.some((field) => {
      let content = item[field] != null ? item[field] : '';
      if (!angular.isString(content)) {
        content = `${content}`;
      }

      return content.toLowerCase().indexOf(needle) > -1;
    })));
  };
}

export default siteMapListingFilter;
