#!/bin/bash

set -eu

VERSION=8.4.3
BASEURL="https://dev.mysql.com/get/Downloads/MySQL-8.4"

LINUX_BASE=mysql-$VERSION-linux-glibc2.28-x86_64
MACOS_BASE=mysql-$VERSION-macos14-x86_64

TAR=tar
command -v gtar >/dev/null && TAR=gtar

if ! $TAR --version | grep -q "GNU tar"
then
    echo "GNU tar is required."
    echo "Hint: brew install gnu-tar"
    $TAR --version
    exit 100
fi

STRIP=strip
command -v gstrip >/dev/null && STRIP=gstrip

if ! $STRIP --version | grep -q "GNU strip"
then
    echo "GNU strip is required."
    echo "Hint: brew install binutils"
    exit 100
fi

PATCHELF=PATCHELF
command -v patchelf >/dev/null && PATCHELF=patchelf

set -x

cd $(dirname $0)

RESOURCES=target/generated-resources

mkdir -p dist $RESOURCES

LINUX_NAME=$LINUX_BASE.tar.xz
LINUX_DIST=dist/$LINUX_NAME

MACOS_NAME=$MACOS_BASE.tar.gz
MACOS_DIST=dist/$MACOS_NAME

test -e $LINUX_DIST || curl -L -o $LINUX_DIST "$BASEURL/$LINUX_NAME" --fail
test -e $MACOS_DIST || curl -L -o $MACOS_DIST "$BASEURL/$MACOS_NAME" --fail

# args:
# 1: DIST name
# 2: BASE name
# 3: packed name (e.g. mysql-$platform-$arch.tar.gz
function pack_macos() {
    PACKDIR=$(mktemp -d "${TMPDIR:-/tmp}/mysql.XXXXXXXXXX")
    $TAR -xf $1 -C $PACKDIR
    pushd $PACKDIR/$2
    $TAR --dereference -czf $OLDPWD/$RESOURCES/$3 \
      LICENSE \
      README \
      docs/INFO* \
      share/*.sql \
      share/*.txt \
      share/charsets \
      share/english \
      */**/libcrypto.* \
      */**/libssl.* \
      */libcrypto.* \
      */libssl.* \
      */libprotobuf.* \
      */libprotobuf-lite.* \
      bin/mysqld
    popd
    rm -rf $PACKDIR
}


# args:
# 1: DIST name
# 2: BASE name
# 3: packed name (e.g. mysql-$platform-$arch.tar.gz
function pack_linux() {
    PACKDIR=$(mktemp -d "${TMPDIR:-/tmp}/mysql.XXXXXXXXXX")
    $TAR -xf $1 -C $PACKDIR
    pushd $PACKDIR/$2
    $STRIP bin/mysqld
    # on newer versions of ubuntu, libaio is replaced with libaiot64. Update the mysql
    # binary to point to libaio.so which should exist on all systems when
    # libaio-dev/devel are installed
    $PATCHELF --replace-needed libaio.so.1 libaio.so bin/mysqld
    $TAR --dereference -czf $OLDPWD/$RESOURCES/$3 \
      LICENSE \
      README \
      docs/INFO* \
      share/*.sql \
      share/*.txt \
      share/charsets \
      share/english \
      */**/libcrypto.* \
      */**/libssl.* \
      */**/libprotobuf-lite.so* \
      */**/libabsl*.so \
      bin/mysqld
    popd
    rm -rf $PACKDIR
}

test -e $RESOURCES/mysql-Mac_OS_X-amd64.tar.gz || pack_macos $MACOS_DIST $MACOS_BASE mysql-Mac_OS_X-amd64.tar.gz
test -e $RESOURCES/mysql-Linux-amd64.tar.gz || pack_linux $LINUX_DIST $LINUX_BASE mysql-Linux-amd64.tar.gz
