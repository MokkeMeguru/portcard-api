FROM clojure:openjdk-11-lein
    MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
ENV LANG C.UTF-8
ENV APP_HOME /app
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
RUN apt-get update -y
RUN apt-get upgrade -y 
RUN apt-get install tmux -y
