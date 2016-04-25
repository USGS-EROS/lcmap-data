CREATE KEYSPACE IF NOT EXISTS lcmap WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'DC1' : 1};

-- Tiles for CONUS
CREATE TABLE IF NOT EXISTS lcmap.conus (
    x int,
    y int,
    ubid text,
    acquired timestamp,
    data blob,
    source text,
    PRIMARY KEY ((x, y, ubid), acquired)
);

-- Tile specifications/metadata
CREATE TABLE IF NOT EXISTS lcmap.tile_specs (
    keyspace_name text,
    table_name text,
    projection text static,
    tile_x int static,
    tile_y int static,
    pixel_x float static,
    pixel_y float static,
    shift_x float static,
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
CREATE INDEX ubid_ix ON lcmap.tile_specs (ubid);

-- Scene Inventory

CREATE TABLE lcmap.scenes (
    source text,
    ubid text,
    global_metadata text static,
    PRIMARY KEY ((source), ubid)
);


-- Job Management (Science Model Execution Tracking)
CREATE TABLE lcmap.job (
    job_id text PRIMARY KEY,
    status int,
    science_model_name text,
    execution_parameters text,
    result_keyspace text,
    result_table text,
    result_id text,

) WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
  AND COMMENT = 'Science model job results tracking and linking.';

-- Results table for sample model
CREATE TABLE lcmap.samplemodel (
    result_id text PRIMARY KEY,
    result text

) WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
  AND COMMENT = 'Sample model results table.';

-- Results table for CCDC model
CREATE TABLE lcmap.ccdcmodel (
    result_id text PRIMARY KEY,
    result text

) WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
  AND COMMENT = 'CCDC model results table.';
