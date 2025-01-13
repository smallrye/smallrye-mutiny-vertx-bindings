#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

mvn -s .build/maven-ci-settings.xml -f vertx-mutiny-clients/pom.xml -B pre-site
cp -R vertx-mutiny-clients/target/reports/apidocs docs/

PROJECT_VERSION=$(cat .github/project.yml | yq eval '.release.current-version' -)
uv sync --all-extras --dev
uv run mkdocs build
uv run mike deploy --push --update-aliases $PROJECT_VERSION latest
uv run mike set-default --push latest