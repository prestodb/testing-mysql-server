#!/bin/bash

set -eu

VERSION=8.4.2
BASEURL="https://downloads.mysql.com/archives/get/p/23/file"

LINUX_BASE=mysql-$VERSION-linux-glibc2.28-x86_64
MAC_OS_ARM64_BASE=mysql-$VERSION-macos14-arm64
MAC_OS_AMD64_BASE=mysql-$VERSION-macos14-x86_64

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

set -x

cd $(dirname $0)

RESOURCES=target/generated-resources

mkdir -p dist $RESOURCES

LINUX_NAME=$LINUX_BASE.tar.xz
LINUX_DIST=dist/$LINUX_NAME

MAC_OS_ARM64_NAME=$MAC_OS_ARM64_BASE.tar.gz
MAC_OS_ARM64_DIST=dist/$MAC_OS_ARM64_NAME

MAC_OS_AMD64_NAME=$MAC_OS_AMD64_BASE.tar.gz
MAC_OS_AMD64_DIST=dist/$MAC_OS_AMD64_NAME

test -e $LINUX_DIST || curl -L -o $LINUX_DIST "$BASEURL/$LINUX_NAME" --fail
test -e $MAC_OS_ARM64_DIST || curl -L -o $MAC_OS_ARM64_DIST "$BASEURL/$MAC_OS_ARM64_NAME" --fail
test -e $MAC_OS_AMD64_DIST || curl -L -o $MAC_OS_AMD64_DIST "$BASEURL/$MAC_OS_AMD64_NAME" --fail

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
      bin/mysqld
    popd
    rm -rf $PACKDIR
}

test -e $RESOURCES/mysql-Mac_OS_X-aarch64.tar.gz || pack_macos $MAC_OS_ARM64_DIST $MAC_OS_ARM64_BASE mysql-Mac_OS_X-aarch64.tar.gz
test -e $RESOURCES/mysql-Mac_OS_X-amd64.tar.gz || pack_macos $MAC_OS_AMD64_DIST $MAC_OS_AMD64_BASE mysql-Mac_OS_X-amd64.tar.gz
test -e $RESOURCES/mysql-Linux-amd64.tar.gz || pack_linux $LINUX_DIST $LINUX_BASE mysql-Linux-amd64.tar.gz
