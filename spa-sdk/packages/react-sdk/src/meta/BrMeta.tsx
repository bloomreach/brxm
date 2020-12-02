/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import React, { useEffect, useRef } from 'react';
import { MetaCollection } from '@bloomreach/spa-sdk';

interface BrMetaProps {
  meta: MetaCollection;
}

export function BrMeta({ children, meta }: React.PropsWithChildren<BrMetaProps>) {
  const head = useRef<HTMLSpanElement>(null);
  const tail = useRef<HTMLSpanElement>(null);

  useEffect(
    () => {
      if (!head.current?.nextSibling || !tail.current) {
        return;
      }

      return meta.render(head.current.nextSibling, tail.current);
    },
    [meta, head.current?.nextSibling, tail.current],
  );

  return (
    <>
      {meta.length > 0 && <span style={{ display: 'none' }} ref={head} />}
      {children}
      {meta.length > 0 && <span style={{ display: 'none' }} ref={tail} />}
    </>
  );
}
