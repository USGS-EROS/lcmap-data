# lcmap.data

## Configuration

This project depends on a working GDAL installation with compiled JAVA SWIG bindings.
Set `LD_LIBRARY_PATH` to the directory that contains `libgdalconstjni.so`, `libgdaljni.so`
`libogrjni.so`, and `libosrjni.so`.

This project will create tiles from an archive obtained from the
[USGS ESPA system](http://espa.cr.usgs.gov/). It does not currently support tiling of
arbitrary geospatial data (GeoTIFF, HDF, etc...) although it may in the future.


## Concepts

This project is used to prepare a Cassandra cluster with a schema, define specifications
for tiles, and create tiles from ESPA archives.

A _tile-spec_ defines the projection system, tile size, pixel sizes, and unique band
identifiers that a _tile_ table contains. It provides a way to ensure that all of the
data contained in a table is consistent, to determine what tile contains an arbitrary
point, and to reconstiture blobs of data into a meaningful type of data.

## Configuration

Add an lcmap.data section to ~/.usgs/lcmap.ini

```
[lcmap.data]
db-hosts = host1, host2, host3
db-user = cluster-username
db-pass = cluster-password
spec-keyspace = lcmap
spec-table = tile_specs
scene-keyspace = lcmap
scene-table = tile_scenes
```


## Usage

Three commands are used to prepare a schema and import data into a Cassandra cluster.

### 1. Prepare the Schema

Create a keyspace, tile spec table, and tile table.

```
lein lcmap run-cql --file resources/schema.cql --hosts 192.168.33.20
```

### 2. Create Tile Specs

Create tile specs, inferring properties from an ESPA archive. Currently,
this will assume the target tile keyspace and table are "lcmap" and "conus".
Eventually, these will be required command line parameters.

You must do this once for Landsat 5, 7, and 8 archives.

```
lein lcmap make-specs ~/Downloads/LC80460272013104-SC20151208193402.tar.gz --hosts 192.168.33.20 --tile-keyspace lcmap --tile-table conus --tile-size 256:256
lein lcmap make-specs ~/Downloads/LE70460272002354-SC20151208192943.tar.gz --hosts 192.168.33.20 --tile-keyspace lcmap --tile-table conus --tile-size 256:256
lein lcmap make-specs ~/Downloads/LT50460271992159-SC20151208192831.tar.gz --hosts 192.168.33.20 --tile-keyspace lcmap --tile-table conus --tile-size 256:256
```

### 3. Create Tiles

```
lein lcmap make-tiles ~/Downloads/LC80460272013104-SC20151208193402.tar.gz --hosts 192.168.33.20
lein lcmap make-tiles ~/Downloads/LE70460272002354-SC20151208192943.tar.gz --hosts 192.168.33.20
lein lcmap make-tiles ~/Downloads/LT50460271992159-SC20151208192831.tar.gz --hosts 192.168.33.20
```

Ingesting tiles will gracefully fail if you attempt to ingest data that does not conform to the corresponding tile specification. Currently, the tiling command only works with archives, it does not handle paths to decompressed archives yet.

## Development

This will create a separate keyspace for dev and test. It will also create a test specific configuration from an example in `test/support/lcmap.test.ini`.

```
make setup
```


## License

Copyright © 2015 United States Government

NASA Open Source Agreement, Version 1.3
