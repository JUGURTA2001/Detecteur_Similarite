FROM eclipse-temurin:25-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget unzip libgtk-3-0 libxtst6 libxxf86vm1 libgl1 libasound2t64 \
    xvfb x11vnc fluxbox novnc websockify \
    && rm -rf /var/lib/apt/lists/*

ARG JFX_VERSION=26.0.1

RUN wget -q "https://download2.gluonhq.com/openjfx/${JFX_VERSION}/openjfx-${JFX_VERSION}_linux-x64_bin-sdk.zip" \
    -O /tmp/javafx.zip \
    && unzip -q /tmp/javafx.zip -d /opt \
    && mv "/opt/javafx-sdk-${JFX_VERSION}" /opt/javafx \
    && rm /tmp/javafx.zip

WORKDIR /app

COPY src/ ./src/
COPY data/ ./data/

RUN mkdir -p out \
    && javac --module-path /opt/javafx/lib \
    --add-modules javafx.controls,javafx.fxml \
    -d out $(find src -name "*.java") \
    && mkdir -p out/ui out/application \
    && cp src/ui/*.fxml out/ui/ \
    && cp src/application/*.css out/application/

ENV DISPLAY=:0
EXPOSE 6080

CMD ["bash", "-lc", "Xvfb :0 -screen 0 1280x800x24 -ac & sleep 1; fluxbox >/tmp/fluxbox.log 2>&1 & x11vnc -display :0 -forever -shared -nopw -rfbport 5900 >/tmp/x11vnc.log 2>&1 & websockify --web=/usr/share/novnc 6080 localhost:5900 >/tmp/websockify.log 2>&1 & exec java --module-path /opt/javafx/lib --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp out application.Main"]