ORG = "usgseros"
REPO = "lcmap-data"
COMMIT = $(shell git rev-parse HEAD)
IMAGE = $(ORG)/$(REPO):$(COMMIT)

#
# XXX: Beware!
#
# Well... the image variable contains the most recent commit.
# But these still let someone build and push an image using
# a dirty repository... not sure how much hand holding we need
# to do?
#

.PHONY: docker

docker-build:
	@docker build docker/lcmap-data-docker --tag $(IMAGE)

docker-publish: docker-build
	@docker push $(IMAGE)

docker-bash:
	@docker run -it $(IMAGE) /bin/bash

docker-repl:
	@docker run -it $(IMAGE) --entrypoint=lein repl

docker-clean:
	-@docker rm $(shell docker ps -a -q)
	-@docker rmi $(shell docker images -q --filter 'dangling=true')
