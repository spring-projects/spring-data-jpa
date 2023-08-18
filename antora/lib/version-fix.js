'use strict'

/**
 * The purpose of this extension is to fix invalid metadata saved to either antora.yml or gradle.properties in certain
 * tags. This invalid metadata prevents Antora from classifying the component versions properly.
 *
 * This extension should be listed directly after @antora/collector-extension.
 */
 module.exports.register = function () {
   this.once('contentAggregated', ({ contentAggregate }) => {
       contentAggregate.forEach((componentVersionBucket) => {
         if (componentVersionBucket.version === '3.2.0-SNAPSHOT') {
           componentVersionBucket.version = '3.2.0'
           componentVersionBucket.prerelease = '-SNAPSHOT'
         }
       })
     })
 }