set -e

# Default to 3g Memory
if [[ -z ${JMEM+epsilon} ]]; then set JMEM=3g; fi

LDP=lib/lp_solve_5.5_java/lib/ux64
JOPTS=-"Xmx${JMEM} -Xms${JMEM} -verbose:gc -Xloggc:logs/gc.log"
# CONFIG_PARAMS="-Djava.library.path=$LDP -Dlog4j.configurationFile=config/static/log4j2.xml"
CONFIG_PARAMS="-Djava.library.path=$LDP"

mkdir -p var

sqjar=`ls lib/sqdimensions*.jar | sort | head -1`
echo $sqjar

${JAVA_HOME}/bin/java ${CONFIG_PARAMS} ${JOPTS} -jar $sqjar


