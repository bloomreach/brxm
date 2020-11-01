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

<script lang="ts">
import { MetaCollection } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { Fragment } from 'vue-fragment';

@Component
export default class BrMeta extends Vue {
  @Prop() meta!: MetaCollection;

  private clear?: MetaCollection['clear'];

  mounted(): void {
    this.inject();
  }

  beforeUpdate(): void {
    this.clear?.();
  }

  updated(): void {
    this.inject();
  }

  beforeDestroy(): void {
    this.clear?.();
  }

  private inject() {
    if (!this.$vnode.elm) {
      return;
    }

    this.meta.render(this.$vnode.elm, this.$vnode.elm);
    this.clear = this.meta.clear.bind(this.meta);
  }

  @Watch('meta', { deep: false })
  private update() {
    this.$forceUpdate();
  }

  render(createElement: Vue.CreateElement): Vue.VNode {
    return this.$slots.default?.length === 1 ? this.$slots.default[0] : createElement(Fragment, this.$slots.default);
  }
}
</script>
