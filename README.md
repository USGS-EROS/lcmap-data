# lcmap-data-clj

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

## Usage

Three commands are used to prepare a schema and import data into a Cassandra cluster.

### 1. Prepare the Schema

Create a keyspace, tile spec table, and tile table.

```
lein db exec --cql resources/schema.cql --hosts 192.168.33.20
```

### 2. Create Tile Specs

Create tile specs, inferring properties from an ESPA archive. Currently,
this will assume the target tile keyspace and table are "lcmap" and "conus".
Eventually, these will be required command line parameters.

You must do this once for Landsat 5, 7, and 8 archives.

```
lein db spec ~/Downloads/LC80460272013104-SC20151208193402.tar.gz --hosts 192.168.33.20
lein db spec ~/Downloads/LE70460272002354-SC20151208192943.tar.gz --hosts 192.168.33.20
lein db spec ~/Downloads/LT50460271992159-SC20151208192831.tar.gz --hosts 192.168.33.20
```

Please note: the tile spec code, although good enough for prototyping, assumes
you know exactly what you are doing. If you have inconsistently projected data
things will not work. However, it is likely that you will be given data that
doesn't have consistency problems.

### Ingest Some Data

```
lein db tile ~/Downloads/LC80460272013104-SC20151208193402.tar.gz --hosts 192.168.33.20
lein db tile ~/Downloads/LE70460272002354-SC20151208192943.tar.gz --hosts 192.168.33.20
lein db tile ~/Downloads/LT50460271992159-SC20151208192831.tar.gz --hosts 192.168.33.20
```

Tiling will gracefully fail if you attempt to ingest data that does not conform
to the corresponding tile specification. Currently, the tiling command only
works with archives, it does not handle paths to decompressed archives yet.

## License

Copyright © 2015-2016, USGS EROS

NASA Open Source Agreement, Version 1.3
