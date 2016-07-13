ORG = "usgseros"
REPO = "lcmap-data"
COMMIT = $(shell git rev-parse HEAD)
IMAGE = $(ORG)/$(REPO):$(COMMIT)

.PHONY: docker

docker-build:
	@docker build  -t $(IMAGE) docker/lcmap-data-docker

docker-publish: docker-build
	@docker push $(IMAGE)

docker-bash:
	@docker run -it $(IMAGE) /bin/bash

docker-repl:
	@docker run -it $(IMAGE) --entrypoint=lein repl

docker-clean:
	-@docker rm $(shell docker ps -a -q)
	-@docker rmi $(shell docker images -q --filter 'dangling=true')
