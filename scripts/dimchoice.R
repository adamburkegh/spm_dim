  library(dplyr)
  library(tidyr)
  library(factoextra)
  library(readr)
  library(stats)
  library(rgl)
  
  hpath = "c:/Users/burkeat/bpm/"
  # hpath = "c:/Users/Adam/bpm/"
  
  workingPath = paste(hpath,"bpm-dimensions-lab/var/",sep="")
  resultsPath = paste(hpath,"bpm-dimensions-lab/results/",sep="")
  
  
  resfile <- paste(workingPath,"dimchoice.txt",sep="")
  write_lines("Dimensional choice",resfile)
  
  nfactors <- 3
  
  
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
  
  pf <- function(msg){
    write_lines(msg,resfile,append=TRUE)
    write_lines(" ",resfile,append=TRUE)
  }
  

  imgname <- function(fprefix,artname){
    paste(workingPath,fprefix,"_",artname,".png",sep="")
  }
  
  prepfig <- function(fprefix,artname, width=30, height=20, mar=c(1,1,1,1))
  {
      par(mar=mar)
      png( paste(workingPath,fprefix,"_",artname,".png",sep=""), 
          res=300, width=width, height=height, units='cm')
      # svg( paste(workingPath,fprefix,"_",artname,".svg",sep=""))
  }
  
  postfig <- function()
  {
      dev.off()
  }
  
  
  
  
  triplot <- function(ctpc, theta=30, phi=30, zoom = 0.9,
                      labloc=ctpc$rotation[,1:3]){
    plot3d(ctpc$rotation,
           # xlab="Adhesion",ylab="Simplicity",zlab="Entropy")
           xlab="PCA1",ylab="PCA2",zlab="PCA3")
  
    text3d(labloc, 
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
  
  wpath <- function(fpath,dpath=workingPath){
    paste(dpath,fpath,sep="")
  }
  
  
  
  
  #rdnl = read.csv( paste(workingPath,"expn_c3.csv",sep=""),
  #                 strip.white=TRUE)
  rdnl = read.csv( paste(workingPath,"expn_nm_c3.csv",sep=""),
                   strip.white=TRUE)
  
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
  expc3dr <- rename(expc3dr, XPU=ALPHA_PRECISION_UNRESTRICTED_ZERO,
                             TGF5=TRACE_GENERALIZATION_FLOOR_5,
                             TGDU=TRACE_GENERALIZATION_DIFF_UNIQ,
                             HRZ=ENTROPIC_RELEVANCE_ZERO_ORDER,
                             TRG2=TRACE_RATIO_GOWER_2,
                             SSEDC=STRUCTURAL_SIMPLICITY_EDGE_COUNT)
  
  
  
  expc2dr <- rdnl2 %>% select(# ALPHA_PRECISION_UNRESTRICTED_ZERO,
                            TRACE_GENERALIZATION_FLOOR_5,
                            ENTROPY_FITNESS_TRACEPROJECT,
                            ENTROPY_PRECISION_TRACEPROJECT,
                            # ENTROPIC_RELEVANCE_ZERO_ORDER,
                            ACTIVITY_RATIO_GOWER,
                            TRACE_RATIO_GOWER_2,
                            STRUCTURAL_SIMPLICITY_EDGE_COUNT )
  expc2dr <- rename(expc2dr, 
                    TGF5=TRACE_GENERALIZATION_FLOOR_5,
                    HJFT=ENTROPY_FITNESS_TRACEPROJECT,
                    HJPT=ENTROPY_PRECISION_TRACEPROJECT,
                    # ENTROPIC_RELEVANCE_ZERO_ORDER,
                    ARG=ACTIVITY_RATIO_GOWER,
                    TRG2=TRACE_RATIO_GOWER_2,
                    SSEDC=STRUCTURAL_SIMPLICITY_EDGE_COUNT)
  
  
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
  eval3dr <- rename(eval3dr, XPU=ALPHA_PRECISION_UNRESTRICTED_ZERO,
                             TGF5=TRACE_GENERALIZATION_FLOOR_5,
                             TGDU=TRACE_GENERALIZATION_DIFF_UNIQ,
                             HRZ=ENTROPIC_RELEVANCE_ZERO_ORDER,
                             TRG2=TRACE_RATIO_GOWER_2,
                             SSEDC=STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                             EM=EARTH_MOVERS)  
  
  
  
  eval2dr <- rdev2 %>% select(# ALPHA_PRECISION_UNRESTRICTED_ZERO,
                           TRACE_GENERALIZATION_FLOOR_5,
                           # ENTROPIC_RELEVANCE_ZERO_ORDER,
                           STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                           EARTH_MOVERS,
                           ENTROPY_PRECISION,
                           ENTROPY_RECALL
  )
  eval2dr <- rename(eval2dr, TGF5=TRACE_GENERALIZATION_FLOOR_5,
                             SSEDC=STRUCTURAL_SIMPLICITY_EDGE_COUNT,
                             EM=EARTH_MOVERS,
                             HP=ENTROPY_PRECISION,
                             HF=ENTROPY_RECALL)
  
  
  # Native metrics
  expc3nm <- rdnl %>% select(# ALPHA_PRECISION_UNRESTRICTED_ZERO,
    TRACE_GENERALIZATION_FLOOR_5,
    # TRACE_GENERALIZATION_DIFF_UNIQ,
    ENTROPIC_RELEVANCE_ZERO_ORDER,
    # TRACE_RATIO_GOWER_2,
    STRUCTURAL_SIMPLICITY_EDGE_COUNT
    # MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY
  )
  
  expc3nm <- rename(  expc3nm,
                      TGF5=TRACE_GENERALIZATION_FLOOR_5,
                      HRZ=ENTROPIC_RELEVANCE_ZERO_ORDER,
                      SSEDC=STRUCTURAL_SIMPLICITY_EDGE_COUNT)
  
  eval3nm <- rdev %>% select( # ALPHA_PRECISION_UNRESTRICTED_ZERO,
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
  
  
  
  pcexp3dr <- prcomp(expc3dr,scale=TRUE)
  
  pceval3dr <- prcomp(eval3dr,scale=TRUE)
  
  pcexp2dr <- prcomp(expc2dr,scale=TRUE)
  
  pceval2dr <- prcomp(eval2dr,scale=TRUE)
  
  pcexp3nm <- prcomp(expc3nm,scale=TRUE)
  
  pceval3nm <- prcomp(eval3nm,scale=TRUE)
  
  pf("Dimension realist factors explore cycle 3")
  fdr <- pcexp3dr$rotation[,1:nfactors] / pcexp3dr$scale
  pf (fdr)
  pf("Dimension realist constant explore cycle 3")
  km <- pcexp3dr$rotation[,1:nfactors] * pcexp3dr$center / pcexp3dr$scale
  k <- apply(km,2,sum)
  pf(k)
  
  pf("Dimension realist min/max empirical explore cycle 3")
  pf( apply(pcexp3dr$x[,1:nfactors],2,min) )
  pf( apply(pcexp3dr$x[,1:nfactors],2,max) )
  
  pf("Dimension realist min/max theoretical explore cycle 3")
  maxentr <- 60
  # Largest in experiments is 52.5
  # > describe(expc3$ENTROPIC_RELEVANCE_ZERO_ORDER)
  # vars    n mean   sd median trimmed  mad  min   max range skew kurtosis   se
  # X1    1 4707 9.23 8.47   6.39    7.98 4.51 1.87 52.55 50.68 3.31    14.16 0.12
  pf(paste("Using max entropic relevance ", maxentr) )
  
  pf("Minimums")
  fdr <- pcexp3dr$rotation[,1:nfactors] / pcexp3dr$scale
  fdr[,3] <- fdr[,3] * -1
  k[3] <- k[3] * -1
  mn_ad   <- sum( c(0,0,0,maxentr,0,0) * fdr[,1] ) - k[1] 
  mn_rel  <- sum( c(1,0,0,maxentr,1,1) * fdr[,2] ) - k[2]
  mn_simp <- sum( c(1,1,0,0,1,0) * fdr[,3] ) - k[3]
  pf ( paste("Adhesion", mn_ad  ) )
  pf ( paste("Relevance", mn_rel ) )
  pf ( paste("Simplicity", mn_simp  ) )
  
  pf("Maximums")
  mx_ad   <- sum( c(1,1,1,0,1,1) * fdr[,1] ) - k[1]
  mx_rel  <- sum( c(0,1,1,0,0,0) * fdr[,2] ) - k[2]
  mx_simp <- sum( c(0,0,1,1,0,1) * fdr[,3] ) - k[3]
  pf ( paste("Adhesion", mx_ad ) )
  pf ( paste("Relevance", mx_rel ) )
  pf ( paste("Simplicity", mx_simp ) )
  
  pf("Factors with min/max scaling 1/(mx-mn)")
  pf("Adhesion")
  pf( fdr[,1] / (mx_ad - mn_ad )  )
  pf("Relevance")
  pf( fdr[,2] / (mx_rel - mn_rel )  )
  pf("Simplicity")
  pf( fdr[,3] / (mx_simp - mn_simp )  )
  
  pf("Constants with min/max scaling 1/(mx-mn)")
  pf("Adhesion")
  pf( k[1] / (mx_ad - mn_ad )  )
  pf("Relevance")
  pf( k[2] / (mx_rel - mn_rel )  )
  pf("Simplicity")
  pf( k[3] / (mx_simp - mn_simp )  )
  
  
  # These biplots are perfect candidates for a function - but I get the notorious
  # blank image bug when I do that
  
  # Dimensional Realism
  
  suffix = "exp_dr"
  ctpc = pcexp3dr
  prepfig("biplot",paste(suffix,"12",sep=""))
  fviz_pca_biplot(ctpc, repel = FALSE, margins=c(10,21),
                  col.var = "#2E9FDF", # Variables color
                  col.ind = "#696969",  # Individuals color
                  label = "var")
  postfig()
  
  prepfig("biplot",paste(suffix,"23",sep=""))
  fviz_pca_biplot(ctpc, axes=c(2,3), repel = FALSE,
                  col.var = "#2E9FDF", # Variables color
                  col.ind = "#696969",  # Individuals color
                  label = "var")
  postfig()
  
  prepfig("biplot",paste(suffix,"13",sep=""))
  fviz_pca_biplot(ctpc, axes=c(1,3), repel = FALSE,
                  col.var = "#2E9FDF", # Variables color
                  col.ind = "#696969",  # Individuals color
                  label = "var")
  postfig()
  
  suffix = "eval_dr"
  ctpc = pceval3dr
  labsize = 6
  prepfig("biplot",paste(suffix,"12",sep=""))
  fviz_pca_biplot(ctpc, repel = FALSE, margins=c(10,21),
                  col.var = "#696969", # Variables color
                  col.ind = "#2E9FDF",  # Individuals color
                  label = "var", labelsize = labsize,
                  title = "" ) #PCA Biplot, selected discovery metrics, 1st vs 2nd component")
  # fn = imgname("biplot",paste(suffix,"12",sep=""))
  # ggsave( filename=fn, device="png" )
  postfig()
  
  prepfig("biplot",paste(suffix,"23",sep=""))
  fviz_pca_biplot(ctpc, axes=c(2,3), repel = FALSE,
                  col.var = "#696969", # Variables color
                  col.ind = "#2E9FDF",  # Individuals color
                  label = "var", labelsize = labsize,
                  title = "" ) # "PCA Biplot, selected discovery metrics, 2nd vs 3rd component")
  postfig()
  
  prepfig("biplot",paste(suffix,"13",sep=""))
  fviz_pca_biplot(ctpc, axes=c(1,3), repel = FALSE,
                  col.var = "#696969", # Variables color
                  col.ind = "#2E9FDF",  # Individuals color
                  label = "var", labelsize = labsize,
                  title = "" ) # "PCA Biplot, selected discovery metrics, 1st vs 3rd component")
  postfig()




# Native metrics
labloc=pcexp3nm$rotation
labloc[1,1] <- pcexp3nm$rotation[1,1] -0.07 # move TRG5 left
labloc[1,2] <- pcexp3nm$rotation[1,2] -0.02 # move TRG5 down
labloc[2,2] <- pcexp3nm$rotation[2,2] -0.05 # move HRZ down
labloc[2,3] <- pcexp3nm$rotation[2,3] -0.06 # move HRZ back
labloc[3,3] <- pcexp3nm$rotation[3,3] +0.07 # move SSEDC forward
# triplot(pcexp3nm,theta=25, phi=20,zoom=1.0,labloc=labloc)
# Needs manual resize of window :(
# rgl.snapshot(filename = wpath("triplot_exp3_nm.png") )

suffix = "exp_nm"
ctpc = pcexp3nm
prepfig("biplot",paste(suffix,"12",sep=""))
fviz_pca_biplot(ctpc, repel = FALSE, margins=c(10,21),
                      col.var = "#696969", # Variables color
                      col.ind = "#2E9FDF",  # Individuals color
                      title = "Native Metrics",
                      label = "var")
postfig()

prepfig("biplot",paste(suffix,"23",sep=""))
fviz_pca_biplot(ctpc, axes=c(2,3), repel = FALSE,
                col.var = "#696969", # Variables color
                col.ind = "#2E9FDF",  # Individuals color
                label = "var")
postfig()

prepfig("biplot",paste(suffix,"13",sep=""))
fviz_pca_biplot(ctpc, axes=c(1,3), repel = FALSE,
                col.var = "#696969", # Variables color
                col.ind = "#2E9FDF",  # Individuals color
                label = "var")
postfig()

suffix = "eval_nm"
ctpc = pceval3nm
prepfig("biplot",paste(suffix,"12",sep=""))
fviz_pca_biplot(ctpc, repel = FALSE, margins=c(10,21),
                col.var = "#696969", # Variables color
                col.ind = "#2E9FDF",  # Individuals color
                label = "var")
postfig()

prepfig("biplot",paste(suffix,"23",sep=""))
fviz_pca_biplot(ctpc, axes=c(2,3), repel = FALSE,
                col.var = "#696969", # Variables color
                col.ind = "#2E9FDF",  # Individuals color
                label = "var")
postfig()

prepfig("biplot",paste(suffix,"13",sep=""))
fviz_pca_biplot(ctpc, axes=c(1,3), repel = FALSE,
                col.var = "#696969", # Variables color
                col.ind = "#2E9FDF",  # Individuals color
                label = "var")
postfig()

