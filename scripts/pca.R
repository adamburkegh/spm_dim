library(dplyr)
library(tidyr)
library(stringr)
library(ggplot2)
library(readr)
library(corrplot)
library(factoextra)
library(MVN)
library(psych)
library(RColorBrewer)
###

hpath = "c:/Users/burkeat/bpm/"
# hpath = "c:/Users/Adam/bpm/"

workingPath = paste(hpath,"bpm-dimensions-lab/var/",sep="")
resultsPath = paste(hpath,"bpm-dimensions-lab/results/",sep="")




exportPic <- TRUE 
# exportPic <- FALSE
matchSampleSizes <- TRUE  # Duplicate data points for underweight sources
# matchSampleSizes <- FALSE  

### 


imgname <- function(prefix,logname)
{
	paste(prefix,"_",logname,".png",sep="")
}


prepfig <- function(fprefix,logname, width=30, height=20, mar=c(1,1,1,1))
{
    if (exportPic){
        par(mar=mar)
        png( paste(workingPath,fprefix,"_",logname,".png",sep=""), 
			res=300, width=width, height=height, units='cm')
    }
}

postfig <- function()
{
    if (exportPic){
        dev.off()
    }
}



  


corheatmap <- function(pv,sel,main){
  mname <- paste(main,"_d",nfactors,sep='')
	prepfig( paste("heatmap",mname,sep=""),sel)
	col= colorRampPalette(brewer.pal(8, "Blues"))(25)
	heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col,
	        cexRow = 1.0, #cexCol = 1.5,
	        labCol="",
	        scale = 'none',
	        add.expr = text(x = seq_along(colnames(pv)),
	                        y=-1.0, srt=45, labels=colnames(pv), 
	                        xpd=TRUE, cex=2.0) )
	legend(x="bottomright", legend=c("min","mid","max"),
	       fill=colorRampPalette(brewer.pal(8, "Blues"))(3),
	       inset=c(0.17,0.10), bty="n" )
	rect(xleft = 0.75, xright = 0.905, ybottom = 0.11, ytop = 0.31) 
	text(x = 0.755, y = 0.25, adj = c(0,0),
	     "Scaled contributions\nper component", cex=1.0 ) 
	postfig()
}


normDataSizes <- function(rdf){
	lc <-	rdf %>% 
		# filter(gtype == "predef" | gtype == "rando") %>% 
		select (Log,gtype) %>% 
		group_by(Log,gtype) %>% 
		count

	lscale <- data.frame(lc$Log, lc$gtype, floor(1000/lc$n) )
	colnames(lscale) = c('Log','gtype','scaleBy')
	rdf <- merge(rdf,lscale)
	rdf <- rdf %>% 
		mutate (
			scaleBy =  case_when (
						gtype == "setm" 	~ 1,
						gtype == "rando" 	~ scaleBy,
						gtype == "predef" ~ scaleBy
					)
		)
	rdf %>% uncount(rdf$scaleBy) %>% select (-scaleBy)
}

pf <- function(msg){
    write_lines(msg,resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}


pfc <- function(msg){
    write_lines(capture.output(msg),resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}

pcaEllipsePlot <- function(figname,ctlogns,xlab,ylab,eaxes,habillage){
  prepfig(figname,ctlogns)
  pcalplot <- 
    fviz_pca_ind(pcares,
                 repel = FALSE,     # Avoid text overlapping
                 axes = eaxes,
                 label = "",
                 addEllipses=TRUE, ellipse.level=0.95,
                 habillage = habillage
    ) +   
    # labs(title ="", x = "PCA1 (Adhesion)", y = "PCA2 (Simplicity)") +
    labs(title ="", x = xlab, y = ylab) +
    theme(legend.position = c(0.93, 0.1))
  print(pcalplot)
  postfig()
}

pcaTopFeatures <- function(pcam,thresh){
  xd <- as.data.frame(pcam)
  res <- list()
  res$d1 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.1) > thresh) %>% select(Dim.1)
  res$d2 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.2) > thresh) %>% select(Dim.2)
  res$d3 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.3) > thresh) %>% select(Dim.3)
  res$d4 <- xd[order(row.names(xd)), ] %>% filter (abs(Dim.4) > thresh) %>% select(Dim.4)
  res
}


warning_handler <- function(c) cat("Found expected warning...\n")


dev.off()
dev.new()



rundata = read.csv( paste(workingPath,"hpc1.3.2.psv", sep=""),
           		 sep ="|", strip.white=TRUE)


logstats <- read.csv( paste(resultsPath,"logstats.csv",sep=""))


logstats <- subset(logstats, select=-c(Log.Trace.Variants))

rundata <- merge(rundata,logstats)

# ctlog <- 'BPIC2013 closed'
# ctlog <- 'BPIC2013 incidents'
# ctlog <- 'BPIC2018 control'
# ctlog <- 'BPIC2018 reference'
# ctlog <- 'Sepsis'

# ctlogns <- gsub(" ","_",ctlog)

# ctlog <- 'BPIC2013_closed and Sepsis'
# ctlog <- 'BPIC2013 closed & incidents, BPIC2018 control & reference, and Sepsis'
ctlog <- 'BPIC2013 closed & incidents, BPIC2018 control & reference, Road Traffic Fines and Sepsis'


# ctlogns <- 'b2013c_sepsis'  
ctlogns <- 'all'
# ctlogns <- 'discorand'


# nfactors <- 5
# nfactors <- 4
nfactors <- 3
# nfactors <- 2

scalepca <- TRUE

resfile <-  paste(workingPath,"pca4_",nfactors,ctlogns,".txt",sep="") 

write_lines(paste(ctlogns,"n=",nfactors),resfile)

pf(colnames(rundata))

creators <- unique(rundata$Artifact.Creator)

pf("Creators")
pf(creators)

rundata$EARTH_MOVERS_TRACEWISE <- ifelse(rundata$EARTH_MOVERS_TRACEWISE < 0, 0, 
				rundata$EARTH_MOVERS_TRACEWISE)
rundata$ENTROPY_PRECISION_TRACEPROJECT <- ifelse(rundata$ENTROPY_PRECISION_TRACEPROJECT < 0, 0, 
                                         rundata$ENTROPY_PRECISION_TRACEPROJECT)

rundata$EARTH_MOVERS_TRACEWISE <- ifelse(rundata$EARTH_MOVERS_TRACEWISE == "-0.0", 0, 
                                      rundata$EARTH_MOVERS_TRACEWISE)
rundata$ENTROPY_PRECISION_TRACEPROJECT <- ifelse(rundata$ENTROPY_PRECISION_TRACEPROJECT == "-0.0", 0, 
                                                 rundata$ENTROPY_PRECISION_TRACEPROJECT)



gt <- rundata %>% 
	select(Artifact.Creator) %>% 
	separate(Artifact.Creator,c("gtype","dtype"),sep="-",extra="merge") 

rundata$gtype <- gt$gtype

pf(paste("Sample size n =",nrow(rundata)))

fnumericcols <- c(			'Log.Trace.Count',
                        'Log.Event.Count',
                        'TRACE_OVERLAP_RATIO',  		
                        'EARTH_MOVERS_TRACEWISE',
                        'EVENT_RATIO_GOWER',
                        'TRACE_RATIO_GOWER_2','TRACE_RATIO_GOWER_3',
                        'TRACE_RATIO_GOWER_4',
                        'ENTROPY_PRECISION_TRACEWISE',
                        'ENTROPY_FITNESS_TRACEWISE',
                        'ENTROPY_PRECISION_TRACEPROJECT',
                        'ENTROPY_FITNESS_TRACEPROJECT',
                        'STRUCTURAL_SIMPLICITY_ENTITY_COUNT',
                        'STRUCTURAL_SIMPLICITY_EDGE_COUNT',
                        'STRUCTURAL_SIMPLICITY_STOCHASTIC',
                        'TRACE_GENERALIZATION_FLOOR_5',
                        'TRACE_GENERALIZATION_DIFF_UNIQ'	,
                        'MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY',
                        'ALPHA_PRECISION_UNRESTRICTED_ZERO',
                        'ENTROPIC_RELEVANCE_RESTRICTED_ZO',
                        'ENTROPIC_RELEVANCE_UNIFORM',
                        'ENTROPIC_RELEVANCE_ZERO_ORDER'
                     )

# removed columns conf -> journal
# 'TRACE_PROBMASS_OVERLAP',
# 'TRACE_GENERALIZATION_FLOOR_1', 	# highly correlated 
# 'TRACE_GENERALIZATION_FLOOR_10',  	# highly correlated 


# Excluded as all zero
rundata <- subset(rundata, select=-c(ALPHA_PRECISION_RESTRICTED_1_PCT,ALPHA_PRECISION_UNRESTRICTED_1_PCT))

rundata[,fnumericcols] <- sapply(rundata[,fnumericcols], as.numeric)

pf("Data description before rescaling")
withCallingHandlers(message = warning_handler, {
  rdd <- describe(rundata,type=2)
} )
pfc(rdd)

# Due to a bug, which turned out to be a good insight into the behaviour of alpha
# precision, the field ALPHA_PRECISION_UNRESTRICTED_ZERO is actually the measure
# EXISTENTIAL_PRECISION (XPU). This was part of the dev process for XPU.
rundata <- rundata %>% rename(EXISTENTIAL_PRECISION = ALPHA_PRECISION_UNRESTRICTED_ZERO) 

rdprescale <- rundata


# rd <- rundata # %>% filter (Log == ctlog) # %>% 
# rd <- rundata %>% filter (Log == 'BPIC2013 closed' || Log == 'Sepsis') %>% 

if (ctlogns == 'discorand'){
  rundata <- rundata %>% filter ( !grepl('setm',Artifact.Creator) ) 
}

# %>%
#		      filter ( !grepl('rando',Artifact.Creator) ) %>%

pf("Data description after filtering")
withCallingHandlers(message = warning_handler, {
  rdd <- describe(rundata,type=2)
})
pfc(rdd)


if (matchSampleSizes){
	rundata <- normDataSizes(rundata)
}




rdshortname <- rundata %>% select (where(is.numeric) ) %>%
                           select (-Log.Id) %>%
                    rename(
                      LTC = Log.Trace.Count,
                      LEC = Log.Event.Count,
                      TOR = TRACE_OVERLAP_RATIO ,
                      EMT = EARTH_MOVERS_TRACEWISE,
                      ARG = EVENT_RATIO_GOWER,
                      TRG2 = TRACE_RATIO_GOWER_2,
                      TRG3 = TRACE_RATIO_GOWER_3,
                      TRG4 = TRACE_RATIO_GOWER_4,
                      HIPT = ENTROPY_PRECISION_TRACEWISE,
                      HIFT = ENTROPY_FITNESS_TRACEWISE,
                      SSENC = STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
                      SSEDC = STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                      SSS = STRUCTURAL_SIMPLICITY_STOCHASTIC,
                      TGF5 = TRACE_GENERALIZATION_FLOOR_5,
                      TGDU = TRACE_GENERALIZATION_DIFF_UNIQ,
                      HJPT = ENTROPY_PRECISION_TRACEPROJECT,
                      HJFT = ENTROPY_FITNESS_TRACEPROJECT ,
                      CSS = MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                      XPU = EXISTENTIAL_PRECISION,
                      HRU = ENTROPIC_RELEVANCE_UNIFORM,
                      HRZ = ENTROPIC_RELEVANCE_ZERO_ORDER,
                      HRR = ENTROPIC_RELEVANCE_RESTRICTED_ZO,
                      MEC = MODEL_ENTITY_COUNT,
                      MGC = MODEL_EDGE_COUNT
                      ) %>%
                  select(LTC,LEC,
                         MEC,MGC,
                         CSS,
                         TOR,EMT,
                         ARG,TRG2,TRG3,TRG4,
                         HIPT,HIFT,
                         SSENC,SSEDC,SSS,
                         TGF5,TGDU,
                         HJPT,HJFT,
                         XPU,
                         HRU,HRZ,HRR)

logcor <- cor(rdshortname)
prepfig("corlog4",ctlogns, width=30, height=30)
corrplot(logcor, method="circle", tl.cex=1.5, cl.cex=1.8)
# corrplot(logcor, method='number', tl.cex=1.5, cl.cex=1.8)
postfig()



# Exclude very highly correlated columns
rdo <- rundata %>%
# filter ( Log == 'BPIC2013 closed' || Log == 'BPIC2013 incidents'
#                        || Log == 'BPIC2018 control' || 'BPIC2018 reference'
#				|| Log == 'Sepsis' ) %>%
			# filter ( !grepl('setm',Artifact.Creator) )  %>% ## exclude SETM
# 		      filter (Log == ctlog)  %>%
		      select (# Model.Run,
				  Log,
				  Log.Id,
				  Log.Trace.Count,
				  Log.Event.Count,
				  # TRACE_OVERLAP_RATIO, # corr with EMT on DISCO+RAND
				  EARTH_MOVERS_TRACEWISE,
			    EVENT_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  ENTROPY_PRECISION_TRACEPROJECT,
				  ENTROPY_FITNESS_TRACEPROJECT,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			    STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_DIFF_UNIQ			,
				  # MODEL_ENTITY_COUNT,
				  MODEL_EDGE_COUNT,
				  # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
				  EXISTENTIAL_PRECISION,
				  ENTROPIC_RELEVANCE_RESTRICTED_ZO,
				  ENTROPIC_RELEVANCE_UNIFORM,
				  ENTROPIC_RELEVANCE_ZERO_ORDER,
				  gtype
				  )


rdo <- rename(rdo,ACTIVITY_RATIO_GOWER= EVENT_RATIO_GOWER)


rd <- subset(rdo, select=-c(Log,Log.Id,gtype))

rdnl <- subset(rdo, select=-c(Log,Log.Id,Log.Trace.Count,Log.Event.Count,gtype))

# reorder
rdnl <- rdnl %>%  select (
  			  ACTIVITY_RATIO_GOWER,
  			  TRACE_RATIO_GOWER_2,
  			  TRACE_RATIO_GOWER_3,
  			  TRACE_RATIO_GOWER_4,
  			  STRUCTURAL_SIMPLICITY_STOCHASTIC,
  			  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
				  TRACE_GENERALIZATION_DIFF_UNIQ,
				  EARTH_MOVERS_TRACEWISE,
				  # TRACE_OVERLAP_RATIO,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  ENTROPY_PRECISION_TRACEPROJECT,
				  TRACE_GENERALIZATION_FLOOR_5,
				  ENTROPY_FITNESS_TRACEPROJECT,
				  ENTROPIC_RELEVANCE_UNIFORM,
				  ENTROPIC_RELEVANCE_ZERO_ORDER,
				  ENTROPIC_RELEVANCE_RESTRICTED_ZO,
				  # MODEL_ENTITY_COUNT,
				  MODEL_EDGE_COUNT,
				  # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,  # test excl complexity
				  EXISTENTIAL_PRECISION			  
				  )



# Basic stats
pf("Data description after rescaling and selection")
withCallingHandlers(message = warning_handler, {
  rdd <- describe(rd,type=2,fast=T)
})
pfc(rdd)


# collinear columns such as TOR are excluded because of this
rmardia <- mvn(data=rd,mvnTest="mardia",univariateTest="AD")

pfc(rmardia)


# Principal Components

pcares <- prcomp(rd, scale = scalepca)

pcaind <- get_pca_ind(pcares)
pcavar <- get_pca_var(pcares)


pcanl <- prcomp(rdnl, scale = scalepca)

pfc(pcavar$contrib[,1:(nfactors+1)])

pcnlv <- get_pca_var(pcanl)
pfc(pcnlv$contrib[,1:nfactors])

#scree
# including log
print(fviz_eig(pcares) 
      + geom_hline(yintercept = 10))



prepfig("screeeig",ctlogns)
print(fviz_eig(pcanl) 
      + geom_hline(yintercept = 10))
postfig()

colours <- palette(rainbow(nfactors))




pcaPlots <- TRUE
ellipsePlot <- TRUE



if (ellipsePlot){
  pcaEllipsePlot("pca4ellipselog",ctlogns,"PCA1","PCA2",c(1,2),rdo$Log  )
  # "PCA1 (Adhesion)", "PCA2 (Simplicity)") +
  pcaEllipsePlot("pca4ellipselog23",ctlogns,"PCA2","PCA3",c(2,3), rdo$Log  )
  # "PCA2 (Simplicity)", "PCA3 (Entropy)") +
  pcaEllipsePlot("pca4ellipsedc",ctlogns,"PCA1","PCA2",c(1,2),rdo$gtype  )
  pcaEllipsePlot("pca4ellipsedc23",ctlogns,"PCA2","PCA3",c(2,3), rdo$gtype  )
}



if (pcaPlots){
fviz_pca_ind(pcares,
             col.ind = "cos2", # Color by the quality of representation
             gradient.cols =  colours ,
             repel = FALSE,     # Avoid text overlapping
		 label = "none",
             ) +   
	labs(title ="PCA", x = "PC1", y = "PC2")

fviz_pca_ind(pcanl,
             col.ind = "cos2", # Color by the quality of representation
             gradient.cols =  colours ,
             repel = FALSE,     # Avoid text overlapping
		 label = "none",
             ) +   
	labs(title ="PCA", x = "PC1", y = "PC2")




fviz_pca_var(pcares,
             col.var = "contrib", # Color by contributions to the PC
             gradient.cols = colours,
             repel = FALSE ,    # Avoid text overlapping
		 label = "none"
             )

fviz_pca_var(pcanl,
             col.var = "contrib", # Color by contributions to the PC
             gradient.cols = colours,
             repel = FALSE ,    # Avoid text overlapping
		 label = "none"
             )


fviz_pca_biplot(pcares, repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")


# 1 vs 2
fviz_pca_biplot(pcanl, repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")

# 2 vs 3
fviz_pca_biplot(pcanl, axes=c(2,3), repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")

}

pvc2 <- pcnlv$cos2[,1:nfactors]
pvct <- pcnlv$contrib[,1:nfactors]

if (nfactors == 5){
  colnames(pvc2) <- c("Adhesion","Relevance","Simplicity","Complexity","Thingy")
  colnames(pvct) <- c("Adhesion","Relevance","Simplicity","Complexity","Thingy")
  
}


if (nfactors == 4){
  colnames(pvc2) <- c("Adhesion","Relevance","Simplicity","Complexity")
  colnames(pvct) <- c("Adhesion","Relevance","Simplicity","Complexity")
}

if ( nfactors == 3 ){
  cn3 <- c("Adhesion\n(PCA1)","Thingy\n(PCA2)","Simplicity\n(PCA3)")
  colnames(pvc2) <- cn3
  colnames(pvct) <- cn3
  # rnct <- rownames(pvct)
  # rnct[1] <- c("Activity Ratio Gower (ARG)")
  # rnct[2] <- c("Trace Ratio Gower 2 (TRG2)")
  # rnct[3] <- c("Trace Ratio Gower 3 (TRG3)")
  # rnct[4] <- c("Trace Ratio Gower 4 (TRG4)")
  # rnct[5] <- c("Structural Simplicity incl. Stochastic (SSS)")
  # rnct[6] <- c("Structural Simplicity by Entity Count (SSENC)")
  # rnct[7] <- c("Structural Simplicity by Edge Count  (SSEDC)")
  # rnct[8] <- c("Generalization by Trace Uniqueness (TGDU)")
  # rnct[9] <- c("Earth Movers With Play-out Trace (EMT)")
  # rnct[10] <- c("Trace Probability Mass Overlap (TMO)")
  # rnct[11] <- c("Play-out Entropy Intersection Precision (HIPT)")
  # rnct[12] <- c("Play-out Entropy Intersection Fitness (HIFT)")
  # rnct[13] <- c("Play-out Entropy Project Precision (HJPT)")
  # rnct[14] <- c("Generalization by Trace Floor 5 (TGF5)")
  # rnct[15] <- c("Play-out Entropy Project Fitness (HJFT)")
  # rownames(pvct) <- rnct
}

if (nfactors == 2){
  colnames(pvc2) <- c("Adhesion","Simplicity")
  colnames(pvct) <- c("Adhesion","Simplicity")
}


corheatmap(pvc2,ctlogns,"cos2")
corheatmap(pvct,ctlogns,"contrib")

cvsexpfile <- ''
if (matchSampleSizes){
  csvexpfile <- "expn_c4.csv"
}else{
  csvexpfile <- "expn_nm_c4.csv"
}
write.csv(rdnl, paste(workingPath,csvexpfile,sep=""), row.names=FALSE)

