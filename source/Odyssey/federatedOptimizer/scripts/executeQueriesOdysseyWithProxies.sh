#!/bin/bash

ODYSSEY_HOME=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/federatedOptimizer
JENA_HOME=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/apache-jena-2.13.0
fedBenchDataPath=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/fedBenchData
proxyFederationFile=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/tmp/proxyFederation

sed -i "s,optimize=.*,optimize=false," ${ODYSSEY_HOME}/lib/fedX3.1/config2
fedBench=/home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/allqueries
#newQueries=/home/roott/queries/fedBench_1_1
datasets=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/datasetsVirtuoso
errorFilePath=/home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/results

cold=true

#l="S6 S7"

s=`seq 1 14`
l=""
n=1
w=2000
for i in ${s}; do
    l="${l} S${i}"
done


s=`seq 1 10`

for i in ${s}; do
    l="${l} C${i}"
done

s=`seq 1 8`

for i in ${s}; do
    l="${l} ch${i}"
done

#s=`seq 1 7`

#for i in ${s}; do
#    l="${l} CD${i}"
#done

#for i in ${s}; do
#    l="${l} LS${i}"
#done
#s=`seq 1 10`
#l=""

#for i in ${s}; do
#    l="${l} C${i}"
#done

for query in ${l}; do
    f=0
    for j in `seq 1 ${n}`; do
        cd ${ODYSSEY_HOME}/scripts
        tmpFile=`bash ./startProxies2.sh "localhost localhost localhost localhost localhost localhost localhost localhost localhost localhost" 3030`
        sleep 1s
	#echo $tmpFile
	#echo "processing..."
        cd ${ODYSSEY_HOME}/code
        if [ "$cold" = "true" ] && [ -f ${ODYSSEY_HOME}/lib/fedX3.1/cache.db ]; then
            rm ${ODYSSEY_HOME}/lib/fedX3.1/cache.db
        fi
        /usr/bin/time -f "%e %P %t %M" timeout ${w}s java -Xmx4096m -cp .:${JENA_HOME}/lib/*:${ODYSSEY_HOME}/lib/fedX3.1/lib/* evaluateSPARQLQuery $fedBench/$query ${datasets} ${fedBenchDataPath} 100000000 true false $query $errorFilePath false > outputFile 2> timeFile
	#echo "planning"
        x=`grep "planning=" outputFile`
        y=`echo ${x##*planning=}`
        if [ -n "$y" ]; then
            s=`echo ${y%%ms*}`
        else
            s=-1
        fi
	#echo "NumberSelectedSources"

        x=`grep "NumberSelectedSources=" outputFile`
        y=`echo ${x##*NumberSelectedSources=}`

        if [ -n "$y" ]; then
            nss=`echo ${y}`
        else
            ${ODYSSEY_HOME}/scripts/processFedXPlansNSS.sh outputFile > xxx
            nss=`cat xxx`
            rm xxx
        fi
	#echo "NumberServices"
        x=`grep "NumberServices=" outputFile`
        y=`echo ${x##*NumberServices=}`

        if [ -n "$y" ]; then
            ns=`echo ${y}`
        else
            ${ODYSSEY_HOME}/scripts/processFedXPlansNSQ.sh outputFile > xxx
            ns=`cat xxx`
            rm xxx
        fi

        x=`tail -n 1 timeFile`
        y=`echo ${x%% *}`
        x=`echo ${y%%.*}`
        if [ "$x" -ge "$w" ]; then
            t=`echo $y`
            t=`echo "scale=2; $t*1000" | bc`
            f=$(($f+1))
            nr=`grep "^\[" outputFile | grep "\]$" | wc -l | sed 's/^[ ^t]*//' | cut -d' ' -f1`
        else
            x=`grep "duration=" outputFile`
            y=`echo ${x##*duration=}`
            t=`echo ${y%%ms*}`
            x=`grep "results=" outputFile`
            nr=`echo ${x##*results=}`
        fi
        cd ${ODYSSEY_HOME}/scripts
        ./killAll.sh ${proxyFederationFile}
        sleep 10s
	#echo "processProxyInfo"
        pi=`./processProxyInfo.sh ${tmpFile} 0 9`
	#echo "printing results"
        echo "${query} ${nss} ${ns} ${s} ${t} ${pi} ${nr}"
        if [ "$f" -ge "2" ]; then
            break
        fi
    done
done
