library(dplyr)
library(ggplot2)
library(readr)
library(factoextra)
library(MVN)
library(psych)
library(RColorBrewer)
###

workingPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/var/"
resultsPath = "c:/Users/burkeat/bpm/bpm-dimensions-lab/results/"


exportPic = TRUE

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

filter_for_BPIC2013_closed <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	#  == EARTH_MOVERS_TRACES_nz EMT
	# TRACE_GENERALIZATION_FLOOR_1 TGF1 == TRACE_GENERALIZATION_FLOOR_5 TGF5 
	#  == TRACE_GENERALIZATION_FLOOR_10 TGF10
	# As this is an invalid matrix for factor analysis, exclude.
	#
	result <- rd %>% select (
				  TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_DIFF_UNIQ  )

}

filter_for_Sepsis <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	#  == EARTH_MOVERS_TRACES_nz EMT == ENTROPY_PRECISION_TRACEWISE EPT
	#  == ENTROPY_FITNESS_TRACEWISE EFT
	# TRACE_GENERALIZATION_FLOOR_5 TGF5 == TRACE_GENERALIZATION_FLOOR_10 TGF10
	# As this is an invalid matrix for factor analysis, exclude.
	#
	result <- rd %>% select (
				  TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_DIFF_UNIQ  )

}


  

filter_for_all <- function(rd){
	# Based on the correlation matrix, these columns are identical
	# TRACE_OVERLAP_RATIO TOR == TRACE_PROBMASS_OVERLAP TMO 
	# As this is an invalid matrix for factor analysis, exclude.
	#
	result <- rd %>% select (
				  TRACE_PROBMASS_OVERLAP, 
			        ACTIVITY_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  TRACE_GENERALIZATION_FLOOR_10,
				  TRACE_GENERALIZATION_DIFF_UNIQ  )
}


corheatmap <- function(pv,sel,main){
	prepfig( paste("heatmap",main,sep=""),sel)
	col= colorRampPalette(brewer.pal(8, "Blues"))(25)
	heatmap(pv, Colv = NA, Rowv = NA,margins=c(10,21), col= col) #,
		# main=paste(sel,main))
	postfig()
}



dev.off()
dev.new()



rundata = read.csv( paste(workingPath,"hpc.psv", sep=""),
            sep ="|", strip.white=TRUE)

logstats <- read.csv( paste(resultsPath,"logstats.csv",sep=""))

rundata <- merge(rundata,logstats)

# ctlog <- 'BPIC2013 closed'
# ctlog <- 'BPIC2013 incidents'
# ctlog <- 'BPIC2018 control'
# ctlog <- 'BPIC2018 reference'
# ctlog <- 'Sepsis'

# ctlogns <- gsub(" ","_",ctlog)

# ctlog <- 'BPIC2013_closed and Sepsis'
ctlog <- 'BPIC2013 closed & incidents, BPIC2018 control & reference, and Sepsis'

# ctlogns <- 'b2013c_sepsis'  
ctlogns <- 'all'
# ctlogns <- 'discorand'


nfactors <- 4


scalepca <- TRUE

resfile <-  paste(workingPath,"pca",nfactors,ctlogns,".txt",sep="") 

write_lines(paste(ctlogns,"n=",nfactors),resfile)

pf <- function(msg){
    write_lines(msg,resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}


pfc <- function(msg){
    write_lines(capture.output(msg),resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
}

creators <- unique(rundata$Artifact.Creator)

pf("Creators")
pf(creators)

rundata$EARTH_MOVERS_TRACES_nz <- ifelse(rundata$EARTH_MOVERS_TRACEWISE < 0, 0, 
				rundata$EARTH_MOVERS_TRACEWISE)


rd <- rundata %>% filter (Log == ctlog) # %>% 
# rd <- rundata %>% filter (Log == 'BPIC2013 closed' || Log == 'Sepsis') %>% 
# rd <- rundata %>% filter ( !grepl('setm',Artifact.Creator) ) 
# %>%
#		      filter ( !grepl('rando',Artifact.Creator) ) %>%


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
				  # TRACE_OVERLAP_RATIO,
				  TRACE_PROBMASS_OVERLAP,
				  EARTH_MOVERS_TRACES_nz,
			        EVENT_RATIO_GOWER,
				  TRACE_RATIO_GOWER_2,TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_4,
				  ENTROPY_PRECISION_TRACEWISE,
				  ENTROPY_FITNESS_TRACEWISE,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  #TRACE_GENERALIZATION_FLOOR_1,
				  TRACE_GENERALIZATION_FLOOR_5,
				  #TRACE_GENERALIZATION_FLOOR_10,
				  TRACE_GENERALIZATION_DIFF_UNIQ				)

rdo <- rename(rdo,EARTH_MOVERS_TRACES = EARTH_MOVERS_TRACES_nz,
			ACTIVITY_RATIO_GOWER= EVENT_RATIO_GOWER)


rd <- rdo[,-1:-2]

rdnl <- rdo[,-1:-4]

# reorder
rdnl <- rdnl %>%  select (
				  TRACE_GENERALIZATION_FLOOR_5,
			        ACTIVITY_RATIO_GOWER,
				  STRUCTURAL_SIMPLICITY_ENTITY_COUNT,
				  STRUCTURAL_SIMPLICITY_EDGE_COUNT,
			        STRUCTURAL_SIMPLICITY_STOCHASTIC,
				  #TRACE_GENERALIZATION_FLOOR_1,
				  #TRACE_GENERALIZATION_FLOOR_10,
				  EARTH_MOVERS_TRACES,
				  TRACE_PROBMASS_OVERLAP,
				  TRACE_GENERALIZATION_DIFF_UNIQ				,
				  ENTROPY_PRECISION_TRACEWISE,
				  TRACE_RATIO_GOWER_4,
				  TRACE_RATIO_GOWER_3,
				  TRACE_RATIO_GOWER_2,
				  ENTROPY_FITNESS_TRACEWISE)

pf(paste("Sample size n =",nrow(rd)))

# Basic stats
rdd <- describe(rd,type=2,fast=T)
pfc(rdd)

# collinear columns such as TOR are excluded because of this
rmardia <- mvn(data=rd,mvnTest="mardia",univariateTest="AD")

pfc(rmardia)

# Principal Components

pcares <- prcomp(rd, scale = scalepca)

pcaind <- get_pca_ind(pcares)
pcavar <- get_pca_var(pcares)


pcanl <- prcomp(rdnl, scale = scalepca)

pfc(pcavar$contrib[,1:nfactors+1])

pcnlv <- get_pca_var(pcanl)
pfc(pcnlv$contrib[,1:nfactors+1])

#scree
#fviz_eig(pcares)

prepfig("screeeig",ctlogns)
fviz_eig(pcanl)
postfig()

colours <- palette(rainbow(nfactors))

pcaPlots <- FALSE
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



fviz_pca_ind(pcares,
             repel = FALSE,     # Avoid text overlapping
		 label = "none",
		 addEllipses=TRUE, ellipse.level=0.95,
		 habillage = rdo$Log
             ) +   
	labs(title ="", x = "S1-Fitness", y = "Precision")



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


fviz_pca_biplot(pcanl, repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")
}

pvc2 <- pcnlv$cos2[,1:nfactors]
pvct <- pcnlv$contrib[,1:nfactors]

# colnames(pv) <- c("Adaptive Fit","Overlap","Simplicity","Trace Profile")
colnames(pvc2) <- c("S1-Fitness","Precision","Simplicity","Trace Profile")
colnames(pvct) <- c("S1-Fitness","Precision","Simplicity","Trace Profile")



corheatmap(pvc2,ctlogns,"cos2")
corheatmap(pvct,ctlogns,"contrib")


