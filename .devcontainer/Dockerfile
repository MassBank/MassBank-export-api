FROM mcr.microsoft.com/devcontainers/base:ubuntu26.04

# Install Java 21, Maven, PostgreSQL client, GitHub CLI and diagnostic tools
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends \
        openjdk-21-jdk \
        maven \
        postgresql-client \
        curl \
        jq \
        git \
        gh \
        ca-certificates \
    && apt-get clean -y && rm -rf /var/lib/apt/lists/*

WORKDIR /workspaces/MassBank-export-api
USER vscode
CMD ["sleep", "infinity"]
