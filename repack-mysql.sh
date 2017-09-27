#!/bin/bash

set -eu

VERSION=5.7.19
BASEURL="https://dev.mysql.com/get/Downloads/MySQL-5.7"

LINUX_BASE=mysql-$VERSION-linux-glibc2.12-x86_64
OSX_BASE=mysql-$VERSION-macos10.12-x86_64

TAR=tar
test -x /usr/local/bin/gtar && TAR=/usr/local/bin/gtar

if ! $TAR --version | grep -q "GNU tar"
then
    echo "GNU tar is required."
    echo "Hint: brew install gnu-tar"
    $TAR --version
    exit 100
fi

STRIP=strip
test -x /usr/local/bin/gstrip && STRIP=/usr/local/bin/gstrip

if ! $STRIP --version | grep "GNU strip"
then
    echo "GNU strip is required."
    echo "Hint: brew install binutils"
    exit 100
fi

set -x

cd $(dirname $0)

RESOURCES=target/generated-resources

mkdir -p dist $RESOURCES

LINUX_NAME=$LINUX_BASE.tar.gz
LINUX_DIST=dist/$LINUX_NAME

OSX_NAME=$OSX_BASE.tar.gz
OSX_DIST=dist/$OSX_NAME

test -e $LINUX_DIST || curl -L -o $LINUX_DIST "$BASEURL/$LINUX_NAME"
test -e $OSX_DIST || curl -L -o $OSX_DIST "$BASEURL/$OSX_NAME"

PACKDIR=$(mktemp -d "${TMPDIR:-/tmp}/mysql.XXXXXXXXXX")
tar -xzf $LINUX_DIST -C $PACKDIR
pushd $PACKDIR/$LINUX_BASE
$STRIP bin/mysqld
$TAR -czf $OLDPWD/$RESOURCES/mysql-Linux-amd64.tar.gz \
  COPYING \
  README \
  docs/INFO* \
  share/*.sql \
  share/*.txt \
  share/charsets \
  share/english \
  bin/mysqld
popd
rm -rf $PACKDIR

PACKDIR=$(mktemp -d "${TMPDIR:-/tmp}/mysql.XXXXXXXXXX")
tar -xzf $OSX_DIST -C $PACKDIR
pushd $PACKDIR/$OSX_BASE
$TAR -czf $OLDPWD/$RESOURCES/mysql-Mac_OS_X-x86_64.tar.gz \
  COPYING \
  README \
  docs/INFO* \
  share/*.sql \
  share/*.txt \
  share/charsets \
  share/english \
  bin/mysqld
popd
rm -rf $PACKDIR