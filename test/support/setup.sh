#!/usr/bin/env bash

# Configuration
mkdir -p ~/.usgs/
cp test/support/lcmap.test.ini.example ~/.usgs/lcmap.test.ini

# Cassandra (not available by default on Travis' Trusty build)
# curl --location --silent -L https://debian.datastax.com/debian/repo_key | sudo apt-key add -
# sudo apt-get install -y -qq dsc21
