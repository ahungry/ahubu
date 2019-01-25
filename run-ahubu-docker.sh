#!/bin/bash

defaultcmd="java -jar /usr/src/myapp/myapp-standalone.jar"

image=ahubu:latest
name=ahubu_running
cmd=$defaultcmd
cmdargs=""
opts=""
data=""
data_is_volume=false
do_sound=false
do_update=false

local_data="$HOME/.local/share/ahubu"

usage() {
    echo "
Usage: $0 [opts] [image]

Options:
    -h, --help          display this message and exit
    --sound             enable sound (requires PulseAudio and Linux)
    --disable-sound     ensure that sound is disabled
    --cmd CMD           initial command (default: $cmd)
    --data PATH         path to store data (default: docker volume ahubu-data)
    --data-volume VOL   docker volume to store data (default: ahubu-data)
    --reset             reset local data and docker volume
    --name NAME         container name (default: $name)
    --shell             start an interactive shell instead of AHUBU
    --winecfg           run winecfg before calling AHUBU
    --dry-run           simply print the docker run command
    --no-tz             disable timezone detection
    --test              test environment
    --update            update docker image before run
    -e                  docker run option (environment)
    -v                  docker run option (mount volume)
    -u                  docker run option (change user)
"
}

lopts="dry-run,help,winecfg,shell,cmd:,name:"
lopts="${lopts},data:,data-volume:,reset"
lopts="${lopts},sound,disable-sound"
lopts="${lopts},test"
lopts="${lopts},no-tz"
lopts="${lopts},update"

getopt=getopt
brew_getopt="/usr/local/opt/gnu-getopt/bin/getopt"
if [[ $OSTYPE == darwin* ]] && [ -x $brew_getopt ]; then
    getopt=$brew_getopt
fi

args="$($getopt -n "${0}" -o hv:u:e: --longoptions $lopts -- "${0}" "${@}")"
if [ $? -ne 0 ]; then
    usage
    exit 1
fi
eval set -- $args

do_run=true
do_reset=false
do_test=false

mytz=${TZ:-}
detect_tz=false
if [ -z "${mytz}" ]; then
    detect_tz=true
fi

while [ -n "${1:-}" ]; do
case "${1:-}" in
    --help|-h)  usage && exit 0 ;;
    --data) shift
        data_is_volume=false
        data="$1" ;;
    --data-volume) shift
        data_is_volume=true
        data="$1" ;;
    --dry-run)
        do_run=false ;;
    --winecfg)
        [[ "$cmd" == "ahubu" ]] && cmdargs="${cmdargs} $1";;
    --sound)
        [[ "$cmd" == "ahubu" ]] && cmdargs="${cmdargs} $1"
        image=panard/ahubu:sound
        do_sound=true ;;
    --disable-sound)
        [[ "$cmd" == "ahubu" ]] && cmdargs="${cmdargs} $1" ;;
    --reset)
        do_reset=true ;;
    --shell)
        cmd="bash"
        cmdargs=""
        opts="${opts} -it" ;;
    --test)
        local_data="$HOME/.local/share/ahubu-test"
        do_test=true ;;
    --cmd) shift;
        cmd="$1"
        cmdargs=""
        opts="${opts} -it" ;;
    --no-tz)
        detect_tz=false
        mytz="" ;;
     --name) shift;
        name="$1" ;;
     --update)
        do_update=true ;;
     -e|-v|-u)
        opts="${opts} $1 $2" ;;
     --) shift ; shift
        if [ -n "${1:-}" ]; then
            image=$1
        fi ;;
esac
shift
done

run() {
    echo "${@}"
    if $do_run; then
        "${@}"
    fi
}

if $detect_tz; then
    if [ -f /etc/timezone ]; then
        mytz=$(</etc/timezone)
    elif [ -L /etc/localtime ]; then
        mytz=$(readlink /etc/localtime |cut -d/ -f 5-)
    elif [[ $OSTYPE == linux-gnu ]]; then
        _tz="$(timedatectl 2>/dev/null|grep "Time zone"|cut -d: -f2|cut -d' ' -f 2)"
        if [ -n ${_tz} ] && [[ "${_tz}" != "n/a" ]]; then
            mytz=$_tz
        fi
    fi
fi

if [ -z "${data:-}" ]; then
    if $do_test; then
        data="ahubu-data-test"
    else
        data="ahubu-data"
    fi
    data_is_volume=true
fi
if $do_reset; then
    msg="You are about to delete ${local_data}"
    if $data_is_volume; then
        msg="${msg} and wipe docker volume ${data}"
    fi
    echo "WARNING: $msg"
    echo "Press Enter to continue, CTRL+C to abort"
    read
    rm -vrf "${local_data}"
    if $data_is_volume; then
        run docker volume rm $data
    fi
fi
if $data_is_volume; then
    docker volume inspect $data >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        run docker volume create $data
    fi
fi

set -e

# if [ ! -d ${local_data} ]; then
#     mkdir -p ${local_data}
#     cd ${local_data}
#     docker run --rm ${image} tar c /home/wine/.wine/host | tar xv --strip-components 4
#     cd -
# fi
# opts="${opts} -v ${local_data}:/home/wine/.wine/host/"
# opts="${opts} -v ${data}:/home/wine/.wine/drive_c/users/"
#-v "${data}:/home/wine/.wine/drive_c/windows/Microsoft.NET/Framework/v4.0.30319/Wizards of the Coast, LLC/" \

if [[ ${OSTYPE} == darwin* ]]; then
    iface=$(route -n get 0.0.0.0 2>/dev/null | awk '/interface: / {print $2}')
    echo "Using network interface '${iface}'"
    ip=$(ifconfig $iface | grep inet | awk '$1=="inet" {print $2}')
    if [ -z ${ip} ]; then
        echo "Cannot find your local IP address (iface=$iface)."
        exit 1
    fi
    run open -a XQuartz
    echo "socat on $ip forwarding to $DISPLAY"
    socat TCP4-LISTEN:6000,bind=$ip,range=$ip/32,reuseaddr,fork UNIX-CLIENT:\"$DISPLAY\" &
    socat_pid=$!
    sleep 2
    export DISPLAY=$ip:0
    opts="${opts} -e WINE_X11_NO_MITSHM=1"
    WEBBROWSER=open
else
    opts="${opts} -v /tmp/.X11-unix:/tmp/.X11-unix:rw"
    #opts="${opts} -v $HOME/.Xauthority:/home/wine/.Xauthority:ro"
    opts="${opts} -v $HOME/.Xauthority:/root/.Xauthority:ro"
    WEBBROWSER=xdg-open
fi

if $do_sound; then
    if [[ ${OSTYPE} != linux-gnu ]]; then
        echo "Sound is currently supported only on Linux"
        exit 1
    fi
    _host_pulse="/run/user/$(id -u)/pulse/native"
    if [ ! -S $_host_pulse ]; then
        echo "PulseAudio does not seem active (${_host_pulse} not found)"
        exit 1
    fi
    opts="${opts} -i -v ${_host_pulse}:/run/user/1000/pulse/native"
fi

opts="${opts} --net=host --ipc=host"
if [ -n "${mytz}" ]; then
    opts="${opts} -e TZ=/usr/share/zoneinfo/${mytz}"
fi

if [ -n "${name}" ]; then
    opts="${opts} --name ${name}"
fi

URL_REQUESTS=${local_data}/openurl
> ${URL_REQUESTS}
chmod 600 ${URL_REQUESTS}
(tail -f ${URL_REQUESTS} | while read url; do
    run $WEBBROWSER "${url}"
done)&
openurl_pid=$!

set +e

if $do_update; then
    run docker pull ${image}
fi

run docker run --privileged --rm -e DISPLAY \
    ${opts} ${image} ${cmd} ${cmdargs}

rm -f ${URL_REQUESTS}
pkill -TERM -P $openurl_pid

if [[ ${OSTYPE} == darwin* ]]; then
    kill ${socat_pid}
fi
