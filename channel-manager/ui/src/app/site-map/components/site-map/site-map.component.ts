/*!
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import { Component, ElementRef, Inject, Input, NgZone, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';
import { combineLatest, Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, pluck, tap } from 'rxjs/operators';

import { IframeService } from '../../../channels/services/iframe.service';
import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1.service';
import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../../../services/ng1/config.ng1.service';
import { NG1_ROOT_SCOPE } from '../../../services/ng1/root-scope.ng1.service';
import { Ng1SiteMapService, NG1_SITE_MAP_SERVICE } from '../../../services/ng1/site-map.ng1.service';
import { SiteMapItem } from '../../models/site-map-item.model';
import { SiteMapService } from '../../services/site-map.service';

interface SiteMapItemNode extends SiteMapItem {
  expandable: boolean;
  level: number;
}

@Component({
  selector: 'em-site-map',
  templateUrl: './site-map.component.html',
  styleUrls: ['./site-map.component.scss'],
})
export class SiteMapComponent implements OnChanges, OnInit, OnDestroy {
  @Input() pathInfo?: string;

  private readonly treeFlattener = new MatTreeFlattener(
    (node: SiteMapItem, level: number) => ({
      ...node,
      level,
    }), node => node.level, node => node.expandable, node => node.children,
  );

  readonly treeControl = new FlatTreeControl<SiteMapItemNode>(node => node.level, node => node.expandable);
  readonly dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  readonly search$ = new Subject<string>();

  expandedNodes: Set<SiteMapItemNode> = new Set([]);
  searchQuery = '';
  subscriptions = new Subscription();
  shouldExpandSelectedNode = false;

  private readonly onLoadSiteMapUnsubscribe: () => void;

  constructor(
    @Inject(NG1_ROOT_SCOPE) private readonly $rootScope: ng.IRootScopeService,
    @Inject(NG1_CONFIG_SERVICE) private readonly ng1ConfigService: Ng1ConfigService,
    @Inject(NG1_CHANNEL_SERVICE) private readonly ng1ChannelService: Ng1ChannelService,
    @Inject(NG1_SITE_MAP_SERVICE) private readonly ng1SiteMapService: Ng1SiteMapService,
    private readonly iframeService: IframeService,
    private readonly siteMapService: SiteMapService,
    private readonly zone: NgZone,
    private readonly elementRef: ElementRef,
  ) {
    this.search$
      .pipe(
        debounceTime(1000),
        tap(value => {
          this.searchQuery = value;
          return value;
        }),
        map<string, [string]>(query => [query]),
        distinctUntilChanged((prevQuery, query) => query === prevQuery),
        pluck(0),
      )
      .subscribe(this.onSearch.bind(this));

    this.onLoadSiteMapUnsubscribe = this.$rootScope.$on('load-site-map', (event, pathInfo) => {
      this.zone.run(() => {
        this.shouldExpandSelectedNode = true;
        const path = pathInfo ? `/${pathInfo}` : '/';
        this.loadSiteMap(path, true);
      });
    });
  }

  ngOnInit(): void {
    const siteMapSubscription = combineLatest(
      this.siteMapService.search$,
      this.siteMapService.items$,
    ).subscribe(([search, items]) => {
      if (this.isSearchMode) {
        this.dataSource.data = search[0]?.children?.length ? search : [];
        this.expandNodesWithChildren(this.treeControl.dataNodes);
      } else {
        this.dataSource.data = items;
        this.restoreExpandedNodes();
        this.saveExpandedNodes();
      }
    });

    this.subscriptions.add(siteMapSubscription);
  }

  ngOnChanges(changes: any): void {
    const selectedNode = this.treeControl.dataNodes.find(this.isSelected.bind(this));

    if (changes.pathInfo.firstChange) {
      this.shouldExpandSelectedNode = true;
      this.loadSiteMap(changes.pathInfo.currentValue);
    }
    if (changes.pathInfo && selectedNode) {
      this.expandSelectedNode();
      this.shouldExpandSelectedNode = true;
      this.toggleNode(selectedNode);
    }
  }

  ngOnDestroy(): void {
    this.search$.complete();
    this.subscriptions.unsubscribe();
    this.onLoadSiteMapUnsubscribe();
  }

  get isSearchMode(): boolean {
    return !!this.searchQuery && this.searchQuery.length > 2;
  }

  isSelected({ pathInfo }: SiteMapItem): boolean {
    const path = this.pathInfo || '/';
    return pathInfo === path || `/${pathInfo}` === path;
  }

  async selectNode(node: SiteMapItemNode): Promise<void> {
    if (node.pathInfo) {
      this.shouldExpandSelectedNode = true;
      await this.iframeService.load(node.pathInfo);
    }
  }

  toggleNode(node: SiteMapItemNode): void {
    this.saveExpandedNodes();
    if (node.expandable && !node.children.length) {
      this.loadSiteMapChildren(node);
    }
  }

  shouldDisplayTree(searchQuery: string): boolean {
    return !searchQuery || (!!searchQuery && searchQuery.length > 2);
  }

  shouldDisplayValidationText(searchQuery: string): boolean {
    return !!searchQuery && !!searchQuery.length && searchQuery.length < 3;
  }

  private loadSiteMap(pathInfo?: string, noMerge = false): void {
    const siteMapId = this.ng1ChannelService.getSiteMapId();
    const path = pathInfo || '/';
    if (path === '/') {
      this.expandedNodes.clear();
    }
    if (path.startsWith('/')) {
      path.slice(0, 1);
    }
    this.siteMapService.loadItem(siteMapId, path, false, true, noMerge);
  }

  private loadSiteMapChildren(node: SiteMapItemNode): void {
    const siteMapId = this.ng1ChannelService.getSiteMapId();
    const parentPath = this.getParentPath(node);
    this.siteMapService.loadItem(siteMapId, `${parentPath}${node.id}`, this.isSearchMode);
  }

  private onSearch(value: string): void {
    const siteMapId = this.ng1ChannelService.getSiteMapId();
    if (this.isSearchMode) {
      this.siteMapService.search(siteMapId, value);
    } else if (!value) {
      this.shouldExpandSelectedNode = true;
      this.loadSiteMap(this.pathInfo);
    }
  }

  private getParentPath(node: SiteMapItemNode): string {
    const parents = this.getParents(node);
    const parentsIds = parents.map(parent => parent.id).filter(path => path !== '/');
    return parentsIds.join('/');
  }

  private getParents(child: SiteMapItemNode): SiteMapItemNode[] {
    const parents = [];
    const { dataNodes, getLevel } = this.treeControl;

    for (let i = dataNodes.indexOf(child), childLevel = getLevel(child); i >= 0; i--) {
      const node = this.treeControl.dataNodes[i];
      const level = this.treeControl.getLevel(node);

      if (level < childLevel) {
        parents.push(node);
        childLevel = level;
      }
    }

    return parents;
  }

  private saveExpandedNodes(): void {
    this.treeControl.dataNodes.forEach(node => {
      if (this.treeControl.isExpanded(node)) {
        this.expandedNodes.add(node);
      } else {
        this.deleteExpandedNode(node);
      }
    });
  }

  private deleteExpandedNode(node: SiteMapItem): void {
    this.expandedNodes.forEach(expandedNode => {
      if (expandedNode.id === node.id) {
        this.expandedNodes.delete(expandedNode);
      }
    });
  }

  private restoreExpandedNodes(): void {
    this.expandedNodes.forEach(node => {
      const expandedNode = this.treeControl.dataNodes.find(n => n.id === node.id);
      if (expandedNode) {
        this.treeControl.expand(expandedNode);
      }
    });

    this.treeControl.expand(this.treeControl.dataNodes[0]);

    if (this.shouldExpandSelectedNode) {
      this.expandSelectedNode();
      this.shouldExpandSelectedNode = false;
    }
  }

  private expandNodesWithChildren(nodes: SiteMapItemNode[]): void {
    nodes.forEach(node => {
      if (node.children.length) {
        this.treeControl.expand(node);
      }
    });
  }

  private expandSelectedNode(): void {
    const node = this.treeControl.dataNodes.find(this.isSelected.bind(this));
    if (node) {
      this.expandNode(node);
    }
    setTimeout(() => {
      this.scrollToSelectedNode();
    }, 500);
  }

  private expandNode(node: SiteMapItemNode): void {
    this.treeControl.expand(node);
    this.getParents(node).forEach(parent => {
      this.treeControl.expand(parent);
    });
  }

  private scrollToSelectedNode(): void {
    const selectedNode = this.elementRef.nativeElement.querySelector('.selected');
    selectedNode?.scrollIntoView();
  }
}
