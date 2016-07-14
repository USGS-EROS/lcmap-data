# lcmap.data

*Data ingest and access for LCMAP*


[![Build Status][travis-badge]][travis][![Dependencies Status][deps-badge]][deps][![Clojars Project][clojars-badge]][clojars]

[![LCMAP open source project logo][lcmap-logo]][lcmap-logo-large]


#### Contents

* [Concepts](#concepts-)
* [Documentation](#documentation-)
* [Dependencies](#dependencies-)
* [Configuration](#configuration-)
* [Usage](#usage-)
  * [1. Prepare the Schema](#1-prepare-the-schema-)
  * [2. Create Tile Specs](#2-create-tile-specs-)
  * [3. Create Tiles](#3-create-tiles-)
* [License](#license-)


## Concepts [&#x219F;](#contents)

This project is used to prepare a Cassandra cluster with a schema, define
specifications for tiles, and create tiles from ESPA archives.

A _tile-spec_ defines the projection system, tile size, pixel sizes, and unique
band identifiers that a _tile_ table contains. It provides a way to ensure that
all of the data contained in a table is consistent, to determine what tile
contains an arbitrary point, and to reconstiture blobs of data into a meaningful
type of data.


## Dependencies [&#x219F;](#contents)

This project depends on a working GDAL installation with compiled JAVA SWIG
bindings. Set `LD_LIBRARY_PATH` to the directory that contains
`libgdalconstjni.so`, `libgdaljni.so`, `libogrjni.so`, and `libosrjni.so`.

This project will create tiles from an archive obtained from the
[USGS ESPA system](http://espa.cr.usgs.gov/). It does not currently support
tiling of arbitrary geospatial data (GeoTIFF, HDF, etc...) although it may in the
future.


## Documentation [&#x219F;](#contents)

The LCMAP data API reference is slowly being updated with docstrings.
The project's auto-generated documentation is available here:

* [http://usgs-eros.github.io/lcmap-event](http://usgs-eros.github.io/lcmap-event)


## Configuration [&#x219F;](#contents)

Add an lcmap.data section to ~/.usgs/lcmap.ini

```ini
[lcmap.data]

db-hosts = host1, host2, host3
db-user = cluster-username
db-pass = cluster-password
spec-keyspace = lcmap
spec-table = tile_specs
scene-keyspace = lcmap
scene-table = tile_scenes
```


## Usage [&#x219F;](#contents)

Three commands are used to prepare a schema and import data into a Cassandra
cluster.


### 1. Prepare the Schema [&#x219F;](#contents)

Create a keyspace, tile spec table, and tile table.

```
$ lein lcmap run-cql --file resources/schema.cql
```


### 2. Create Tile Specs [&#x219F;](#contents)

Create tile specs, inferring properties from an ESPA archive. Currently,
this will assume the target tile keyspace and table are "lcmap" and "conus".
Eventually, these will be required command line parameters.

You must do this once for Landsat 5, 7, and 8 archives.

```
$ lein lcmap make-specs ~/Downloads/LC80460272013104-SC20151208193402.tar.gz --tile-keyspace lcmap --tile-table conus --tile-size 128:128
$ lein lcmap make-specs ~/Downloads/LE70460272002354-SC20151208192943.tar.gz --tile-keyspace lcmap --tile-table conus --tile-size 128:128
$ lein lcmap make-specs ~/Downloads/LT50460271992159-SC20151208192831.tar.gz --tile-keyspace lcmap --tile-table conus --tile-size 128:128
```


### 3. Create Tiles [&#x219F;](#contents)

```
$ lein lcmap make-tiles ~/Downloads/LC80460272013104-SC20151208193402.tar.gz
$ lein lcmap make-tiles ~/Downloads/LE70460272002354-SC20151208192943.tar.gz
$ lein lcmap make-tiles ~/Downloads/LT50460271992159-SC20151208192831.tar.gz
```

Ingesting tiles will gracefully fail if you attempt to ingest data that does not
conform to the corresponding tile specification. Currently, the tiling command
only works with archives, it does not handle paths to decompressed archives yet.


## Development [&#x219F;](#contents)

The followiong `make` target will create a separate keyspace for dev and test. It
will also create a test specific configuration from an example in
`test/support/lcmap.test.ini`.

```
$ make setup
```


## License [&#x219F;](#contents)

Copyright © 2015 United States Government

NASA Open Source Agreement, Version 1.3


<!-- Named page links below: /-->

[travis]: https://travis-ci.org/USGS-EROS/lcmap-data
[travis-badge]: https://travis-ci.org/USGS-EROS/lcmap-data.png?branch=master
[deps]: http://jarkeeper.com/usgs-eros/lcmap-data
[deps-badge]: http://jarkeeper.com/usgs-eros/lcmap-data/status.svg
[lcmap-logo]: https://raw.githubusercontent.com/USGS-EROS/lcmap-system/master/resources/images/lcmap-logo-1-250px.png
[lcmap-logo-large]: https://raw.githubusercontent.com/USGS-EROS/lcmap-system/master/resources/images/lcmap-logo-1-1000px.png
[clojars]: https://clojars.org/gov.usgs.eros/lcmap-data
[clojars-badge]: https://img.shields.io/clojars/v/gov.usgs.eros/lcmap-data.svg
[tag-badge]: https://img.shields.io/github/tag/usgs-eros/lcmap-data.svg?maxAge=2592000
[tag]: https://github.com/usgs-eros/lcmap-data/tags
