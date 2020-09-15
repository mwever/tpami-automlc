# Automated Machine Learning for Multi-Label Classification: Overview and Benchmark

This project provides a platform for benchmarking different optimizers for the task of automated machine learning ensuring all optimizers to work on the same set of potential solution candidates. The implementation is based on the Java open-source library [AILibs](https://github.com/starlibs/AILibs), providing the basic technical support for describing search spaces, HTN planning and heuristic search algorithms, as well as the infrastructure for synchronizing the execution of a benchmarking suite on a distributed system.

The benchmark distinguishes itself from previously published benchmark in the way how the optimizers are integrated with the benchmarking system. While all optimizers share the same routine for executing candidate solutions, the benchmark works in a cross-platform fashion, i.e. although the benchmark and the execution of candidate solutions is implemented in Java, optimizers available in Python can be benchmarked within the system. The search space and recursive structures of the search space are automatically translated into a format understandable to the respective optimizers. The inter-platform communication is done via the [Google ProtoBuf](https://developers.google.com/protocol-buffers) library which offers interfaces for various platforms. Thereby, the communication link only transfers the execution request to the benchmarking system allowing to share the same evaluation routine for all the optimizers. Another advantage is that it also allows for live-tracking the performance of the optimizers, logging each evaluated candidate and monitoring the current incumbent at any point in time.


## Quickstart - Setup

Prerequisites: Due to certain dependencies requiring a Linux operating system, the execution of optimizers in Python is not supported for Windows nor MacOS. However, optimizers available in Java can still be executed on Windows or MacOS. Running a Linux OS, you may execute Python optimizers as well. To this end, please use the SingularityRecipe to build a container in order to ensure all the dependencies necessary for running the benchmark are available.

### Preparing the Singularity Container

In order to set up a Singularity Container, please ensure that you have administrator permissions and installed the [Singularity Container](https://sylabs.io/guides/3.6/user-guide/) software on your computer. (Hint: On Ubuntu you can simply install it via `sudo apt install singularity-container`. Once you have Singularity Container installed on your system, follow these steps in order to create the container:

1. Build a singularity container from the provided recipe file: `sudo singularity build automlc.sif SingularityRecipe`. 
2. Once the container is built, you can now proceed to run a shell within the Singularity container like this: `singularity shell automlc.sif` (No sudo this time).
3. Please make sure that your current folder is mounted into the Singularity container. You may prove this by typing `dir`. If the files of the project's root directory are printed on your command line, everything should be fine.
4. You are now prepared to run the tasks via the gradle wrapper. For further steps please have a look at the subsequent documentation.

### Test your Setup

Once you have prepared 

## Showing Results

### Inspecting the Search Space
The configuration files containing the specification of the search space are contained in the folder `searchspace/`. For this, the root file is `searchspace/meka-all.json` and (recursively) includes the remaining configuration files

#### Export to Gephi to visualize the search space as a DAG

Gephi is a graph modeling and visualization tool. You can export the search space as described in the `searchspace/` folder to the Gephi graph format to be loaded and visualized in Gephi. By running

```Shell
./gradlew exportSearchSpacesToGephiFormat
```

you will find a folder `results/gephi-export`, containing three files: `slc-only.gephi`, `mlc-only.gephi`, and `mlc-complete.gephi`. These three files contain a Gephi specification of a directed acyclic graph (DAG) where each node corresponds to one algorithm in the search space and an edge a dependency relation from one to another algorithm indicating that the algorithm represented by the parent node can be configured with the child node as a base learner/kernel. With these graph visualizations one can easily see the exponential growth of the search space when combining the single-label classification algorithms with the multi-label classification algorithms.

#### Generate HTML Overview
Generate an HTML document summarizing the search space in terms of some statistics, listing all included algorithms together with their hyper-parameters (including their types, domains, and defaults) and recursive dependencies on other algorithms. The possible choices for each dependency are listed and linked within the document accordingly.

```Shell
./gradlew generateMultiLabelSearchSpaceDescription
```

This will generate a file `results/searchspace-meka.html`. If you want to generate the same type of description for the single-label classification (WEKA) search space use the following.

```Shell
./gradlew generateSingleLabelSearchSpaceDescription
```

This will generate a file `results/searchspace-weka.html`.

### Evaluation Results

The data obtained by running the benchmark across various datasets and folds can be found in `results/data`. From this data you can generate several statistics, summaries, and plots. How these can be generated is explained in the following.

#### Generate One-VS-Rest Scatter Plots

In the corresponding paper we compared the different optimizers in a one-vs-rest fashion plotting their performances against each other facilitating the analysis which approach performs preferably over the rest. The plots can be generated with the following command:

```Shell
./gradlew generateScatterPlots
```

The command will produce its output in the folder `results/scatter-plots`, where afterwards you will find for each optimizer and performance measure (instance-wise F-measure, label-wise F-measure, and micro F-measure) you will find a LaTeX file named like this: `scatter-<optimizer>VSrest-<measure>.tex`. Furthermore you can find a `main.tex` which will include all packages and scatter plots to compile them into a PDF document.

#### Generate Anytime Average Rank Plots

```Shell
./gradlew generateAnytimeAverageRankPlots
```

`results/anytime-plots/`

#### Generate Result Tables

```Shell
./gradlew generateResultTables
```

