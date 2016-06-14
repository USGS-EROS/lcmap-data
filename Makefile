VERSION=0.5.0
PROJECT=lcmap-data
STANDALONE=target/$(PROJECT)-$(VERSION)-SNAPSHOT-standalone.jar
ROOT_DIR = $(shell pwd)

include resources/make/code.mk
include resources/make/data.mk
include resources/make/docs.mk
include resources/make/docker.mk
