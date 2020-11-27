/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { FlatTreeControl } from '@angular/cdk/tree';
import { AfterViewChecked, Component, ElementRef, Input, NgZone, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, pluck } from 'rxjs/operators';

import { IframeService } from '../../../channels/services/iframe.service';
import { SiteMapItem } from '../../models/site-map-item.model';

interface SiteMapItemNode extends SiteMapItem {
  expandable: boolean;
  level: number;
}

@Component({
  selector: 'em-site-map',
  templateUrl: './site-map.component.html',
  styleUrls: ['./site-map.component.scss'],
})
export class SiteMapComponent implements OnChanges, OnDestroy, AfterViewChecked {
  @Input() siteMap: SiteMapItem[] = [];
  @Input() renderPathInfo?: string;

  private readonly treeFlattener = new MatTreeFlattener(
    (node: SiteMapItem, level: number) => ({
      ...node,
      level,
      expandable: !!node.children && node.children.length > 0,
    }),
    node => node.level,
    node => node.expandable,
    node => node.children,
  );
  readonly treeControl = new FlatTreeControl<SiteMapItemNode>(node => node.level, node => node.expandable);
  readonly dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  readonly search$ = new Subject<string>();

  private readonly matched = new Set<SiteMapItemNode>();
  private readonly visible = new Set<SiteMapItemNode>();

  isMatched = this.matched.has.bind(this.matched);
  isVisible = this.visible.has.bind(this.visible);

  constructor(
    private readonly iframeService: IframeService,
    private readonly zone: NgZone,
    private readonly elementRef: ElementRef,
  ) {
    this.search$
      .pipe(
        debounceTime(500),
        map<string, [string, SiteMapItemNode[]]>(query => [query, this.treeControl.dataNodes]),
        distinctUntilChanged(([prevQuery, prevNodes], [query, nodes]) => query === prevQuery && nodes === prevNodes),
        pluck(0),
      )
      .subscribe(this.onSearch.bind(this));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.siteMap) {
      this.dataSource.data = this.siteMap;
      this.zone.run(() => this.search$.next(''));
    }

    this.expandSelected();
  }

  ngAfterViewChecked(): void {
    const selectedNode = this.elementRef.nativeElement.querySelector('.selected');
    selectedNode?.scrollIntoView();
  }

  ngOnDestroy(): void {
    this.search$.complete();
  }

  isSelected(node: SiteMapItem): boolean {
    return node.renderPathInfo === this.renderPathInfo;
  }

  async selectNode(node: SiteMapItemNode): Promise<void> {
    if (node.expandable) {
      this.treeControl.expand(node);
    }

    if (node.renderPathInfo) {
      await this.iframeService.load(node.renderPathInfo);
    }
  }

  private expandSelected(): void {
    const node = this.treeControl.dataNodes.find(({ renderPathInfo }) => renderPathInfo === this.renderPathInfo);

    if (node) {
      this.treeControl.expand(node);
      this.getParents(node).forEach(parent => {
        this.treeControl.expand(parent);
      });
    }
  }

  private getParents(child: SiteMapItemNode): SiteMapItemNode[] {
    const parents = [];

    for (let i = this.treeControl.dataNodes.indexOf(child), childLevel = this.treeControl.getLevel(child); i >= 0; i--) {
      const node = this.treeControl.dataNodes[i];
      const level = this.treeControl.getLevel(node);
      if (level < childLevel) {
        parents.push(node);
        childLevel = level;
      }
    }

    return parents;
  }

  private onSearch(value: string): void {
    this.matched.clear();
    this.visible.clear();

    if (!value) {
      this.treeControl.dataNodes.forEach(node => {
        this.matched.add(node);
        this.visible.add(node);
      });

      return;
    }

    this.treeControl.dataNodes
      .filter(
        ({ name, pageTitle }) => name.toLowerCase().includes(value.toLowerCase())
          || pageTitle?.toLowerCase().includes(value.toLowerCase()),
      )
      .forEach(node => {
        this.matched.add(node);

        this.getParents(node).forEach(parent => {
          this.treeControl.expand(parent);
          this.visible.add(parent);
        });

        this.treeControl.getDescendants(node).forEach(child => {
          this.visible.add(child);
        });
      });
  }
}
