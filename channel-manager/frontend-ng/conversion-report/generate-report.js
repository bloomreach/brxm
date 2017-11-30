const colors = require('colors');
const path = require('path');
const globToRegExp = require('glob-to-regexp');
const fs = require('fs');
const jsonfile = require('jsonfile');

const config = processConfiguration();

/* TODO: configuration */

let counts = {};

function main() {
  counts = {
   origin: 0,
   target: 0,
   total: 0
  };

  const exportedObject = {
    metadata: {
      origin: config.conversion.origin,
      target: config.conversion.target,
      counts: counts,
    },
    filetree: getDirectoryContents(config.src)
  };

  exportedObject.metadata.counts.total = exportedObject.metadata.counts.origin + exportedObject.metadata.counts.target;

  jsonfile.writeFile('./export/report.json', exportedObject, { spaces: 2 }, (err) => {
    if (err) {
      throw err;
    }

    console.log('Report exported');
  });
}
main();

function printReport(counts) {

}

function processFile(filePath) {
  const fileStats = fs.lstatSync(filePath);
  const fileName = path.basename(filePath);

  const file = {
    type: fileStats.isDirectory() === true ? 'dir' : 'file',
    name: fileName,
    path: filePath
  };

  if (file.type === 'file') {
    file.ext = path.extname(fileName);

    if(globToRegExp(config.conversion.origin.pattern).test(fileName)) counts.origin++;
    if(globToRegExp(config.conversion.target.pattern).test(fileName)) counts.target++;
  }

  if (file.type === 'dir') file.contents = getDirectoryContents(file.path);

  return file;
}

function getDirectoryContents(path, recursive = false) {
  const dirContents = fs.readdirSync(path);
  const relevantFiles = dirContents.filter(file => isItemRelevant(file, path));
  return relevantFiles.map(file => processFile(`${path}/${file}`));
}

function isItemRelevant(fileName, path) {
  const stats = fs.lstatSync(`${path}/${fileName}`);
  return !stats.isSymbolicLink() && checkIncludePatterns(fileName, stats);
}

function checkIncludePatterns(fileName, stats = null) {
  if(stats.isDirectory()) return true;
  return config.includedPatterns.some(pattern => globToRegExp(pattern).test(fileName));
}

function processConfiguration() {
  const importedConfig = jsonfile.readFileSync('./config.json');

  if(!importedConfig) {
    throw 'No configuration file found in "./config.json"!'
  }

  importedConfig.includedPatterns = [
    importedConfig.conversion.origin.pattern,
    importedConfig.conversion.target.pattern
  ];

  return importedConfig;
}