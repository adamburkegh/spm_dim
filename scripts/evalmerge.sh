
# input paths
survpath=~/bpm/bpm-dimensions-lab/results/hpc1.2.2
spdpath=~/bpm/spd_we/results/paper
pdpath=~/bpm/bpm-dimensions-lab/results/pdeval
# output path
evalpath=~/bpm/bpm-dimensions-lab/results/eval2

tlog=rtfmp

dryrun=false

spdconvert(){

for f in $survpath/mrun_*predef*
do
    g=`basename $f`
    echo ==$g==
    hl=`echo $g | cut -f1 -d- | cut -c6-100 | sed s/_predef//`
    ha=`echo $g | cut -f2-3 -d- | cut -f1 -d_`
    echo $ha $hl
    sf=$spdpath/"mrun_${ha}_${hl}.xml"
    echo $sf
    if [ "$dryrun" = true ]; then
        ls ${sf}
        continue
    fi
    head --lines=-4 $f > $evalpath/$g
    grep EARTH --before-context=3 --after-context=3 $sf >> $evalpath/$g
    grep ENTROPY --before-context=3 --after-context=3 $sf >> $evalpath/$g
    echo "   </taskRunStats>
   <errorMessage></errorMessage>
   <runState>SUCCESS</runState>
</runStats>
" >> $evalpath/$g

done
}


pdevalconvert(){
for f in $survpath/mrun_${tlog}_predef*
do
    g=`basename $f`
    echo ==$g==
    hl=`echo $g | cut -f1 -d- | cut -c6-100 | sed s/_predef//`
    ha=`echo $g | cut -f2-3 -d- | cut -f1 -d_`
    echo $ha $hl
    sf=$pdpath/"mrun_${hl}_pdeval-${ha}"
    echo $sf
    if [ "$dryrun" = true ]; then
        ls ${sf}*.xml
        continue
    fi
    head --lines=-4 $f > $evalpath/$g
    for fii in ${sf}*.xml ; do
        grep EARTH --before-context=4 --after-context=3 ${fii} >> $evalpath/$g
        grep ENTROPY --before-context=4 --after-context=3 ${fii} >> $evalpath/$g
    done
    echo "   </taskRunStats>
   <errorMessage></errorMessage>
   <runState>SUCCESS</runState>
</runStats>
" >> $evalpath/$g
done
}


#spdconvert
pdevalconvert

