#!/bin/bash
set -ex
cd $(dirname $0)
export REPO_ROOT=$(cd .. && pwd)
export GIT_COMMITTER_DATE="1678008251 +0100"
export GIT_AUTHOR_DATE="1678008252 +0100"
export GIT_AUTHOR_NAME="Christof Lemke"
export GIT_COMMITTER_NAME="Christof Lemke"
export GIT_AUTHOR_EMAIL="doesnotexist@gmail.com"

lit() {
  java -jar $REPO_ROOT/build/libs/lit.java-1.0-SNAPSHOT.jar "${@}"
}

(cd $REPO_ROOT && ./gradlew jar)

(
  set -e
  REF_REPO_PATH=reference-repo
  rm -rf "$REF_REPO_PATH"
  git init $REF_REPO_PATH
  cd $REF_REPO_PATH
  git config --local user.name $GIT_AUTHOR_NAME
  git config --local user.email $GIT_AUTHOR_EMAIL

  echo 'hello' > hello.txt
  echo 'world' > world.txt
  git add .
  git commit -m 'first commit'

  mkdir bin
  echo 'hi!' > bin/hi.sh
  chmod u+x bin/hi.sh
  git add bin/hi.sh
  echo 'world' > bin/world.txt
  git add bin/world.txt
  git commit -m 'second commit'
)

(
  set -e

  MY_REPO_PATH=my-repo
  rm -rf "$MY_REPO_PATH"
  lit init $MY_REPO_PATH
  cd $MY_REPO_PATH
  echo 'hello' > hello.txt
  echo 'world' > world.txt
  lit add hello.txt
  lit add world.txt
  echo 'first commit' | lit commit

  mkdir bin
  echo 'hi!' > bin/hi.sh
  chmod u+x bin/hi.sh
  echo 'world' > bin/world.txt
  lit add bin/hi.sh
  lit add bin/world.txt
  echo 'second commit' | lit commit
)

# index test
(
  set -e
  REF_REPO_PATH=index-repo
  rm -rf "$REF_REPO_PATH"
  git init $REF_REPO_PATH
  cd $REF_REPO_PATH
  mkdir dir
  echo '123' > dir/aindex.test
  echo '456' > dir/bindex.test
  echo '789' > cindex.test
  lit add .
  echo 'added' > added.txt
  lit add added.txt
  cp .git/index ../lit-index
  hexdump -C .git/index > ../lit-index.hex

  rm -r .git
  git init .
  git add cindex.test dir
  git commit -m 'first'
  git add added.txt
  cp .git/index ../git-index
  hexdump -C .git/index > ../git-index.hex
)

# Status test
(
  rm -rf status-repo
  git init status-repo
  cd status-repo
  echo 'modified' > modified.txt
  echo 'missing' > missing.txt
  mkdir b
  touch b/c.txt
  git add .
  git commit -m 'initial commit'

  echo 'test' > modified.txt
  rm missing.txt
  touch new.txt
  touch new_added.txt
  git add new_added.txt
)