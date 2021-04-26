FROM clojure:openjdk-11-lein
MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
ARG GSM_SECRET
ARG GSM_CREDENTIALS
ARG GSM_PROFILES
ARG GSM_TOKENS
ENV LANG C.UTF-8
ENV APP_HOME /app
ENV GSM_SECRET=$GSM_SECRET
ENV GSM_CREDENTIALS=$GSM_CREDENTIALS
ENV GSM_PROFILES=$GSM_PROFILES
ENV GSM_TOKENS=$GSM_TOKENS
ENV GMAIL_TOKENS_DIR=$APP_HOME/resources/tokens
ENV GMAIL_CREDENTIAL_FILE=$APP_HOME/resources/credentials.json
COPY . $APP_HOME
WORKDIR $APP_HOME
RUN apt-get update -y \
    && apt-get upgrade -y
RUN apt install -y gnupg
RUN lein deps
RUN ls -la resources
ENTRYPOINT ["/app/entrypoint.sh"]
