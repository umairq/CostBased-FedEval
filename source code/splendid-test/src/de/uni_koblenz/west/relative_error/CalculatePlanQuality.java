package de.uni_koblenz.west.relative_error;

import de.uni_koblenz.west.splendid.evaluation.AsyncCursor;
import de.uni_koblenz.west.splendid.evaluation.FederationEvalStrategy;
import de.uni_koblenz.west.splendid.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.splendid.helpers.QueryExecutor;
import de.uni_koblenz.west.splendid.helpers.SparqlPrinter;
import de.uni_koblenz.west.splendid.index.Graph;
import de.uni_koblenz.west.splendid.model.BindJoin;
import de.uni_koblenz.west.splendid.model.HashJoin;
import de.uni_koblenz.west.splendid.model.RemoteQuery;
import de.uni_koblenz.west.splendid.test.config.Configuration;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.DistinctIteration;
import info.aduna.iteration.EmptyIteration;
import info.aduna.iteration.UnionIteration;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.In;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.impl.TupleQueryResultImpl;
import org.openrdf.sail.helpers.SailBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CalculatePlanQuality {
    public static Map<TupleExpr,Double> estimatedCard = new HashMap<TupleExpr, Double>();
    public static Map<Map<TupleExpr,Double>, Integer> plan_wise_estCard = new HashMap<Map<TupleExpr, Double>, Integer>();
    public static Map<TupleExpr,Double> realCard = new HashMap<TupleExpr, Double>();
    public static Map<Map<TupleExpr,Double>, Integer> plan_wise_realCard = new HashMap<Map<TupleExpr, Double>, Integer>();
    public static TupleExpr queryPlan = null;
    public static Map<String, TupleExpr> tpQueryMap = new HashMap<String, TupleExpr>();
    public static String recentQuery = "";
    public static TupleExpr temp = null;
//
    private static final Logger LOGGER = LoggerFactory.getLogger(FederationEvalStrategy.class);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final boolean MULTI_THREADED = true;
    //
    public static void addRealCardinality(TupleQueryResult res){
//                ((TupleQueryResultImpl) res).next().

    }

    /**
     * Adding real cardinality along with associated tuple expression
     * @param tupleExpr
     */
    public static void addRealCard(TupleExpr tupleExpr){
        if(realCard.get(tupleExpr)==null){
            realCard.put(tupleExpr,1.0);
        }else{
            realCard.put(tupleExpr,realCard.get(tupleExpr)+1);
        }

    }

    /**
     * initial join call for adding join values
     * @param tupleExpr
     */
    public static void addRealJoinText(TupleExpr tupleExpr){
        if(realCard.get(tupleExpr)==null){
            realCard.put(tupleExpr,0.0);
        }
    }


    public static void addRealCardHashJ(){
        if(realCard.get(temp)!=null){
            realCard.put(temp,realCard.get(temp)+1.0);
        }
    }
    /**
     * Adding fixed real cardinality along with associated tuple expression
     * @param tupleExpr
     */
    public static void addRealCard(TupleExpr tupleExpr,Double card){
        if(realCard.get(tupleExpr)==null){
            realCard.put(tupleExpr,card);
        }
    }

    /**
     * saving the query plan
     * @param qp
     */
    public static void saveQueryPlan(TupleExpr qp){
        queryPlan = qp;
    }

    /**
     * writing the join and tp cardinalities to the file
     */
    public static void writeToFile(String q) throws QueryEvaluationException {
        saveDataForSimilarity();
        WriteErrorData.writeQueryPlan(q);
        WriteErrorData.join_number =1;
        Map<TupleExpr,Double> realCard_copy = new HashMap<TupleExpr, Double>(realCard);
        Map<TupleExpr,Double> estimatedCard_copy = new HashMap<TupleExpr, Double>(estimatedCard);

        ArrayList<TupleExpr> real_cards = new ArrayList<TupleExpr>(realCard_copy.keySet());
        for(TupleExpr tupleExpr: real_cards){
            //checking the joins
            if(tupleExpr instanceof BindJoin){
                //writting the join info
                double estJ = (estimatedCard_copy.get(tupleExpr)!=null)?estimatedCard_copy.get(tupleExpr):0.0;
                double realJ = (realCard_copy.get(tupleExpr)!=null)?realCard_copy.get(tupleExpr):0.0;
                WriteErrorData.writeJoinInfoToFile(estJ,realJ);
                estimatedCard_copy.remove(tupleExpr);
                realCard_copy.remove(tupleExpr);
                int tp=1;
                ///writing join arguments data
                TupleExpr leftArg = ((BindJoin)tupleExpr).getLeftArg();
                TupleExpr rightArg = ((BindJoin)tupleExpr).getRightArg();
                if(!(leftArg instanceof Join)) {
                    double realTp = realCard_copy.get(leftArg)!=null?realCard_copy.get(leftArg):0.0;
                    double estTp = estimatedCard_copy.get(leftArg)!=null?estimatedCard_copy.get(leftArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(leftArg);
                    realCard_copy.remove(leftArg);
                }

                if(!(rightArg instanceof Join)) {
                    realCard_copy.put(rightArg,0.0);
                    RemoteQuery query = ((RemoteQuery)rightArg);
                    CloseableIteration<BindingSet, QueryEvaluationException> cursor= sendSparqlQuery(query.getArg(),query.getSources(),new EmptyBindingSet());
                    Double total =0.0;
                    while (cursor.hasNext()){
                        String test = cursor.next().getValue("triples").stringValue();
                        total += Double.parseDouble(test);
                        System.out.println( ": " + test);
                    }
                    realCard_copy.put(rightArg,total);
                    Double realTp = realCard_copy.get(rightArg)!=null?realCard_copy.get(rightArg):0.0;
                    Double estTp = estimatedCard_copy.get(rightArg)!=null?estimatedCard_copy.get(rightArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(rightArg);
                    realCard_copy.remove(rightArg);
                }
                WriteErrorData.join_number++;

            }
            if(tupleExpr instanceof HashJoin){
                //writting the join info
                double estJ = (estimatedCard_copy.get(tupleExpr)!=null)?estimatedCard_copy.get(tupleExpr):0.0;
                double realJ = (realCard_copy.get(tupleExpr)!=null)?realCard_copy.get(tupleExpr):0.0;
                WriteErrorData.writeJoinInfoToFile(estJ,realJ);
                estimatedCard_copy.remove(tupleExpr);
                realCard_copy.remove(tupleExpr);
                int tp=1;
                ///writing join arguments data
                TupleExpr leftArg = ((HashJoin)tupleExpr).getLeftArg();
                TupleExpr rightArg = ((HashJoin)tupleExpr).getRightArg();
                if(!(leftArg instanceof Join)) {
                    double realTp = realCard_copy.get(leftArg)!=null?realCard_copy.get(leftArg):0.0;
                    double estTp = estimatedCard_copy.get(leftArg)!=null?estimatedCard_copy.get(leftArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(leftArg);
                    realCard_copy.remove(leftArg);
                }

                if(!(rightArg instanceof Join)) {
                    double realTp = realCard_copy.get(rightArg)!=null?realCard_copy.get(rightArg):0.0;
                    double estTp = estimatedCard_copy.get(rightArg)!=null?estimatedCard_copy.get(rightArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(rightArg);
                    realCard_copy.remove(rightArg);
                }
                WriteErrorData.join_number++;

            }

        }
        int count =1;
        real_cards = new ArrayList<TupleExpr>(realCard_copy.keySet());
        //remaining triple patterns that are not under any join
        for(TupleExpr tupleExpr:real_cards){
            double estJ = estimatedCard_copy.get(tupleExpr)!=null?estimatedCard_copy.get(tupleExpr):0.0;
            double realJ = realCard_copy.get(tupleExpr)!=null?realCard_copy.get(tupleExpr):0.0;
            WriteErrorData.writeIndvidualTPInfoToFile(estJ,realJ,count);
            estimatedCard_copy.remove(tupleExpr);
            realCard_copy.remove(tupleExpr);
            count++;

        }
        estimatedCard_copy.clear();
        realCard_copy.clear();
        WriteErrorData.calculateSimilarity();
        estimatedCard.clear();
        realCard.clear();

    }

    public static void saveDataForSimilarity(){
        for(TupleExpr tupleExpr: realCard.keySet()){
            if(estimatedCard.get(tupleExpr)!=null){
                SimilarityConstants.estimatedValues.add(estimatedCard.get(tupleExpr));
                SimilarityConstants.realValues.add(realCard.get(tupleExpr));
            }
        }
    }


    /**
     *
     * This function is for recalculating the real cardinality because bind join does not
     * give correct cardinality of individual triple pattern
     * @param expr
     * @param sources
     * @param bindings
     * @return
     */
    private static CloseableIteration<BindingSet, QueryEvaluationException> sendSparqlQuery(TupleExpr expr, Set<Graph> sources, BindingSet bindings) {

        // check if there are any sources to query
        if (sources.size() == 0) {
            if(Configuration.relative_error) {
                CalculatePlanQuality.addRealCard((TupleExpr) expr.getParentNode(),0.0);
            }
            LOGGER.warn("Cannot find any source for: " + OperatorTreePrinter.print(expr));
            return new EmptyIteration<BindingSet, QueryEvaluationException>();
        }

//		if (expr instanceof StatementPattern)
//			LOGGER.error("is statement pattern");

        // TODO: need to know actual projection and join variables to reduce transmitted data

        CloseableIteration<BindingSet, QueryEvaluationException> cursor;
        List<CloseableIteration<BindingSet, QueryEvaluationException>> cursors = new ArrayList<CloseableIteration<BindingSet, QueryEvaluationException>>(sources.size());
        final String query = "SELECT REDUCED (COUNT(*) AS ?triples) WHERE {" + SparqlPrinter.print(expr) + "}";
        if(Configuration.relative_error) {
            CalculatePlanQuality.tpQueryMap.put(query, (TupleExpr) expr.getParentNode());
            CalculatePlanQuality.recentQuery = query;
        }
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Sending SPARQL query to '" + sources + " with bindings " + bindings + "\n" + query);

        for (final Graph rep : sources) {
                cursors.add(QueryExecutor.eval(rep.toString(), query, bindings));
        }


        // create union if multiple sources are involved
        if (cursors.size() > 1) {
//			cursor = new UnionCursor<BindingSet>(cursors);
            cursor = new UnionIteration<BindingSet, QueryEvaluationException>(cursors);
        } else {
            cursor = cursors.get(0);
        }

        // Filter any duplicates
//		cursor = new DistinctCursor<BindingSet>(cursor);
        // TODO: check if this is bad for performance
        cursor = new DistinctIteration<BindingSet, QueryEvaluationException>(cursor);

        return cursor;

    }



}
