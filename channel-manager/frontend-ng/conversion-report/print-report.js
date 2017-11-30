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