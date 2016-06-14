#!/usr/bin/env bash

# Configuration
mkdir ~/.usgs/
cp test/support/lcmap.test.ini.example ~/.usgs/lcmap.test.ini

# GDAL
sudo apt-add-repository ppa:ubuntugis/ppa -y
sudo apt-get update -qq
sudo apt-get install libgdal-dev libgdal-java -y

# Cassandra (not available by default on Travis' Trusty build)
curl --location --silent -L https://debian.datastax.com/debian/repo_key | sudo apt-key add -
sudo apt-get install -y -qq dsc21
