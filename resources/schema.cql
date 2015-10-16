CREATE KEYSPACE IF NOT EXISTS lcmap WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'DC1' : 1};

CREATE TABLE IF NOT EXISTS lcmap.epsg_5070 (
    x int,
    y int,
    layer text,
    acquired timestamp,
    data blob,
    data_fill int static,
    data_range list<int> static,
    data_scale float static,
    data_shape list<int> static,
    data_type text static,
    source text,
    PRIMARY KEY ((x, y, layer), acquired)
) WITH CLUSTERING ORDER BY (acquired DESC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
    AND compression = {'chunk_length_kb': '64', 'sstable_compression': 'org.apache.cassandra.io.compress.SnappyCompressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';

CREATE TABLE IF NOT EXISTS lcmap.tile_groups (
    table_name text,
    projection text static,
    pixel_units text static,
    pixel_x float static,
    pixel_x_offset float static,
    pixel_y float static,
    pixel_y_offset float static,
    resample_method text static,
    band text,
    satellite text,
    instrument text,
    product text,
    category text,
    long_name text,
    short_name text,
    spectrum list<float>,
    data_fill int,
    data_range list<int>,
    data_scale float,
    data_type text,
    data_units text,
    class_values map<int, text>,
    PRIMARY KEY (table_name, band));
