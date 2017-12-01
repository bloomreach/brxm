/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
const jsonfile = require('jsonfile');
const colors = require('colors');
const clui = require('clui');

function main() {
    const report = jsonfile.readFileSync('./export/report.json');
    const metadata = report.metadata;

    printConsoleReport(metadata);
}
main();

function printConsoleReport(metadata) {
    const lines = [
        `==================[ CONVERSION REPORT ]==================`,
        `ORIGIN: `.green + `${metadata.origin.name}\t\t\t` + `FILES:\t`.green + `${metadata.counts.origin}`,
        `TARGET: `.green + `${metadata.target.name}\t\t\t` + `FILES:\t`.green + `${metadata.counts.target}`,
        `\t\t\t\t\tTOTAL:\t`.green + `${metadata.counts.total}`,
        ``,        
        getProgressBar(metadata.counts),
        `=========================================================`        
    ];

    lines.forEach((line) => console.log(line));
}

function getProgressBar(counts) {
    let progress = new clui.Progress(58);
    progress = progress.update(counts.target, counts.total);
    return progress;
}