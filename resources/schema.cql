CREATE KEYSPACE IF NOT EXISTS lcmap WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'DC1' : 1};

CREATE TABLE IF NOT EXISTS lcmap.conus (
    x int,
    y int,
    ubid text,
    acquired timestamp,
    data blob,
    source text,
    PRIMARY KEY ((x, y, ubid), acquired)
);

CREATE TABLE IF NOT EXISTS lcmap.tile_specs (
    keyspace_name text,
    table_name text,
    projection text static,
    tile_x int static,
    tile_y int static,
    pixel_units text static,
    pixel_x float static,
    shift_x float static,
    pixel_y float static,
    shift_y float static,
    resample_method text static,
    ubid text,
    satellite text,
    instrument text,
    band_product text,
    band_category text,
    band_name text,
    band_long_name text,
    band_short_name text,
    band_spectrum list<double>,
    data_fill int,
    data_range list<int>,
    data_scale float,
    data_type text,
    data_units text,
    data_shape list<int> static,
    data_mask map<int, text>,
    PRIMARY KEY ((keyspace_name, table_name), ubid));

CREATE INDEX satellite_ix ON lcmap.tile_specs (satellite);
CREATE INDEX instrument_ix ON lcmap.tile_specs (instrument);
CREATE INDEX band_product_ix ON lcmap.tile_specs (band_product);
CREATE INDEX band_name_ix ON lcmap.tile_specs (band_name);
