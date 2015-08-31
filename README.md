## Overview

[Build Flow Test Aggregator](https://wiki.jenkins-ci.org/display/JENKINS/Build+Flow+Test+Aggregator+Plugin) is the simplest way to aggregate test results from builds started dynamically by the [build flow job](https://wiki.jenkins-ci.org/display/JENKINS/Build+Flow+Plugin). It's a post-build step that only shows up for build flow projects.

Normally to aggregate test results Jenkins has to "understand" the upstream - downstream dependencies between jobs. This can be achieved by generating an artifact in job1 and fingerprinting that archive from all the downstream jobs. But even then you'd have to have a separate job where you aggregate test results, which is not convenient.

Build Flow Test Aggregator simply loops over all of the builds scheduled by a flow run and retrieves and aggregates test results if there are any. The test results will be available just like any other test results, it will show aggregated results and statistics, with links to each individual test for further inspection:

<img src="http://cl.ly/image/2S1k221G1k0W/Image%202014-11-07%20at%201.37.40%20pm.png" />

## Configuring

To configure test aggregation you need to set the *"Flow run needs a workspace"* checkbox:

<img src="http://cl.ly/image/2E1p0Z1O2W1i/Image%202014-11-07%20at%201.33.17%20pm.png" />

Then just enable the *"Aggregate build flow test results"* post-build action:

<img src="http://cl.ly/image/3I2p213I3O3C/Image%202014-11-07%20at%201.21.02%20pm.png" />

----

# Changelog

## Version 1.2 (Aug 31, 2015)

* Added configurable test results trend chart
* Added support for aggregating test results from Maven module builds
* Fixed handling project renaming and deleting
* Fixed direct links to failed tests on test results summary page

## Version 1.1 (Apr 18, 2015)

* Fixed a NPE caused by not retrieving project by its full name
* Test results are now shown by the builds' full name, enables seeing build parameters etc right from the results page
* Added support for aggregating test results from MultiJob and matrix builds as well
* Added support for recursively aggregating test results from started flow jobs and MultiJob-s

## Version 1.0 (Nov 07, 2014)

* Initial release of the plugin