<!--
  Copyright 2020 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

<template>
  <nav aria-label="News List Pagination">
    <ul class="pagination">
      <li class="page-item" :class="{ disabled: !pageable.previous }">
        <router-link :to="previousUrl" class="page-link" aria-label="Previous">
          <span aria-hidden="true">&laquo;</span>
          <span class="sr-only">Previous</span>
        </router-link>
      </li>
      <li
        v-for="(pageNumber, key) in pageable.pageNumbersArray"
        :key="key"
        class="page-item"
        :class="{ active: pageNumber === pageable.currentPage }"
      >
        <router-link :to="page.getUrl(`?page=${pageNumber}`)" class="page-link">{{ pageNumber }}</router-link>
      </li>
      <li class="page-item" :class="{ disabled: !pageable.next }">
        <router-link :to="nextUrl" class="page-link" aria-label="Next">
          <span aria-hidden="true">&raquo;</span>
          <span class="sr-only">Next</span>
        </router-link>
      </li>
    </ul>
  </nav>
</template>

<script lang="ts">
import { Page } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  computed: {
    nextUrl(this: BrNewsListPagination) {
      return this.pageable.next ? this.page.getUrl(`?page=${this.pageable.nextPage}`) : '#';
    },
    previousUrl(this: BrNewsListPagination) {
      return this.pageable.previous ? this.page.getUrl(`?page=${this.pageable.previousPage}`) : '#';
    },
  },
  name: 'br-news-list-pagination',
})
export default class BrNewsListPagination extends Vue {
  @Prop() pageable!: Pageable;

  @Prop() page!: Page;
}
</script>
