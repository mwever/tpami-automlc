# AutoML for Multi-Label Classification: Overview and Empirical Evaluation

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

### Without a Singularity Container

If you are running Linux you also have the possibility to run the benchmark directly on your system, without creating any Singularity container.
To this end, we have prepared a `requirements.txt` file so that you can install the required dependencies for the Python environment with ease.
However, since we mainly work with Singularity containers to have a clearly distinct system running, preventing interferences with inappropriate python versions and compatibility conflicts in general, we do not officially support the setup variant without creating a Singularity container.

### Test your Setup

In order to test your setup we have prepared a test runner that will work out-of-the-box if everything has been setup correctly.
More precisely, you can test whether each of the optimizers can be run for a specific dataset split with short timeouts of 1 minute for the entire optimization run and 45 seconds for a single evaluation. 

## Visualizing Benchmark Data

Throughout the paper, several visualization of the results have been presented. A pre-processed version of the logged data is contained in the directory `results/data/`.
From this data, you can further process the data to derive the numbers presented in the paper. Plots that have been generated via LaTeX's `tikz` or `pgfplots`, you can even generate the corresponding LaTeX code in the following. 

### Inspecting the Search Space
The configuration files containing the specification of the search space are contained in the folder `searchspace/`. For this, the root file is `searchspace/meka-all.json` and (recursively) includes the remaining configuration files

#### List Algorithms Contained in the Search Space

If you only want to obtain a list of algorithms ordered by algorithm type that are contained in the search space including their abbreviation as given in the paper, you can use the following short cut:

```Shell
./gradlew exportAlgorithmsInSearchSpace
```

This will create a txt file in the `/results` directory with the name `searchspace-algorithms-in-space.txt` listing all the algorithm types together with the algorithms belonging to these types.

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

The bar charts comparing the two search spaces in the paper have been generated from the statistical values contained on the top of these HTML documents. However, since the plots have been generated using `matplotlibs` in Python, please refer to the Jupyter notebook `TPAMI Plots.ipynb` for deriving the bar charts from these statistics.
Therewith, you can obtain the comparison figure for the different search spaces.



### Evaluation Results

The data obtained by running the benchmark across various datasets and folds can be found in `results/data`. From this data you can generate several statistics, summaries, and plots. How these can be generated is explained in the following.

#### Generate One-VS-Rest Scatter Plots

In the paper we compared the different optimizers in a one-vs-rest fashion plotting their performances against each other facilitating the analysis which approach performs preferably over the rest. The plots can be generated with the following command:

```Shell
./gradlew compileScatterPlots
```

The command will produce its output in the folder `results/scatter-plots`, where afterwards you will find for each optimizer and performance measure (instance-wise F-measure, label-wise F-measure, and micro F-measure) you will find a LaTeX file named like this: `scatter-<optimizer>VSrest-<measure>.tex`. Furthermore you can find a `main.tex` which will include all packages and scatter plots to compile them into a PDF document.

#### Generate Anytime Average Rank Plots

Furthermore, we have presented anytime average rank plots comparing the different optimizers across the time dimension. You can compile the result data into these anytime plots by executing the following command:

```Shell
./gradlew compileAnytimeAverageRankPlots
```

This will generate several `.tex`-files in the directory `results/anytime-plots/`.
In fact, there are even two different types of plots: First following the naming schema `avgrank-<MEASURE>.tex` you will find the average rank plots as presented in the paper. However, for each combination of measure and dataset you can also find the actual average performance in terms of the respective performance measure of the different optimizers.
Since presenting those would have required lots of space, these plots have not been included in the paper but are made available here as a kind of supplementary material.

#### Generate Result Tables

For a comparison of the performance of eventually returned incumbents, we presented tables for each measure individually.
You can generate these tables yourself once again by running the following command:

```Shell
./gradlew compileResultTables
```

This will again generate `.tex`-files containing the LaTeX code to represent the corresponding table data. In addition to the average performances per dataset and optimizer, a Wilcoxon signed-rank test is conducted with a threshold for the p-value of 0.05. The best results, significant improvements and degradations are highlighted as described in the paper. Furthermore, in the last row of the tables, an average rank for each optimizer across all datasets is given.

#### Generate Incumbent Frequency Statistics

Another figure in the paper shows which algorithms have been chosen with what frequency by which optimizer. Since these plots have been generated with the help of matplotlibs, here, we will only compile the necessary statistics from the data, necessary to produce the figures.
In order to compile the statistics from the result data, run the following command:

```Shell
./gradlew compileIncumbentFrequencyStatistics
```

This will generate a txt file `results/incumbent-frequencies.txt` containing JSON arrays that can be copied and pasted into a Jupyter notebook which is also available via this repository. Please refer to the `TPAMI Plots.ipynb` notebook file for further processing of the compiled raw data into the nested donut charts.