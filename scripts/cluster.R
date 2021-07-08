library(dplyr)
library(data.table)


###

workingPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/var/"

exportPic = TRUE

### 


norm01 <- function(vals,valfloor=0)
{
	mnv <- min(vals)-valfloor
	mxv <- max(vals)
	(vals-mnv)/(mxv-mnv)
}

dendro <- function(trd, fname = "dendro.png", exportPic = FALSE, distmethod="euclidean")
{
	if (exportPic){
		par(mar=c(1,1,1,1))
		png( paste(workingPath,fname,sep=""), res=300, width=30, height=20, units='cm')
	}
	dst=dist(trd,method=distmethod)
	hc=hclust(dst,method="average")
	plabel <- paste("Dendrogram ",ctlog, " (", creators ,")", sep='' )
	plot(hc, main=plabel )
	if (exportPic){
		dev.off()
	}
}

kcluster <- function(trd, tclusters, fname = "kcluster.png", exportPic = FALSE, distmethod="euclidean")
{
	if (exportPic){
		par(mar=c(1,1,1,1))
		png( paste(workingPath,fname,sep=""), res=300, width=30, height=20, units='cm')
	}

	kclu <- kmeans(trd,centers=tclusters)

	meas2kclu = data.frame(
                  cluster=kclu$cluster)

	meas2kclu %>% arrange(cluster)
	dst <- dist(trd)
	mds <- cmdscale(dst)

	plabel <- paste("K-cluster (k=", tclusters,") ", ctlog, " (", creators ,")", sep='' )
	# plot the measures in the 2D space
	plot(mds,main=plabel, pch=19,col=rainbow(tclusters)[kclu$cluster])

	# set the legend for cluster colors
	legend("bottomright",
      	 legend=paste("clu",unique(kclu$cluster)),
	       fill=rainbow(tclusters)[unique(kclu$cluster)],
      	 border=NA,box.col=NA)
	if (exportPic){
		dev.off()
	}
}

imgname <- function(prefix,logname)
{
	paste(prefix,"_",logname,".png",sep="")
}



dev.off()
dev.new()

rundata = read.csv( paste(workingPath,"hpc.psv", sep=""),
            sep ="|", strip.white=TRUE)


logstats <- data.frame(
	Log 	 = c('BPIC2013 closed'),
	TranCt = c(4)
)



ctlog <- 'BPIC2013 closed'
ctlogns <- gsub(" ","_",ctlog)

#  Excluding LOG_EVENT_COUNT,LOG_TRACE_COUNT

rd <- rundata %>% filter (Log == ctlog) %>% 
			select (Model.Run,
				  MODEL_EDGE_COUNT,MODEL_ENTITY_COUNT,
				  ENTROPY_PRECISION_TRACEWISE,ENTROPY_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,EVENT_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
			        MODEL_STRUCTURAL_STOCHASTIC_SIMPLICITY)

ls <- logstats %>% filter (Log == ctlog)


rd$normet <- norm01(rd$ENTROPY_TRACEWISE)

rd$normedgect <- norm01(rd$MODEL_EDGE_COUNT,ls$TranCt)
rd$normentct <- norm01(rd$MODEL_ENTITY_COUNT,ls$TranCt)
rd$normsss <- norm01(rd$MODEL_STRUCTURAL_STOCHASTIC_SIMPLICITY,ls$TranCt)

rd <- rd %>% select (Model.Run,
				  normedgect,normentct,normsss,
				  ENTROPY_PRECISION_TRACEWISE,normet,
				  ENTROPY_FITNESS_TRACEWISE,EVENT_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4)


trd <- transpose(rd[,-1])

colnames(trd) <- rd$Model.Run
rownames(trd) <- colnames(rd)[-1]

creators <- paste(unique(rundata$Artifact.Creator),collapse='')

# imgname <- paste("dendro_",ctlogns,".png",sep="")
dendro(trd,fname=imgname("dendro",ctlogns),
 	exportPic=exportPic,distmethod="manhattan")

#imgname <- paste("kcluster_",ctlogns,".png",sep="")

kcluster(trd,5,fname=imgname("kcluster",ctlogns),
	exportPic=exportPic,distmethod="euclidean")

if (exportPic){
	dev.off()
}

