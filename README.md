# spd\_dim

Source code and results investigating stochastic process quality dimensions in process mining. It includes implementations of a number of exploratory conformance measures for stochastic process models in the form of Generalized Stochastic Petri Nets. 

This also includes a genetic algorithm for mining stochastic process models, called the Stochastic Evolutionary Tree Miner (SETM). 

The main paper describing these experiments is: Burke, Adam T., Sander J. J. Leemans, Moe T. Wynn, Wil M. P. van der Aalst, and Arthur H. M. ter Hofstede. 2024. “A Chance for Models to Show Their Quality: Stochastic Process Model-Log Dimensions.” Information Systems 124: 102382. [doi:10.1016/j.is.2024.102382](https://doi.org/10.1016/j.is.2024.102382).

# Development Setup and Installation

## Gradle and Java

Checkout [`prom-helpers`](https://github.com/adamburkegh/prom-helpers) and [`prob-process-tree`](https://github.com/adamburkegh/prob-process-tree)

In `prob-process-tree`, `./gradlew test ; ./gradlew publishToMavenLocal`

In `prom-helpers`, `./gradlew test ; ./gradlew publishToMavenLocal`

In `spd_dim`, `./gradlew test`

## R 

Statistical analysis and visualization code is in `scripts`.

# Running

Experiments are run with `ExperimentRunner`. It depends on a configuration property file, with examples files in `config`.

A standalone command line interface to SETM is in `SETMCommandLine`.

The class `SETMReporter` extracts experimental data from XML `mrun_*` files to pipe-separated files for import into `R` or other tools.

# Results

Measurements and paradigm models are in `results/` and `models/` respectively.


# License

This project is licensed under the Lesser GNU Public License (LGPL). The source code extends (and forks) the [ProM EvolutionaryTreeMiner](https://svn.win.tue.nl/repos/prom/Packages/EvolutionaryTreeMiner/Trunk) by J.C.A.M. Buijs (which is LGPL). 

