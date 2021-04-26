#!/usr/bin/env bash
GSM_SECRET=${GSM_SECRET:-""}
GSM_CREDENTIALS=${GSM_CREDENTIALS:-""}
GSM_PROFILES=${GSM_PROFILES:-""}
GSM_TOKENS=${GSM_TOKENS:-""}

if type gcloud > /dev/null 2>&1; then
    GSM_SECRET=$(gcloud secrets versions access 1 --secret="secret") 
    GSM_CREDENTIALS=$(gcloud secrets versions access 1 --secret="credentials") 
    GSM_PROFILES=$(gcloud secrets versions access 1 --secret="profiles") 
    GSM_TOKENS=$(gcloud secrets versions access 1 --secret="tokens") 
fi

gpg --passphrase-fd 0 --decrypt --batch --no-secmem-warning resources/secret.json.gpg \
    <<< $GSM_SECRET > resources/secret.json
gpg --passphrase-fd 0 --decrypt --batch --no-secmem-warning resources/credentials.json.gpg \
    <<< $GSM_CREDENTIALS > resources/credentials.json
gpg --passphrase-fd 0 --decrypt --batch --no-secmem-warning profiles.clj.gpg \
    <<< $GSM_PROFILES > profiles.clj
gpg --passphrase-fd 0 --decrypt --batch --no-secmem-warning resources/tokens.tar.gz.gpg \
    <<< $GSM_TOKENS > resources/tokens.tar.gz

tar -zxvf resources/tokens.tar.gz -C resources

ls -la resources

lein with-profile cloud run
# gcloud builds submit --tag gcr.io/portcard/portcard-api -f cloudbuild.Dockerfile
