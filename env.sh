#!/usr/bin/env bash

# HOW TO USE; source env.sh
SECRET_ENV=.env

export GOOGLE_APPLICATION_CREDENTIALS=resources/secret.json

if test -f "$SECRET_ENV"; then
    source "$SECRET_ENV"
fi
