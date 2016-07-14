# lcmap.data

This project provides LCMAP developers and data curators with tools to produce
and retrieve tiles, tile-specs, and related metadata. It is intended to be used
as a dependency to projects that need to access tile data.

This overview explains some of the key concepts you should be familiar with
when working with this project.

## Tiles

A tile is a chunk of geospatial data. It has a few basic properties: ubid, x,
y, acquired, source, data.

The *UBID* (Universal Band IDentifier) is a string that contains information
about the mission, instrument, and collection. For example,
"LANDSAT_5\/TM\/sr_band1" refers to a tile that contains band 1 surface
reflectance collected by the Thematic Mapper aboard Landsat 5.

The *X* and *Y* coordinates,  two integers, are projection coordinate system
relative values that describe the upper-left point of the tile's upper-left
pixel. They often have a large magnitude for projections suitable for analysis
of wide areas. For example, the x- and y- coordinate for a point in Washington
state, when projected using an Albers Equal Area projection for CONUS, is
(-2062395,2968095).

The _acquired_ datetime is the moment in time when the satellite collected the
center pixel of the **scene**. Keep in mind that the scene a tile comes from is
much larger than the tile so this value shouldn't be used as the precise time
the tile's data was collected.

The _source_ identifies the precise scene from which a tile originates.

The _data_ is a raw blob of data that was extracted from a scene. In order to
find and make sense of this data, you'll need to use a tile-spec.

In general, tiles from related missions, with a common projection, shape, and
size; are stored in the same table.

## Tile Specs

Tile specs contain a variety of information that make it possible to find and
use tile data. Here are some questions that would be virtually impossible to
answer without them:

- What tile contains a specific point for a specific band and collection of
  data in a given projection coordinate system?
- What collections of data are available?
- What type of data is contained in a tile, what are the valid range of values,
  what value is used to indicate fill, and is there a scaling factor?

Using a tile-spec, you can find the tiles that contain data for an arbitrary
UBID and point. _Note: the system only supports one projection system, but
eventually you will need to specify the projection as well._ This requires
knowing the dimensions of the data, the magnitude of tiles, pixels, and shift
of upper-left points from the origin of the projection system.

## Scenes

Scene data is not currently stored, so there isn't much to say about this yet.

