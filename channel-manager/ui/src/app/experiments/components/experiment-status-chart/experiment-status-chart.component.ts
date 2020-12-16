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

import { Component, ElementRef, Input, OnInit } from '@angular/core';
import * as c3 from 'c3';
import * as d3 from 'd3';

import { ExperimentStatusAtTimestampWithVisits } from '../../models/experiment-status-with-visits.model';
import { ExperimentStatusAtTimestamp } from '../../models/experiment-status.model';
import { ExperimentWithStatusData } from '../../models/experiment-with-status-data.model';

interface AxisDayRegion {
  axis: 'x';
  start: Date;
  end: Date;
}

@Component({
  selector: 'em-experiment-status-chart',
  template: '',
  styleUrls: ['experiment-status-chart.component.scss'],
})
export class ExperimentStatusChartComponent implements OnInit {
  @Input()
  experiment!: ExperimentWithStatusData;

  private chart!: c3.ChartAPI;

  constructor(private readonly elRef: ElementRef<HTMLElement>) {}

  get defaultChartConfiguration(): Partial<c3.ChartConfiguration> {
    return {
      bar: {width: {ratio: 0.8}},
      tooltip: {
        show: true,
        position: () => ({top: 0, left: 0}),
        format: {
          title: d3.timeFormat('%d/%m %H:%M') as (x: c3.Primitive, index: number) => string,
          value: (value: number) => `${value} %`,
        },
      },
      axis: {
        x: {
          height: 60,
          type: 'timeseries',
          tick: {
            rotate: 45,
            format: this.formatXAxis(),
          },
        },
        y: {
          max: 100,
          padding: {
            top: 0,
          },
          show: false,
        },
      },
      padding: {
        left: 0,
      },
      legend: {
        show: true,
      },
      zoom: {enabled: true},
      size: {
        height: 200,
      },
    };
  }

  get dataTemplate(): c3.Data {
    return {
      json: [],
      keys: {value: this.variantIds, x: 'timestamp'},
      names: this.variantNames,
      type: 'area-step',
      order: 'asc',
    };
  }

  get variantIds(): string[] {
    return this.experiment.variants.map(v => v.variantId);
  }

  get variantNames(): { [key: string]: string } {
    return this.experiment.variants.reduce((names: { [key: string]: string }, v) => {
      let variantName = v.variantName;
      const mean = 100 * v.mean;
      const sigma = 100 * Math.sqrt(v.variance);

      if (mean >= 0 && sigma < 2.5) {
        variantName += ` (${mean.toFixed(1)}%)`;
      }

      names[v.variantId] = variantName;

      return names;
    }, {});
  }

  ngOnInit(): void {
    if (!this.experiment.statusWithVisits) {
      return;
    }

    this.chart = c3.generate({
      bindto: this.elRef.nativeElement,
      data: this.dataTemplate,
      ...this.defaultChartConfiguration,
    });

    this.updateData(this.experiment.statusWithVisits);
  }

  private formatXAxis(): string {
    const start = this.midnight(new Date(this.experiment.startTime));
    const lastWeek = this.midnight(this.incrementDate(new Date(), -7));
    const now = this.midnight(new Date());

    if (start >= now) {
      return '%H:%M';
    }

    if (start <= lastWeek) {
      return '%d/%m';
    }

    return '%d/%m %H:%M';
  }

  private updateData(points: ExperimentStatusAtTimestampWithVisits[]): void {
    const data = this.dataTemplate;
    data.json = this.normalizeData(points);

    this.chart.load(data as any);
    this.chart.groups([this.variantIds]);
    this.chart.regions(this.dayRegions(points));
  }

  private normalizeData(points: ExperimentStatusAtTimestampWithVisits[]): ExperimentStatusAtTimestamp[] {
    return points.map(point => {
      const normalizedPoint: ExperimentStatusAtTimestamp = { timestamp: point.timestamp };
      const keys = Object.keys(point).filter(x => x !== 'timestamp');

      if (point.visits === 0 || keys.length === 0) {
        keys.forEach(key => {
          normalizedPoint[key] = point[key];
        });

        return normalizedPoint;
      }

      let newNormalizedVisits = 0;
      let maxKey: string = keys[0];
      let maxValue = 0;

      for (const key of keys) {
        const normalizedValue = Math.round((100 * point[key]) / point.visits);
        normalizedPoint[key] = normalizedValue;
        newNormalizedVisits += normalizedValue;

        if (normalizedPoint[key] > maxValue) {
          maxKey = key;
          maxValue = normalizedPoint[key];
        }
      }

      if (newNormalizedVisits > 100) {
        normalizedPoint[maxKey] -= (newNormalizedVisits - 100);
      }

      return normalizedPoint;
    });
  }

  /**
   * Create a list of {axis:'x', start:Date1, end:Date2} objects where Date1 and Date2 span
   * every other day of the time range covered by the data points, in such a way that the
   * date of the first data point is not covered.
   *
   * For example, if the points start at 2015-2-9 10:15 and end at 2015-2-12 8:45,
   * this function returns the spans 2015-2-10 0:00 .. 2015-2-11 0:00 and
   * 2015-2-12 0:00 .. 2015-2-13 0:00.
   *
   * This can be used to indicate day boundaries using a zebra pattern.
   * We make the regions start on the second day because that looks nicer when there's only
   * a single day.
   */
  private dayRegions(points: ExperimentStatusAtTimestampWithVisits[]): AxisDayRegion[] {
    if (!points || points.length === 0) {
      return [];
    }

    const startDate = this.midnight(new Date(points[0].timestamp));
    const endDate = this.incrementDate(new Date(points[points.length - 1].timestamp), 1);
    const regions = [];

    for (let d = this.incrementDate(startDate, 1); d < endDate; d = this.incrementDate(d, 2)) {
      regions.push(
        { axis: 'x', start: d, end: this.incrementDate(d, 1) } as AxisDayRegion,
      );
    }

    return regions;
  }

  /**
   * Rounds the given datetime to whole-day granularity and adds the given number of days to it.
   */
  private incrementDate(date: Date, increment: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + increment);
  }

  private midnight(datetime: Date): Date {
    return this.incrementDate(datetime, 0);
  }
}
