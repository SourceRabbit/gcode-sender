
#!/bin/bash

platform='unknown'
unamestr=`uname`
case "$unamestr" in
        Linux)
                platform='linux'
                rootdir="$(dirname $(readlink -f $0))"
        ;;
        Darwin)
                platform='mac'
                rootdir="$(cd $(dirname $0); pwd -P)"
        ;;
esac

case "$platform" in
        mac)
                java -Xdock:name=SourceRabbit-GCODE-Sender -jar -Xmx256m $rootdir/SourceRabbit-GCODE-Sender*.jar
        ;;
        linux)
                java -jar -Xmx256m $rootdir/USourceRabbit-GCODE-Sender*.jar
        ;;
esac
        
