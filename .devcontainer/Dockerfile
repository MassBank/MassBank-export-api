FROM eclipse-temurin:25-jdk AS java
FROM mcr.microsoft.com/devcontainers/base:ubuntu26.04
COPY --from=java /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"

# Install development tools.
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends \
        maven \
        postgresql-client \
        curl \
        jq \
        git \
        gh \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN install -d -m 0700 -o vscode -g vscode /home/vscode/.codex

WORKDIR /workspaces/MassBank-export-api
USER vscode
