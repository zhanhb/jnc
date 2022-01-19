#!/bin/sh

write_with_backup() { # <file> <content> <original> <extension>
  backup="$1${4:-.bak}"
  [ -e "$backup" ] || [ "${3-}" = "$2" ] || {
    if [ -e "$1" ]; then
      [ -z "${3-}" ] || echo "replace '$1' with '$2' from '$3'"
      cp -Lp "$1" "$backup"
    fi
    printf "%s\n" "$2" >"$1"
  }
}

FreeBSD() {
  ASSUME_ALWAYS_YES=yes pkg-static bootstrap -f
  IGNORE_OSVERSION=yes pkg update
  pkg upgrade -y
  pkgs=
  for pkg in bash gh git jq rsync; do
    command -v "$pkg" >/dev/null ||
      pkgs="${pkgs:+${pkgs} }$pkg"
  done
  # shellcheck disable=SC2086
  [ -z "$pkgs" ] || pkg install -fy $pkgs
}

openbsd_libraries() { # <prefix> <no_dot>
  # https://stackoverflow.com/a/62293458
  # xserv and xfont are not necessary for this project
  # xserv=/usr/X11R6/bin/X
  # xfont=/usr/X11R6/lib/X11/fonts/100dpi
  names=
  for pred in \
    comp=/usr/lib/libc.a \
    xshare=/usr/X11R6/bin/startx \
    xbase=/usr/X11R6/bin/xinit; do
    [ -e "${pred#*=}" ] || names="$names,${pred%%=*}$2.tgz"
  done
  [ -z "$names" ] || (
    dir="$(mktemp -d)"
    on_exit() { rm -rf "$dir"; }
    trap on_exit EXIT
    cd "$dir" || return
    # curl with old version might not support option `--fail-early`
    curl -fsSLOC- "$1/{SHA256.sig$names}" || return
    signify -Cp "/etc/signify/openbsd-$2-base.pub" -x SHA256.sig -- *.tgz || return
    for file in *.tgz; do tar -C / -zxf "$file"; done
    ldconfig /usr/local/lib /usr/X11R6/lib
  )
}

OpenBSD() {
  # OpenBSD older than 6.6 has been removed from https://cdn.openbsd.org
  # work with http for old version perform bad on the ssl certificate
  # SSL write error: certificate verification failed: certificate has expired
  host=http://ftp.nluug.nl
  mirror="$host/pub/OpenBSD"
  PKG_PATH="$host/%m" pkg_add -uI
  pkgs=
  for pkg in bash curl git jq rsync; do
    command -v "$pkg" >/dev/null ||
      pkgs="${pkgs:+${pkgs} }$pkg--"
  done
  # shellcheck disable=SC2086
  [ -z "$pkgs" ] || PKG_PATH="$host/%m" pkg_add -I $pkgs

  install_url="$(sed 's/#.*//;/^$/d' /etc/installurl 2>/dev/null || :)"
  release="$(uname -r)"
  arch="$(uname -m)"
  no_dot="$(echo "$release" | sed 's/\.//g')"
  if [ -n "$install_url" ] && curl -fsSIo/dev/null "$install_url/$release/$arch/base$no_dot.tgz"; then
    mirror="$install_url"
  else
    write_with_backup /etc/installurl "$mirror" "$install_url"
  fi
  openbsd_libraries "$mirror/$release/$arch" "$no_dot" &
  pid=$!

  if login_conf="$(curl -fsSL "https://github.com/openbsd/src/raw/master/etc/etc.$arch/login.conf")"; then
    write_with_backup /etc/login.conf "$login_conf"
  fi
  wait $pid
}

main() {
  OS="$(uname -s)"
  case "$OS" in
  FreeBSD | OpenBSD) "$OS" "$@" ;;
  *) ;;
  esac
}

set -e
main "$@"
