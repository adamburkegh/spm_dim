# spd\_dim

Source code and results investigating stochastic process quality dimensions in process mining. It includes implementations of a number of exploratory conformance measures for stochastic process models in the form of Generalized Stochastic Petri Nets. 

This also includes a genetic algorithm for mining stochastic process models, called the Stochastic Evolutionary Tree Miner (SETM). 

The paper describing this experiment is "Burke, A., Leemans, SJJ, Wynn, M.T and van der Aalst, W.M.T - Stochastic Process Model Quality Dimensions: An Experimental Study (forthcoming)".

# Development Setup and Installation

## Gradle and Java

Checkout [`prom-helpers`](https://github.com/adamburkegh/prom-helpers) 

In `prom-helpers` ./gradlew test ./gradlew publishToMavenLocal

In `spd\_dim`, ./gradlew test

## R 

Statistical analysis and visualization code is in `scripts`.

## Results

Measurements and paradigm models are in `results/` and `models/` respectively.


## License

This project is licensed under the GNU Public License (GPL). The source code extends (and forks) the [ProM EvolutionaryTreeMiner](https://svn.win.tue.nl/repos/prom/Packages/EvolutionaryTreeMiner/Trunk) by Buijs (which is LGPL). 

