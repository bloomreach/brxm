/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import {
  AfterContentInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { MatTree, MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';
import { Site } from '@bloomreach/navapp-communication';
import { PerfectScrollbarComponent } from 'ngx-perfect-scrollbar';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { SiteService } from '../../../services/site.service';
import { RightSidePanelService } from '../../services/right-side-panel.service';

interface SiteFlatNode {
  accountId: number;
  siteId: number;
  isNavappEnabled: boolean;
  expandable: boolean;
  name: string;
  level: number;
}

@Component({
  selector: 'brna-site-selection',
  templateUrl: 'site-selection.component.html',
  styleUrls: ['site-selection.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SiteSelectionComponent implements OnInit, AfterContentInit, OnDestroy {
  searchText = '';
  treeControl = new FlatTreeControl<SiteFlatNode>(
    node => node.level,
    node => node.expandable,
  );
  treeFlattener = new MatTreeFlattener(
    this.transformer,
    node => node.level,
    node => node.expandable,
    node => node.subGroups,
  );
  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  @ViewChild(PerfectScrollbarComponent, { static: true })
  scrollbar: PerfectScrollbarComponent;

  @ViewChild(MatTree, { static: true, read: ElementRef })
  treeElRef: ElementRef<HTMLElement>;

  private sites: Site[];
  private selectedSite: Site;
  private readonly unsubscribe = new Subject();

  constructor(
    private readonly siteService: SiteService,
    private readonly rightSidePanelService: RightSidePanelService,
    private readonly cd: ChangeDetectorRef,
  ) { }

  get isNotFoundPanelVisible(): boolean {
    return (
      this.dataSource.data &&
      this.dataSource.data.length === 0 &&
      !!this.searchText
    );
  }

  ngOnInit(): void {
    this.sites = this.siteService.sites;
    this.updateDataSource();

    this.siteService.selectedSite$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(x => {
      this.selectedSite = x;
      this.updateDataSource();
    });
  }

  ngAfterContentInit(): void {
    this.scrollToActiveNode();
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  hasChild(index: number, node: SiteFlatNode): boolean {
    return node.expandable;
  }

  onNodeClicked(node: SiteFlatNode): void {
    const site = {
      accountId: node.accountId,
      siteId: node.siteId,
      isNavappEnabled: node.isNavappEnabled,
      name: node.name,
    };

    this.siteService.updateSelectedSite(site).then(() => {
      this.rightSidePanelService.close();
    });
  }

  isActive(node: Site): boolean {
    return this.selectedSite ? (node.accountId === this.selectedSite.accountId && node.siteId === this.selectedSite.siteId) : false;
  }

  onSearchInputKeyUp(): void {
    this.updateDataSource();
  }

  private updateDataSource(): void {
    this.dataSource.data = this.filterSites(this.sites, this.searchText);
    if (this.searchText) {
      this.treeControl.expandAll();
    } else {
      this.expandActiveNode();
    }
  }

  private expandActiveNode(): void {
    if (!this.selectedSite) {
      this.treeControl.collapseAll();
      return;
    }

    const selectedNode = this.treeControl.dataNodes.find(
      x => x.accountId === this.selectedSite.accountId && x.siteId === this.selectedSite.siteId,
    );

    if (!selectedNode) {
      this.treeControl.collapseAll();
      return;
    }

    const path = this.buildPath(this.treeControl.dataNodes, selectedNode);

    path.forEach(node => this.treeControl.expand(node));
  }

  private filterSites(sites: Site[], searchText: string): Site[] {
    if (!sites || !sites.length) {
      return [];
    }

    searchText = searchText.toLowerCase();

    return sites.reduce((result, site) => {
      site = { ...site };

      if (site.name.toLowerCase().includes(searchText)) {
        result.push(site);
        return result;
      }

      site.subGroups = this.filterSites(site.subGroups, searchText);

      if (site.subGroups.length) {
        result.push(site);
      }

      return result;
    }, []);
  }

  private transformer(node: Site, level: number): SiteFlatNode {
    return {
      accountId: node.accountId,
      siteId: node.siteId,
      isNavappEnabled: node.isNavappEnabled,
      expandable: !!node.subGroups && node.subGroups.length > 0,
      name: node.name,
      level,
    };
  }

  private buildPath(nodes: SiteFlatNode[], node: SiteFlatNode): SiteFlatNode[] {
    const parents: SiteFlatNode[] = [];
    let currentNode = node;

    while (true) {
      const parent = this.getParentNode(nodes, currentNode);

      if (!parent) {
        break;
      }

      parents.unshift(parent);
      currentNode = parent;
    }

    return parents;
  }

  private getParentNode(
    nodes: SiteFlatNode[],
    node: SiteFlatNode,
  ): SiteFlatNode {
    const nodeIndex = nodes.indexOf(node);

    for (let i = nodeIndex - 1; i >= 0; i--) {
      if (nodes[i].level === node.level - 1) {
        return nodes[i];
      }
    }

    return undefined;
  }

  private scrollToActiveNode(): void {
    this.cd.detectChanges(); // Ensures the content of MatTree is checked

    const nodeEl: HTMLElement = this.treeElRef.nativeElement.querySelector('mat-tree-node.active');

    if (!nodeEl) {
      return;
    }

    this.scrollbar.directiveRef.scrollToY(nodeEl.offsetTop);
  }
}
