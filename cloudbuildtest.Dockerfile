FROM  openjdk:15-slim as app
# FROM clojure:openjdk-15-lein 
ENV APP_HOME /app
WORKDIR $APP_HOME
ENV GOOGLE_APPLICATION_CREDENTIALS=$APP_HOME/resources/secret.json
COPY resources $APP_HOME/resources
COPY .lein-env $APP_HOME
COPY ./target/portcard-api-0.1.0-SNAPSHOT-standalone.jar $APP_HOME/app.jar
EXPOSE 3000
CMD ["java", "-Xmx512m", "-Xms512m", "-jar", "app.jar"]
