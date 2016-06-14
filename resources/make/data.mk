db-schema:
	lein lcmap run-cql --file resources/schema.cql

db-specs:
	lein lcmap make-specs test/data/ESPA/CONUS/ARD/LT50460282002026-SC20160608172634.tar.gz --tile-table conus --tile-keyspace lcmap --tile-size 128:128
	lein lcmap make-specs test/data/ESPA/CONUS/ARD/LE70460272002002-SC20160608172749.tar.gz --tile-table conus --tile-keyspace lcmap --tile-size 128:128

db-tiles:
	lein lcmap make-tiles test/data/ESPA/CONUS/ARD/*.tar.gz

db-setup: db-schema db-specs db-tiles
