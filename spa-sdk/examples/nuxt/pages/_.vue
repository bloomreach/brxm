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
  <div class="d-flex flex-column vh-100">
    <br-page :configuration="configuration" :mapping="mapping" :page="page">
      <template #default="props">
        <header>
          <nav class="navbar navbar-expand-sm navbar-dark sticky-top bg-dark" role="navigation">
            <div class="container">
              <nuxt-link :to="props.page.getUrl('/')" class="navbar-brand">
                {{ props.page.getTitle() || 'brXM + Nuxt.js = ♥' }}
              </nuxt-link>
              <div class="collapse navbar-collapse">
                <br-component component="menu" />
              </div>
            </div>
          </nav>
        </header>
        <section class="container flex-fill pt-3">
          <br-component component="main" />
        </section>
        <footer class="bg-dark text-light py-3">
          <div class="container clearfix">
            <div class="float-left pr-3">&copy; Bloomreach</div>
            <div class="overflow-hidden">
              <br-component component="footer" />
            </div>
          </div>
        </footer>
      </template>
    </br-page>
  </div>
</template>

<script lang="ts">
import { Configuration, initialize } from '@bloomreach/spa-sdk';
import { Component, Vue, Watch } from 'nuxt-property-decorator';

import Banner from '~/components/BrBanner.vue';
import Content from '~/components/BrContent.vue';
import Menu from '~/components/BrMenu.vue';
import NewsList from '~/components/BrNewsList.vue';

@Component({
  async asyncData(context) {
    const configuration = {
      baseUrl: process.env.BASE_URL !== '/' ? process.env.BASE_URL : '',
      endpoint: process.env.VUE_APP_BRXM_ENDPOINT,
      endpointQueryParameter: 'endpoint',
      path: context.route.fullPath,
      visitor: context.nuxtState?.visitor,
    };

    const page = await initialize({
      ...configuration,
      httpClient: context.$axios,
      request: context.req,
    });

    if (process.server) {
      context.beforeNuxtRender(({ nuxtState }) => {
        nuxtState.visitor = page.getVisitor();
      });
    }

    return { configuration, page };
  },

  data: () => ({
    mapping: {
      Banner,
      Content,
      menu: Menu,
      'News List': NewsList,
      'Simple Content': Content,
    },
  }),
})
export default class App extends Vue {
  configuration!: Configuration;

  beforeMount() {
    this.configuration.httpClient = this.$axios;
  }

  beforeUpdate() {
    this.configuration.httpClient = this.$axios;
  }

  @Watch('$route', { deep: true })
  navigate() {
    this.$set(this.configuration, 'path', this.$route.fullPath);
  }
}
</script>
