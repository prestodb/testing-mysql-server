#!/bin/bash

set -eu

VERSION=5.7.22
BASEURL="https://dev.mysql.com/get/Downloads/MySQL-5.7"
PPC64LE_BASEURL="http://yum.mariadb.org/10.2/centos/7/ppc64le/rpms/"

LINUX_BASE=mysql-$VERSION-linux-glibc2.12-x86_64
LINUX_PPC64LE_RPM=MariaDB-server-10.2.32-1.el7.centos.ppc64le.rpm
OSX_BASE=mysql-$VERSION-macos10.13-x86_64

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

set -x

cd $(dirname $0)

RESOURCES=target/generated-resources

mkdir -p dist $RESOURCES

LINUX_NAME=$LINUX_BASE.tar.gz
LINUX_DIST=dist/$LINUX_NAME

LINUX_PPC64LE_DIST=dist/$LINUX_PPC64LE_RPM

OSX_NAME=$OSX_BASE.tar.gz
OSX_DIST=dist/$OSX_NAME

test -e $LINUX_DIST || curl -L -o $LINUX_DIST "$BASEURL/$LINUX_NAME"
test -e $LINUX_PPC64LE_DIST || curl -L -o $LINUX_PPC64LE_DIST "$PPC64LE_BASEURL/$LINUX_PPC64LE_RPM"
test -e $OSX_DIST || curl -L -o $OSX_DIST "$BASEURL/$OSX_NAME"

PACKDIR=$(mktemp -d "${TMPDIR:-/tmp}/mysql.XXXXXXXXXX")
$TAR -xf $LINUX_DIST -C $PACKDIR
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
cp $LINUX_PPC64LE_DIST $PACKDIR/
pushd $PACKDIR
rpm2cpio $LINUX_PPC64LE_RPM | cpio -idm
mkdir -p mysql-Linux-ppc64le/bin mysql-Linux-ppc64le/lib64 mysql-Linux-ppc64le/share mysql-Linux-ppc64le/data
cp usr/bin/mysql_install_db mysql-Linux-ppc64le/bin/
cp usr/bin/my_print_defaults mysql-Linux-ppc64le/bin/
cp usr/bin/resolveip mysql-Linux-ppc64le/bin/
cp usr/sbin/mysqld mysql-Linux-ppc64le/bin/
cp -r usr/lib64/* mysql-Linux-ppc64le/lib64/
cp -r usr/share/mysql mysql-Linux-ppc64le/share/
$TAR -C ./mysql-Linux-ppc64le -czf $OLDPWD/$RESOURCES/mysql-Linux-ppc64le.tar.gz bin lib64 share data
popd
rm -rf $PACKDIR

PACKDIR=$(mktemp -d "${TMPDIR:-/tmp}/mysql.XXXXXXXXXX")
$TAR -xf $OSX_DIST -C $PACKDIR
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
