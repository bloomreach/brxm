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

import React from 'react';
import { Meta, META_POSITION_BEGIN, META_POSITION_END } from '@bloomreach/spa-sdk';
import { BrMeta } from './BrMeta';

interface BrMetaWrapperProps {
  meta: Meta[];
}

function renderMeta(meta: Meta[], position: typeof META_POSITION_BEGIN | typeof META_POSITION_END) {
  return meta
    .filter(meta => position === meta.getPosition())
    .map((meta, index) => <BrMeta key={index} meta={meta} />);
}

// tslint:disable-next-line:function-name
export function BrMetaWrapper(props: React.PropsWithChildren<BrMetaWrapperProps>) {
  return (
    <>
      {renderMeta(props.meta, META_POSITION_BEGIN)}
      {props.children}
      {renderMeta(props.meta, META_POSITION_END)}
    </>
  );
}
