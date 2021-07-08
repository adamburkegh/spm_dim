
survpath=~/bpm/bpm-dimensions-lab/results/hpc
spdpath=~/bpm/spd_we/results/paper
evalpath=~/bpm/bpm-dimensions-lab/results/eval


for f in $survpath/mrun_*predef*
do
    g=`basename $f`
    echo ==$g==
    head --lines=-4 $f > $evalpath/$g
    hl=`echo $g | cut -f1 -d- | cut -c6-100 | sed s/_predef//`
    ha=`echo $g | cut -f2-3 -d- | cut -f1 -d_`
    echo $ha $hl
    sf=$spdpath/"mrun_${ha}_${hl}.xml"
    echo $sf
    # ls $spdpath/$sf
    grep EARTH --before-context=3 --after-context=3 $sf >> $evalpath/$g
    grep ENTROPY --before-context=3 --after-context=3 $sf >> $evalpath/$g
    echo "   </taskRunStats>
   <errorMessage></errorMessage>
   <runState>SUCCESS</runState>
</runStats>
" >> $evalpath/$g

done




