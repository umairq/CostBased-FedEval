#!/bin/bash

m=500000
subj=true
#datasets=" Affymetrix CHEBI KEGG Drugbank Jamendo NYTimes SWDF LMDB DBpedia Geonames LinkedTCGA-A LinkedTCGA-E LinkedTCGA-M"
datasets="LinkedTCGA-M"
folder=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/fedBenchData
export codehome=/home/MuhammadSaleem/umair/evaluation/odeyssey/roott/federatedOptimizer
cd ${codehome}/code

for d in ${datasets}; do
    f=`echo "$d" | tr '[:upper:]' '[:lower:]'`
    dump="${folder}/${d}/${f}.n3"
    n=45000000
    echo $n $dump
    n=`wc -l ${dump} | sed 's/^[ ^t]*//' | cut -d' ' -f1`
    echo $n
    if [ "$n" -gt "$m" ]; then
        echo "splitting datasets"
        accFile=`mktemp`
        tmpFile=`mktemp`
        split -l $m ${dump} tmp${d}
        echo "splitted"
        for g in `ls tmp${d}*`; do
            echo ${g}
            t=`wc -l ${g} | sed 's/^[ ^t]*//' | cut -d' ' -f1`
            java orderDataset ${g} ${t} ${subj} > ${g}_sorted
            rm ${g}
        done
        started=0
        for g in `ls tmp${d}*_sorted`; do
            if [ "$started" -gt "0" ]; then
                echo "mergeDataset started"
                java mergeDataset ${accFile} ${g} ${subj} > ${tmpFile}
                mv ${tmpFile} ${accFile}
                rm ${g}
            else 
                mv ${g} ${accFile}
                started=1
            fi  
        done
        mv  ${accFile} ${folder}/${d}/${f}Sorted.n3
    else 
    java -Xmx350g orderDataset ${dump} ${n} ${subj} > ${folder}/${d}/${f}Sorted.n3
    fi
done
