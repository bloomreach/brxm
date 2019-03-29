module.exports = {
  "presets": [
    ["@babel/preset-env", {
      "modules": false,
      "targets": {
        "browsers": [
          "last 1 chrome version",
          "last 1 firefox version",
          "last 1 safari version",
          "last 1 edge version"
        ]
      }
    }]
  ],
  "plugins": [
    "@babel/plugin-transform-object-assign",
    "@babel/plugin-proposal-class-properties",
    "@babel/plugin-proposal-object-rest-spread",
    ["babel-plugin-transform-async-to-promises", {
      "inlineHelpers": true
    }]
  ]
}
