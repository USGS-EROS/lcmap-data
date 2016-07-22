PROJECT=lcmap-data
VERSION=0.5.1-SNAPSHOT
STANDALONE=target/$(PROJECT)-$(VERSION)-standalone.jar
ROOT_DIR = $(shell pwd)

include resources/make/code.mk
include resources/make/data.mk
include resources/make/docs.mk
include resources/make/docker.mk
