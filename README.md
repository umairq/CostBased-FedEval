### An Empirical Evaluation of Cost-based Federated SPARQL Query Processing Engines


We present novel evaluation metrics targeted at a fine-grained benchmarking of cost-based federated SPARQL query engines. 
We evaluate the query planners of five different cost-based federated SPARQL query engines using LargeRDFBench queries

### Reproducing Results
Please follow the steps to reproduce our results. 
* First you need to setup LargeRDFBench. The complete details can be found from LargeRDFBench [home page](https://github.com/dice-group/largerdfbench)
* Download the runable jar files of the selected cost-based federation engines from [here](https://github.com/dice-group/CostBased-FedEval/tree/master/jars) except Odyssey, for Odyssey there are many dependencies involved and classes are run using scripts provided in scripts folder of project zip file. Detailed instructions to run the engine is provided at Odyssey [home page](https://github.com/gmontoya/federatedOptimizer), updated code with our metric is available [here](https://github.com/dice-group/CostBased-FedEval/tree/master/source%20code/Odyssey/federatedOptimizer). 
## For generating results from jars 
For generating results after above setups, next step is generate the summaries(not needed for engines using VoID descriptions, as it is already provided along with [source code](https://github.com/dice-group/CostBased-FedEval/tree/master/source%20code)) and then run the engine using the jar files, we provided. Running queries on engines will result in producing similarity files which contains information related to Acctual and Estimated cardinalities, and overall similarity values of query plan. You can run the jar files using CLI replacing argumnets with following commands:

**CostFed: Generating summaries:
```
java -jar costfed-summaries.jar [path-of-(summary.n3)-file] [path-of-endpoints-text-file-folder]
example:
java -jar costfed-summaries.jar /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/index/costfed/summaries/summary.n3 /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/endpoints


\\endpoints file should contain the URLs of all endpoints
```
**CostFed: Executing Queries and Generating plan similarity and cardinality values:
```
java -jar costfed-core.jar [path-of-(costfed.props)-file] [path-of-query-results-folder] [path-of-queries-folder] [path-of-endpoints-file-folder]  [path-of-similarity-results-folder]
example:
java -jar costfed-core.jar /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/index/costfed/costfed.props /home/MuhammadSaleem/umair/evaluation/experiments/query_results /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries /home/MuhammadSaleem/umair/evaluation/experiments/endpoints  /home/MuhammadSaleem/umair/evaluation/experiments/queries/results

\\example constfed.props file in source code folder.We should set Relative_Error variable to "true" in costfed.prop file. More details about properties and index files is mentioned on project [page](https://github.com/dice-group/CostFed).
```
**semaGrow: Generating summaries
```
java -jar semagrow-summary-1.4.1.jar [path-of-endpoints-file-folder] [path-of-SemaGrow-index-file]
example:
java -jar semagrow-summary-1.4.1.jar /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/index/semagrow/semagrow4.ttl

```
**SemaGrow: Executing Queries and Generating plan similarity and cardinality values
```
java -jar semagrow-core-1.4.1.jar [path-to-(results.csv)-file] [path-to-qeruries-file] [path-to-similatiy-error-folder] [path-to-(repository-index.ttl)-file] true 

example:
java -jar semagrow-core-1.4.1.jar /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/results/results.csv /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/queries /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/similarityResults /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/index/semagrow/repositoryindex.ttl true
```
**SPLENDID: Executing Queries and Generating plan similarity and cardinality values
```
\\SPLENDID uses VoID 

java -jar splendid-orignal.jar [path-to-file-(federation-test.properties)] [path-to-splendid-output-file] [path-to-queries-folder] [path-to-similarity-results-file] [true]

example:
java -jar splendid-orignal.jar /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/index/splendid/eval/federation-test.properties /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/res/splendid-output.txt /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/queries /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/similarityResults true


```
**LHD: Executing Queries and Generating plan similarity and cardinality values
```
\\LHD uses VoID 
java -jar LHD.jar [path-to-stats-file] [path-to-queries-file] [path-to-similarity-results-folder] [true]

example:
java -jar LHD.jar /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/index/lhd/stats /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/lhdqueries /home/MuhammadSaleem/umair/evaluation/experiments/LargeRDFBenchQueries/queries/results true

```

Notice that in arguments, if file is mentioned then we give path to exact file, if folder is mentioned then we give path to folder of respective file/files. 

Second thing which is important to notice is that, in other engines except LHD queries folder contains all queries in seperate files, while in LHD all queries are placed in single file. Sample is [here](queries/lhdqueries.txt).

***Odyssey:

For the case of oddyssey, first you need to extract [project](federatedOptimizer.rar), second step will be to compile the code in code folder, and then you need to run the script(executeQueriesOdyssey.sh) in scripts folder replacing some paths in the script file. For complete instruction you may refer to project readme [file](https://github.com/gmontoya/federatedOptimizer/blob/master/README.md) and [issue page](https://github.com/gmontoya/federatedOptimizer/issues/2), that we posted in order to run the engine successfully. 

### Generating Results from Source code
Source code is available [here](https://github.com/dice-group/CostBased-FedEval/tree/master/source%20code) , Import each engine as seperate project. It contains 5 -- CostFed, LHD, SemaGrow, splendid-test, Odyssey -- java projects. Each project could be compiled and run seperately. Main files are as following (arguments will be same as in jar files discussed before):

```
//Execute Queries on SemaGrow from 
package org.semagrow.semagrow.org.aksw.simba.start.semagrow
public class QueryEvaluation 

//Execute Queries on CostFed from 
package org.aksw.simba.start
public class QueryEvaluation 

//Execute Queries on LHD from 
package trunk
public class lhd 

//Execute Queries on SPLENDID from 
package de.uni_koblenz.west.evaluation
public class QueryProcessingEval

```
In order to run Odyssey instructions are same as discussed before.

### Loading results into Virtuoso and calculating similarity errors:
Similarity results that we have calculated in our experiments are available [here](https://github.com/dice-group/CostBased-FedEval/tree/master/results/similarityResults)

After generating similarity results, these results are loaded into Virtuoso server and then using SPARQL queries we can get the required output using similarity calculation formula we discussed in paper. Our complete evaluation Results are [here](https://docs.google.com/spreadsheets/d/1ue0pbR1cZNlmZcETo3pcgqme1vA5DVoNS_1KF8BCwfc/edit?usp=sharing). 

### Complete Evaluation Results
We have compared 5 - [CostFed](https://svn.aksw.org/papers/2018/SEMANTICS_CostFed/public.pdf), [SPLENDID](http://ceur-ws.org/Vol-782/GoerlitzAndStaab_COLD2011.pdf ), [LHD](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.362.6974&rep=rep1&type=pdf ), [Odyssey](https://iswc2017.semanticweb.org/wp-content/uploads/papers/MainProceedings/204.pdf ), [SemaGrow](https://www.researchgate.net/publication/281898683_SemaGrow_optimizing_federated_SPARQL_queries) - state-of-the-art SPARQL endpoint federation systems using LargeRDFBench on our proposed metric.
Our complete evaluation results can be found [here](https://github.com/dice-group/CostBased-FedEval/blob/master/results/CostBased-FedEval-Results%20(1)%20(1).xlsx)

### Canonical Citations

M. Saleem, A. Potocki, T. Soru, O. Hartig, and A.-C. Ngonga Ngomo.  Costfed:Cost-based query optimization for sparql endpoint federation.  06 2018.

G. Montoya, H. Skaf-Molli, and K. Hose.  The odyssey approach for optimizingfederated  sparql  queries.   In  C.  d’Amato,  M.  Fernandez,  V.  Tamma,  F.  Lecue,P. Cudr ́e-Mauroux, J. Sequeda, C. Lange, and J. Heflin, editors,The SemanticWeb – ISWC 2017, pages 471–489, Cham, 2017. Springer International Publishing.

A. Charalambidis, A. Troumpoukis, and S. Konstantopoulos. Semagrow: Optimizingfederated sparql queries.  InProceedings of the 11th International Conference onSemantic Systems, SEMANTICS ’15, pages 121–128, New York, NY, USA, 2015.ACM.

X. Wang, T. Tiropanis, and H. Davis. Lhd: Optimising linked data query processingusing parallelisation.CEUR Workshop Proceedings, 996, 05 2013.

O. G ̈orlitz and S.  Staab.   Splendid:  Sparql endpoint federation  exploiting voiddescriptions.  InProceedings of the Second International Conference on ConsumingLinked Data - Volume 782, COLD’11, pages 13–24, Aachen, Germany, Germany,2010. CEUR-WS.org.


### Future plan: 
We will add the resource results in to [RdfStoreBenchmarking](https://www.w3.org/wiki/RdfStoreBenchmarking) same like we did for our other published benchmarking results such as [DBpedia SPARQL benchmark](http://jens-lehmann.org/files/2011/dbpsb.pdf), [FEASIBL](http://svn.aksw.org/papers/2015/ISWC_FEASIBLE/public.pdf) and Federation evaluation.

### Authors
  * [Umair Qudus](https://dice-research.org/UmairQudus) (DKE, Kyung Hee University) 
  * [Muhammad Saleem](https://sites.google.com/site/saleemsweb/) (AKSW, University of Leipzig) 
  * [Axel-Cyrille Ngonga Ngomo](http://aksw.org/AxelNgonga.html) (AKSW, University of Leipzig)
  * [Young-Koo Lee](http://dke.khu.ac.kr/wordpress/professor) (DKE, Kyung Hee University) 
  

