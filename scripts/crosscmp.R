library(dplyr)
library(tidyr)
library(corrplot)
library(factoextra)
library(stats)
library(rgl)
library(RColorBrewer)
# library(caret) for findCorrelations()


resetplots <- function(){
  dev.off()
  par("mar") 
  par(mar=c(1,1,1,1))
}

pcaheatmap <- function(pcv){
  scale <- 'none'
  # scale <- 'row'   'col'
  col= colorRampPalette(brewer.pal(8, "Blues"))(25)
  
  # margins=c(10,21)
  heatmap(pcv[,1:nfactors], Colv = NA, Rowv = NA, col= col,
          cexRow = 1.0, cexCol = 1.5 , 
          scale = scale)
}

triplot <- function(ctpc, theta=30, phi=30, zoom = 0.9){
  plot3d(ctpc$rotation,
         # xlab="Adhesion",ylab="Simplicity",zlab="Entropy")
         xlab="PCA1",ylab="PCA2",zlab="PCA3")
  
  text3d(ctpc$rotation[,1:3], 
         texts=rownames(ctpc$rotation), 
         col="blue", 
         cex=0.8)
  
  
  coords <- NULL
  for (i in 1:nrow(ctpc$rotation)) {
    coords <- rbind(coords, 
                    rbind(c(0,0,0),
                          ctpc$rotation[i,1:3]))
  }
  
  
  lines3d(coords, 
          col="blue", 
          lwd=1)
  
  view3d(theta = theta, phi = phi, zoom = zoom)
}

# Coordinates of the individuals
coord_func <- function(ind, loadings){
  r <- loadings*ind
  apply(r, 2, sum)
}


pcapred <- function(rd, ctpc){
  scaledind <- scale(rd, 
                     center = ctpc$center,
                     scale  = ctpc$scale)
  loadings <- ctpc$rotation
  rdcoord <- apply(scaledind, 1, coord_func, loadings )
  t(rdcoord)
}

minmaxscale <- function(x,buffer=1) {
  range = buffer* (max(x)-min(x))
  return((x- min(x)) /range)
}


hpath = "c:/Users/burkeat/bpm/"
# hpath = "c:/Users/Adam/bpm/"

workingPath = paste(hpath,"bpm-dimensions-lab/var/",sep="")
resultsPath = paste(hpath,"bpm-dimensions-lab/results/",sep="")


rdnl = read.csv( paste(workingPath,"expn_c3.csv",sep=""),
                 strip.white=TRUE)
# rdnl = read.csv( paste(workingPath,"expn_nm_c3.csv",sep=""),
#                 strip.white=TRUE)

rdnl2 = read.csv( paste(workingPath,"expn_c2.csv",sep=""),
                  strip.white=TRUE)

rdev = read.csv( paste(workingPath,"eval_c3.csv",sep=""),
                 strip.white=TRUE)

rdev2 = read.csv( paste(workingPath,"eval_c2.csv",sep=""),
                 strip.white=TRUE)

# ACTIVITY_RATIO_GOWER,                   # exclude - correlation with TRGx
# TRACE_RATIO_GOWER_2,                    # KEEP - TRG correlation group
# TRACE_RATIO_GOWER_3,                    # exclude - correlation with ARG,TRGx
# TRACE_RATIO_GOWER_4,                    # exclude - correlation with ARG,TRGx
# STRUCTURAL_SIMPLICITY_STOCHASTIC,       # exclude - correlation with SSENC,SSEDC
# STRUCTURAL_SIMPLICITY_ENTITY_COUNT,     # exclude - correlation with SSENC,SSS
# STRUCTURAL_SIMPLICITY_EDGE_COUNT,       # KEEP - SIMP correlation group
# TRACE_GENERALIZATION_DIFF_UNIQ,         # KEEP - TOR/EMT correlation group
# EARTH_MOVERS_TRACEWISE,                 # exclude - correlation with TOR,TGDU
# TRACE_OVERLAP_RATIO,                    # exclude - correlation with EMT,TGDU
# ENTROPY_PRECISION_TRACEWISE,            # exclude - correlation with APU0
# ENTROPY_FITNESS_TRACEWISE,              # exclude - correlation with TGF5
# ENTROPY_PRECISION_TRACEPROJECT,         # exclude - correlation with APU0, HJFT
# TRACE_GENERALIZATION_FLOOR_5,           # KEEP over correlated EMT - correlates better with EM in eval
# ENTROPY_FITNESS_TRACEPROJECT,           # exclude - correlation with APU0, HJPT
# ENTROPIC_RELEVANCE_UNIFORM,             # exclude - correlation with HRZ,HRR
# ENTROPIC_RELEVANCE_ZERO_ORDER,          # KEEP - entropic relevance correlation group
# ENTROPIC_RELEVANCE_RESTRICTED_ZO,       # exclude - correlation with HRU,HRZ
# MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,  # include?? Known relation
# ALPHA_PRECISION_UNRESTRICTED_ZERO			  # KEEP - entropy correlation group


# DR view
expc3dr <- rdnl %>% select(ALPHA_PRECISION_UNRESTRICTED_ZERO,
                         TRACE_GENERALIZATION_FLOOR_5,
                         TRACE_GENERALIZATION_DIFF_UNIQ,
                         ENTROPIC_RELEVANCE_ZERO_ORDER,
                         TRACE_RATIO_GOWER_2,
                         STRUCTURAL_SIMPLICITY_EDGE_COUNT
                         # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY
                         )

expc2dr <- rdnl2 %>% select(# ALPHA_PRECISION_UNRESTRICTED_ZERO,
                          TRACE_GENERALIZATION_FLOOR_5,
                          ENTROPY_FITNESS_TRACEPROJECT,
                          ENTROPY_PRECISION_TRACEPROJECT,
                          # ENTROPIC_RELEVANCE_ZERO_ORDER,
                          ACTIVITY_RATIO_GOWER,
                          TRACE_RATIO_GOWER_2,
                          STRUCTURAL_SIMPLICITY_EDGE_COUNT )

# DR view
eval3dr <- rdev %>% select(ALPHA_PRECISION_UNRESTRICTED_ZERO,
                         TRACE_GENERALIZATION_FLOOR_5,
                         TRACE_GENERALIZATION_DIFF_UNIQ,
                         ENTROPIC_RELEVANCE_ZERO_ORDER,
                         TRACE_RATIO_GOWER_2,
                         STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                         # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                         EARTH_MOVERS
                         # ,ENTROPY_PRECISION, # excluded for vagueness + expense - better proxies for PCA dim
                         # ENTROPY_RECALL      # excluded for vagueness + expense - better proxies for PCA dim
)
  



eval2dr <- rdev2 %>% select(# ALPHA_PRECISION_UNRESTRICTED_ZERO,
                         TRACE_GENERALIZATION_FLOOR_5,
                         # ENTROPIC_RELEVANCE_ZERO_ORDER,
                         STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                         EARTH_MOVERS,
                         ENTROPY_PRECISION,
                         ENTROPY_RECALL
)


pcexp3dr <- prcomp(expc3,scale=TRUE)

pceval3dr <- prcomp(eval3,scale=TRUE)

pcexp2dr <- prcomp(expc2,scale=TRUE)

pceval2dr <- prcomp(eval2,scale=TRUE)


# NM view

expc3nm <- rdnl %>% select(# ALPHA_PRECISION_UNRESTRICTED_ZERO,
                           TRACE_GENERALIZATION_FLOOR_5,
                           # TRACE_GENERALIZATION_DIFF_UNIQ,
                           ENTROPIC_RELEVANCE_ZERO_ORDER,
                           # TRACE_RATIO_GOWER_2,
                           STRUCTURAL_SIMPLICITY_EDGE_COUNT
                           # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY
)

eval3nm <- rdev %>% select( ALPHA_PRECISION_UNRESTRICTED_ZERO,
                             # TRACE_GENERALIZATION_FLOOR_5,
                             # TRACE_GENERALIZATION_DIFF_UNIQ,
                             ENTROPIC_RELEVANCE_ZERO_ORDER,
                             # TRACE_RATIO_GOWER_2,
                             STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                             # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY,
                             EARTH_MOVERS
                             # ,ENTROPY_PRECISION, # excluded for vagueness + expense - better proxies for PCA dim
                             # ENTROPY_RECALL      # excluded for vagueness + expense - better proxies for PCA dim
)

pcexp3nm <- prcomp(expc3nm,scale=TRUE)

pceval3nm <- prcomp(eval3nm,scale=TRUE)





resetplots()


# warning: interactive from here down

# ctpc <- pceval2
# ctpc <- pcexp3dr
ctpc <- pceval3dr
# ctpc <- pcexp3nm
# ctpc <- pceval3nm


pcv <- get_pca_var(ctpc)



nfactors <- 3

pcaheatmap(pcv$contrib)
pcaheatmap(pcv$cos2)
pcaheatmap(pcv$cor)
pcaheatmap(ctpc$rotation)

# pairs(ica3$S )

# scree
print(fviz_eig(ctpc) 
      + geom_hline(yintercept = 10))


fviz_pca_biplot(ctpc, repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")

fviz_pca_biplot(ctpc, axes=c(2,3), repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")

fviz_pca_biplot(ctpc, axes=c(1,3), repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")

fviz_pca_biplot(ctpc, axes=c(1,4),repel = FALSE,
                col.var = "#2E9FDF", # Variables color
                col.ind = "#696969",  # Individuals color
                label = "var")



triplot(ctpc,theta=25, phi=20)

