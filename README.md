# AutoML for Multi-Label Classification: Overview and Empirical Evaluation

This project provides a platform for benchmarking different optimizers for the task of automated machine learning ensuring all optimizers to work on the same set of potential solution candidates. The implementation is based on the Java open-source library [AILibs](https://github.com/starlibs/AILibs), providing the basic technical support for describing search spaces, HTN planning and heuristic search algorithms, as well as the infrastructure for synchronizing the execution of a benchmarking suite on a distributed system.

The benchmark distinguishes itself from previously published benchmark in the way how the optimizers are integrated with the benchmarking system. While all optimizers share the same routine for executing candidate solutions, the benchmark works in a cross-platform fashion, i.e. although the benchmark and the execution of candidate solutions is implemented in Java, optimizers available in Python can be benchmarked within the system. The search space and recursive structures of the search space are automatically translated into a format understandable to the respective optimizers. The inter-platform communication is done via the [Google ProtoBuf](https://developers.google.com/protocol-buffers) library which offers interfaces for various platforms. Thereby, the communication link only transfers the execution request to the benchmarking system allowing to share the same evaluation routine for all the optimizers. Another advantage is that it also allows for live-tracking the performance of the optimizers, logging each evaluated candidate and monitoring the current incumbent at any point in time.


## Quickstart - Setup

Prerequisites: Due to certain dependencies requiring a Linux operating system, the execution of optimizers in Python is not supported for Windows nor MacOS. However, optimizers available in Java can still be executed on Windows or MacOS. Running a Linux OS, you may execute Python optimizers as well. To this end, please use the SingularityRecipe to build a container in order to ensure all the dependencies necessary for running the benchmark are available.

### Preparing the Singularity Container

In order to set up a Singularity Container, please ensure that you have administrator permissions and installed the [Singularity Container](https://sylabs.io/guides/3.6/user-guide/) software on your computer. (Hint: On Ubuntu you can simply install it via `sudo apt install singularity-container`). Once you have Singularity Container installed on your system, follow these steps in order to create the container:

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

You can test to run each optimizer individually via the following commands:

```Shell
./gradlew testHTNBF
./gradlew testBOHB
./gradlew testHB
./gradlew testSMAC
./gradlew testRandomSearch
./gradlew testGGP
```

As a shortcut you can also simply test all the optimizers as follows:

```Shell
./gradlew testAllOptimizers
```

## Using the Benchmark

The benchmark implemented in this repository is meant to be run in a distributed way.

### Hardware Requirements
For running a benchmark suite you need the following resources:
A central database server managing the experiments to be executed and worker clients meeting the hardware requirements.
In the paper we used worker clients each equipped with `8 CPU cores` and `32GB RAM`.
The recommended hardware specifications for the database server depends on the degree of parallelization and how you set the parameters for the experiments.
The latter point is for instance depending what evaluation timeouts you choose for assessing the performance of single candidates.
The smaller the timeout the more intermediate evaluation results will be logged in the database.
As a consequence there will be a higher load on the respective database server.
In our study and with the timeout configuration proposed in the paper, we found that a configuration of `4 CPU cores` and `16GB RAM` is sufficient to deal with up to 200 worker clients.
For the database, we tested only a MySQL database. In principle other drivers are usable, but may require the inclusion of additional dependencies for the project.
Officially, we only support MySQL databases.

### Initialize the Database Server

In order to initialize the database server, first of all, you need to fill in the connection details in the `automlc-setup.properties` file.

```properties
...
db.driver = mysql
db.host = <YOUR DB HOST>
db.username = <YOUR DB USER>
db.password = <YOUR DB PASSWORD>
db.database = <YOUR DATABASE NAME>
db.table = <YOUR JOBS TABLE NAME>
db.ssl = <FLAG WHETHER TO USE SSL>
candidate_eval_table = <YOUR INTERMEDIATE EVALUATIONS TABLE NAME>
...
``` 

The specifics of the benchmark are then given with the following properties in the same files:

```properties
mem.max = 32768 // maximum available memory
cpu.max = 8 // number of cores

... // database connection properties and AILibs experimenter specific properties 


algorithm = bf,random,hb,bohb,smac,ggp // optimizers to consider
dataset = arts1,bibtex,birds,bookmarks,business1,computers1,education1,emotions,enron-f,entertainment1,flags,genbase,health1,llog-f,mediamill,medical,recreation1,reference1,scene,science1,social1,society1,tmc2007,yeast // datasets to consider
measure = FMacroAvgD,FMacroAvgL,FMicroAvg // performance measures to consider
split = 0,1,2,3,4,5,6,7,8,9 // split indices to consider
seed = 42 // seed of the dataset splitter to consider
globalTimeout=86400 // timeout for an entire optimization run
evaluationTimeout=1800 // timeout for a single candidate evaluation

... // constant properties for the experiment runner
```

Based on this specification, the benchmark will compute all possible combinations of entires given via the fields `algorithm`, `dataset`, `measure`, `split`, `seed`, `globalTimeout`, `evaluationTimeout`.
In principle it is also possible to configure multiple seeds, globalTimeouts or evaluationTimeouts in the same style as it is done e.g. for the algorithm field, i.e. simply by seperating multiple values by a comma.

Once the database connection is configured and the property values for all the benchmark suite specific parameters have been set, you can proceed by initializing the database server centrally managing the experiment conduction with the following command:

```Shell
./gradlew initializeExperimentsInDatabase
``` 

This will automatically create a table with the name specified in the `db.table` property which will then specify all the experiments to be executed which have been computed via taking the cross-product of all possible combinations of the properties describing the benchmark suite.

For cleaning this table, i.e., removing all of its entries, you can run the following command:

```Shell
./gradlew cleanExperimentsInDatabase
``` 

**Caution:** This functionality will also remove all results that are stored in this table after running an experiment.

### Preparing Dataset Splits

The original datasets from which the train and test splits have been derived are provided via this repository as well.
The datasets are located in the `original_datasets/` directory.
In order to derive the train and test dataset splits via a 10-fold cross-validation, run the following command:

```Shell
./gradlew generateDatasetSplits
```
This will create a directory `datasets/`, where the generated train and test splits are stored in seperated files.
As this procedure is done for all datasets contained in the `original_datasets/` directory and seed combinations, this probably takes some time and disk space.
The generated files follow the name schema `<DATASET>_<SEED>_<SPLIT INDEX>_{train|test}.arff`. As the worker client relies on this naming, the schema must not be changed.
Unfortunately, we cannot provide the dataset splits used in our study directly, as this would dramatically increase the size of the repository and lead to unreasonable download times for cloning.
However, on request we can of course provide the original dataset splits.

**Note**: We assume that all worker clients either have all the dataset splits locally available or share a network hard drive, centrally providing access to the respective dataset splits. The dataset folder can be configured in the `automlc-setup.properties` file via the property `datasetFolder`. 

### Running a Worker Client

Once everything is set up correctly, you may run a worker client via the command

```Shell
./gradlew runExperimentEvaluationWorker
```

The execution of the worker will also rely on the database connection configured in the `automlc-setup.properties`.
However, there is nothing specific you need to configure when deploying the worker client in a distributed way.
In fact, you can simply run the same command multiple times (on different nodes) in order to parallelize the processing of the benchmark.

The central database server will take care that each of the specified experiments will be executed only once at maximum, i.e. it will prevent the same experiment from being conducted twice. 
Since one worker client will also only take care of running a single experiment, you need to deploy as many worker clients as there are rows in the jobs table (named as you configured it in the properties file). In addition to the final results, the worker clients will also store intermediate evaluation results, i.e., candidates that have been requested for evaluation by the respective optimizer together with the measured performance value.

### Post-Processing for Anytime Test Performances

Since assessing the test performances on-line, i.e. during an optimizer run, would distort the overall perception of the optimization performance, all the test performances for later on compiling anytime plots etc. have been estimated via a post-processing step based on the log of intermediate candidate evaluations.
The post-processing is also implemented in a distributed way, analoguous to the already explained setup for running the actual benchmark of optimizers. In contrast to the latter, for the post-processing the `test-eval.properties` serves as a configuration.
Obviously, the jobs table for the post-processing needs a different name than the one specified for the benchmark itself. Furthermore, it will also use the `automlc-setup.properties` file for accessing the logged data and filtering the evaluated candidates.
Additionally, the post-processing requires access to the datasets directory.

As before you can configure the corresponding properties, for which optimizers,datasets, measures, etc. you want to run the post-processing. Furthermore you can setup the jobs table for distributing the workload on a cluster etc. as already described before for the benchmark via the following commands:

```Shell
./gradlew initializePostProcessingsInDatabase
./gradlew cleanPostProcessingsInDatabase
./gradlew runPostProcessingWorker
```

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

[Gephi](https://gephi.org/) is a graph modeling and visualization tool. You can export the search space as described in the `searchspace/` folder to the Gephi graph format to be loaded and visualized in Gephi. By running

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
In fact, there are even two different types of plots: First following the naming schema `avgrank-<MEASURE>.tex` you will find the average rank plots as presented in the paper.
However, for each combination of measure and dataset you can also find the actual average anytime performance of the different optimizers contained in the files named after the schema `<MEASURE>_<DATASET>.tex`.
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