#!/usr/bin/env bash

sed -i 's/git@github.com:/https:\/\/github.com\//' .gitmodules
git submodule update --init --recursive

mkdir checkouts
cd checkouts && \
    git clone https://github.com/USGS-EROS/lcmap-config.git && \
    git clone https://github.com/USGS-EROS/lcmap-logger.git && \
    cd ../

mkdir ~/.usgs/
cp test/support/lcmap.test.ini.example ~/.usgs/lcmap.test.ini

# Cassandra (not available by default on Travis' Trusty build)
# curl --location --silent -L https://debian.datastax.com/debian/repo_key | sudo apt-key add -
# sudo apt-get install -y -qq dsc21
