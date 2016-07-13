FROM usgseros/ubuntu-gis-clj:0.5.0
MAINTAINER USGS LCMAP http://eros.usgs.gov
LABEL Description="This image is used to initialize the LCMAP DB schema and ingest ARD"

VOLUME ["/root/.usgs","/app/data"]

WORKDIR /app
RUN git clone https://github.com/USGS-EROS/lcmap-data

WORKDIR /app/lcmap-data
RUN lein deps

# Temporary fix for a broken dependency on prismatic/schema
RUN mkdir /app/lcmap-data/checkouts
WORKDIR /app/lcmap-data/checkouts
RUN git clone https://github.com/plumatic/schema.git
