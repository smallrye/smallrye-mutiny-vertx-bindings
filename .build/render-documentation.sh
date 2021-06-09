#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

mvn -f vertx-mutiny-clients/pom.xml pre-site
cp -R vertx-mutiny-clients/target/site/apidocs docs/

PROJECT_VERSION=$(cat .github/project.yml | yq eval '.release.current-version' -)
mike deploy --push --force --update-aliases $PROJECT_VERSION latest
mike set-default --push --force latest