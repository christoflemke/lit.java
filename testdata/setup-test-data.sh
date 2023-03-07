#!/bin/bash
set -ex
cd $(dirname $0)
export REPO_ROOT=$(cd .. && pwd)
export GIT_COMMITTER_DATE="1678008251 +0100"
export GIT_AUTHOR_DATE="1678008252 +0100"
export GIT_AUTHOR_NAME="Christof Lemke"
export GIT_AUTHOR_EMAIL="doesnotexist@gmail.com"

(cd $REPO_ROOT && ./gradlew jar)

(
  set -e
  REF_REPO_PATH=reference-repo
  rm -rf "$REF_REPO_PATH"
  git init $REF_REPO_PATH
  git config --local user.name $GIT_AUTHOR_NAME
  git config --local user.email $GIT_AUTHOR_EMAIL
  cd $REF_REPO_PATH

  echo 'hello' > hello.txt
  echo 'world' > world.txt
  git add .
  git commit -m 'first commit'

  mkdir bin
  echo 'hi!' > bin/hi.sh
  chmod u+x bin/hi.sh
  git add bin/hi.sh
  git commit -m 'second commit'
)

(
  set -e
  lit() {
    java -jar $REPO_ROOT/build/libs/lit.java-1.0-SNAPSHOT.jar "${@}"
  }
  MY_REPO_PATH=my-repo
  rm -rf "$MY_REPO_PATH"
  lit init $MY_REPO_PATH
  cd $MY_REPO_PATH
  echo 'hello' > hello.txt
  echo 'world' > world.txt
  echo 'first commit' | lit commit

  mkdir bin
  echo 'hi!' > bin/hi.sh
  chmod u+x bin/hi.sh
  echo 'second commit' | lit commit
)