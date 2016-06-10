build: clean
	@lein compile
	@lein uberjar

standalone: build
	java -jar $(STANDALONE)

standalone-heavy: build
	java -Xms3072m -Xmx3072m -jar $(STANDALONE)

shell:
	@lein repl

repl:
	@lein repl

clean-all: clean clean-docs clean-docker

clean:
	@rm -rf target
	@rm -f pom.xml

deps-tree:
	@lein pom
	@mvn dependency:tree

loc:
	@find src -name "*.clj" -exec cat {} \;|wc -l

check: setup-test
	@lein with-profile

run:
	-@lein trampoline run

setup-dev:
	lein lcmap run-cql --file resources/schema.cql

setup-test:
	lein lcmap run-cql --file resources/schema.cql
	lein lcmap make-specs test/data/ESPA/CONUS/ARD/LT50460282002026-SC20160608172634.tar.gz --tile-table conus --tile-keyspace lcmap --tile-size 128:128
	lein lcmap make-specs test/data/ESPA/CONUS/ARD/LE70460272002002-SC20160608172749.tar.gz --tile-table conus --tile-keyspace lcmap --tile-size 128:128
	lein lcmap make-tiles test/data/ESPA/CONUS/ARD/*.tar.gz


setup: setup-dev setup-test
