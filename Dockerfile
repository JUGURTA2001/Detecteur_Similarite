
# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:26-jdk

# Installer Maven 3.9.11 directement
ADD https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz /tmp/maven.tar.gz

RUN mkdir -p /opt/maven \
    && tar -xzf /tmp/maven.tar.gz -C /opt/maven --strip-components=1 \
    && rm /tmp/maven.tar.gz

ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

WORKDIR /app

COPY . .

# Cache Maven + nouvelles tentatives si le réseau interrompt le téléchargement
RUN --mount=type=cache,target=/root/.m2/repository \
    sh -c 'for i in 1 2 3 4 5; do \
    mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 clean package -DskipTests && exit 0; \
    echo "Téléchargement interrompu. Nouvelle tentative $i/5..."; \
    sleep 10; \
    done; exit 1'
