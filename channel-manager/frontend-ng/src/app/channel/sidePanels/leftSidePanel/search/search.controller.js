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

class SearchController {
  constructor($filter) {
    'ngInject';

    this.keywords = '';
    this.searchFilter = $filter('search');
  }

  $onChanges() {
    this.clearFilter();
  }

  get translationData() {
    const result = {
      hits: this.hits,
      total: this.items.length,
      itemName: this.itemName,
      itemsName: this.itemsName,
    };
    return result;
  }

  filterItems() {
    const filteredItems = this.searchFilter(this.items, this.keywords, this.filteredFields);
    this.hits = filteredItems.length;
    this.onFilter({ filteredItems });
  }

  clearFilter() {
    this.keywords = '';
    this.hits = this.items.length;
    this.onFilter({ filteredItems: this.items });
  }
}

export default SearchController;
