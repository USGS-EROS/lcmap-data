ORG = "usgseros"
REPO = "lcmap-data"
IMAGE = $(ORG)/$(REPO):$(VERSION)
CONFIG = "/home/jmorton/.usgs:/root/.usgs"
DATA = "/home/jmorton/Projects/lcmap/data:/data"

.PHONY: docker

uberjar-build:
	@lein uberjar

uberjar-move: uberjar-build
	cp $(STANDALONE) docker/lcmap-data-docker/app.jar

docker-build: uberjar-move
	@docker build -t $(IMAGE) docker/lcmap-data-docker

docker-publish: docker-build
	@docker push $(IMAGE)

docker-bash:
	@docker run \
	--volume $(CONFIG) \
	--volume $(DATA) \
	--entrypoint=/bin/bash \
	-it $(IMAGE)

docker-info:
	@docker run \
	--volume $(CONFIG) \
	--volume $(DATA) \
	-it $(IMAGE) --info

docker-tile:
	@docker run \
	--volume $(CONFIG) \
	--volume $(DATA) \
	--entrypoint=/bin/bash \
	-it $(IMAGE) -c 'java -jar app.jar make-tiles /data/*.tar.gz'

docker-clean:
	-@docker rm $(shell docker ps -a -q)
	-@docker rmi $(shell docker images -q --filter 'dangling=true')

docker-push:
	@docker push $(IMAGE)
