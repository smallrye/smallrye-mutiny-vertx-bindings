#!/usr/bin/env bash

export ROOT=vertx-mutiny-clients
export MVN_PHASE=pre-site
export DEST=pages
cd ${ROOT} || exit
echo "Generating aggregated javadoc"
mvn ${MVN_PHASE}
echo "Cloning gh-pages"
cd target  || exit
git clone -b gh-pages "git@github.com:smallrye/smallrye-reactive-utils.git" ${DEST}
echo "Copy content"
yes | cp -R site/apidocs ${DEST}/apidocs
echo "Pushing documentation"
cd ${DEST}  || exit
git add -A
git commit -m "update javadoc"
git push origin gh-pages
echo "Done!"