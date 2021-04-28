#!/usr/bin/env bash

# 1. compile the code and generate a pom.xml
lein do compile :all, pom

# 2. containerize with jib
mvn compile jib:build

