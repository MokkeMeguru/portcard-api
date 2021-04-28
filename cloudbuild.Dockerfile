FROM clojure:openjdk-15-lein as builder
MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
# Set WORKDIR
ENV APP_HOME /app
COPY . $APP_HOME
WORKDIR $APP_HOME
# Load ARGS
ARG GSM_SECRET
ARG GSM_CREDENTIALS
ARG GSM_PROFILES
ARG GSM_TOKENS

RUN apt-get upgrade -y \
    && apt-get update -y \
    && apt-get install gnupg -y

ENV GSM_SECRET=$GSM_SECRET
ENV GSM_CREDENTIALS=$GSM_CREDENTIALS
ENV GSM_PROFILES=$GSM_PROFILES
ENV GSM_TOKENS=$GSM_TOKENS
ENV GMAIL_TOKENS_DIR=$APP_HOME/resources/tokens
ENV GMAIL_CREDENTIAL_FILE=$APP_HOME/resources/credentials.json
ENV GOOGLE_APPLICATION_CREDENTIALS=$APP_HOME/resources/secret.json

ENV LANG C.UTF-8
RUN lein deps
RUN ["/bin/bash", "./cloudbuild.sh"]

FROM openjdk:15-slim as app
ENV APP_HOME /app

WORKDIR $APP_HOME
ENV GOOGLE_APPLICATION_CREDENTIALS=$APP_HOME/resources/secret.json
COPY --from=builder /app/resources $APP_HOME/resources
COPY --from=builder /app/.lein-env $APP_HOME/.lein-env
ENV CONFIG_FILE=cloud_config.edn
COPY --from=builder /app/target/*-standalone.jar $APP_HOME/app.jar
CMD ["java", "-Xmx512m", "-Xms512m", "-jar", "app.jar"]
