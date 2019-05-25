package eu.semagrow.core.impl.error;

import eu.semagrow.core.estimator.CardinalityEstimator;
import eu.semagrow.core.impl.estimator.CostEstimatorImpl;
import eu.semagrow.core.impl.plan.PlanVisitorBase;
import eu.semagrow.core.impl.plan.ops.BindJoin;
import eu.semagrow.core.impl.plan.ops.HashJoin;
import eu.semagrow.core.impl.plan.ops.SourceQuery;
import eu.semagrow.core.impl.sparql.SPARQLSite;
import eu.semagrow.core.plan.Plan;
import eu.semagrow.core.source.Site;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.DistinctIteration;
import info.aduna.iteration.EmptyIteration;
import info.aduna.iteration.UnionIteration;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.impl.EmptyBindingSet;
import org.semagrow.semagrow.org.aksw.simba.start.semagrow.QueryEvaluation;
//import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CalculatePlanQuality extends PlanVisitorBase<RuntimeException> {
    public static Map<TupleExpr,Double> estimatedCard = new HashMap<TupleExpr, Double>();
    public static Map<Map<TupleExpr,Double>, Integer> plan_wise_estCard = new HashMap<Map<TupleExpr, Double>, Integer>();
    public static Map<TupleExpr,Double> realCard = new HashMap<TupleExpr, Double>();
    public static Map<Map<TupleExpr,Double>, Integer> plan_wise_realCard = new HashMap<Map<TupleExpr, Double>, Integer>();
    public static TupleExpr queryPlan = null;
    public static Map<String, TupleExpr> tpQueryMap = new HashMap<String, TupleExpr>();
    public static String recentQuery = "";
    public static TupleExpr temp = null;

    public static TupleExpr arg1 = null;
    public static TupleExpr arg2 = null;
//
    public static CardinalityEstimator cardinalityEstimator;
//	protected static final Logger log = Logger.getLogger(CalculatePlanQuality.class);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final boolean MULTI_THREADED = true;
    //

    public static void addRealCardinality(TupleQueryResult res){
//                ((TupleQueryResultImpl) res).next().

    }
    public static BindingSet addToRealCardinality(TupleExpr tupleExpr, Double card, BindingSet setStream ){
        if ((realCard.get(tupleExpr) != null)) {
            realCard.put(tupleExpr, realCard.get(tupleExpr) + card);
        } else {
            realCard.put(tupleExpr, 1.0);
        }
        estimatedCard.put(tupleExpr, (double)cardinalityEstimator.getCardinality(tupleExpr));
        return setStream;
    }
    public static void calculatePlanQuality(){
        for(TupleExpr expr:realCard.keySet())
            estimatedCard.put(expr,1.0);
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
    public static void addRealCard(TupleExpr tupleExpr, Double card){
        if(realCard.get(tupleExpr)==null){
            realCard.put(tupleExpr,card);
        }
    }

    @Override
    public void meet(Plan plan) throws RuntimeException {
        TupleExpr tupleExpr = plan.getArg();
        CalculatePlanQuality.arg1 = tupleExpr;

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
    public static void writeToFile(String q) throws Exception {
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
                TupleExpr leftArg = ((Plan)((BindJoin)tupleExpr).getLeftArg()).getArg();
                TupleExpr rightArg = ((Plan)((BindJoin)tupleExpr).getRightArg()).getArg();
                if(!(leftArg instanceof Join) && !(leftArg instanceof Union)) {
                   // leftArg.visit(leftArg);
                    double realTp = realCard_copy.get(leftArg)!=null?realCard_copy.get(leftArg):0.0;
                    double estTp = estimatedCard_copy.get(leftArg)!=null?estimatedCard_copy.get(leftArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(leftArg);
                    realCard_copy.remove(leftArg);
                }

                if(!(rightArg instanceof Join) && !(rightArg instanceof Union)) {
//                    double realTp = realCard_copy.get(rightArg)!=null?realCard_copy.get(rightArg):0.0;
//                    double estTp = estimatedCard_copy.get(rightArg)!=null?estimatedCard_copy.get(rightArg):0.0;
//                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
//                    tp++;
//                    estimatedCard_copy.remove(rightArg);
//                    realCard_copy.remove(rightArg);

                    if(rightArg instanceof SourceQuery) {
                        realCard_copy.put(rightArg, 0.0);
                        SourceQuery query = ((SourceQuery) rightArg);
                        CloseableIteration<BindingSet, QueryEvaluationException> cursor = sendSparqlQuery(query.getArg(), query.getSources(), new EmptyBindingSet());
                        Double total = 0.0;
                        while (cursor.hasNext()) {
                            String test = cursor.next().getValue("triples").stringValue();
                            total += Double.parseDouble(test);
                            System.out.println(": " + test);
                        }
                        realCard_copy.put(rightArg, total);
                        Double realTp = realCard_copy.get(rightArg) != null ? realCard_copy.get(rightArg) : 0.0;
                        Double estTp = estimatedCard_copy.get(rightArg) != null ? estimatedCard_copy.get(rightArg) : 0.0;
                        WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                        tp++;
                        estimatedCard_copy.remove(rightArg);
                        realCard_copy.remove(rightArg);
                    }
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
                TupleExpr leftArg = ((Plan)((BindJoin)tupleExpr).getLeftArg()).getArg();
                TupleExpr rightArg = ((Plan)((BindJoin)tupleExpr).getRightArg()).getArg();
                if(!(leftArg instanceof Join) && !(leftArg instanceof Union)) {
                    double realTp = realCard_copy.get(leftArg)!=null?realCard_copy.get(leftArg):0.0;
                    double estTp = estimatedCard_copy.get(leftArg)!=null?estimatedCard_copy.get(leftArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(leftArg);
                    realCard_copy.remove(leftArg);
                }

                if(!(rightArg instanceof Join) && !(rightArg instanceof Union)) {
                    double realTp = realCard_copy.get(rightArg)!=null?realCard_copy.get(rightArg):0.0;
                    double estTp = estimatedCard_copy.get(rightArg)!=null?estimatedCard_copy.get(rightArg):0.0;
                    WriteErrorData.writeJoinTPInfoToFile(estTp, realTp, tp);
                    tp++;
                    estimatedCard_copy.remove(rightArg);
                    realCard_copy.remove(rightArg);
                }
                WriteErrorData.join_number++;

            }
//            if(tupleExpr instanceof Union){
//                //writting the join info
//                double estJ = (estimatedCard_copy.get(tupleExpr)!=null)?estimatedCard_copy.get(tupleExpr):0.0;
//                double realJ = (realCard_copy.get(tupleExpr)!=null)?realCard_copy.get(tupleExpr):0.0;
//                WriteErrorData.writeUnionInfoToFile(estJ,realJ);
//                estimatedCard_copy.remove(tupleExpr);
//                realCard_copy.remove(tupleExpr);
//                int tp=1;
//                ///writing join arguments data
//                TupleExpr leftArg = ((Plan)((Union)tupleExpr).getLeftArg()).getArg();
//                TupleExpr rightArg = ((Plan)((Union)tupleExpr).getRightArg()).getArg();
//                if(!(leftArg instanceof Join) && !(leftArg instanceof Union)) {
//                    double realTp = realCard_copy.get(leftArg)!=null?realCard_copy.get(leftArg):0.0;
//                    double estTp = estimatedCard_copy.get(leftArg)!=null?estimatedCard_copy.get(leftArg):0.0;
//                    if(estTp !=0.0 && realTp != 0.0)WriteErrorData.writeUnionTPInfoToFile(estTp, realTp, tp);
//                    tp++;
//                    estimatedCard_copy.remove(leftArg);
//                    realCard_copy.remove(leftArg);
//                }
//
//                if(!(rightArg instanceof Join) && !(rightArg instanceof Union)) {
//                    double realTp = realCard_copy.get(rightArg)!=null?realCard_copy.get(rightArg):0.0;
//                    double estTp = estimatedCard_copy.get(rightArg)!=null?estimatedCard_copy.get(rightArg):0.0;
//                    if(estTp !=0.0 && realTp != 0.0)WriteErrorData.writeUnionTPInfoToFile(estTp, realTp, tp);
//                    tp++;
//                    estimatedCard_copy.remove(rightArg);
//                    realCard_copy.remove(rightArg);
//                }
//                WriteErrorData.union_number++;
//            }

        }


        int count =1;
        real_cards = new ArrayList<TupleExpr>(realCard_copy.keySet());
        //remaining triple patterns that are not under any join
        for(TupleExpr tupleExpr:real_cards){
            if(!(tupleExpr instanceof Union)) {

                double estJ = estimatedCard_copy.get(tupleExpr) != null ? estimatedCard_copy.get(tupleExpr) : 0.0;
                double realJ = realCard_copy.get(tupleExpr) != null ? realCard_copy.get(tupleExpr) : 0.0;
                WriteErrorData.writeIndvidualTPInfoToFile(estJ, realJ, count);
                estimatedCard_copy.remove(tupleExpr);
                realCard_copy.remove(tupleExpr);
                count++;
            }

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
    private static CloseableIteration<BindingSet, QueryEvaluationException> sendSparqlQuery(TupleExpr expr, List<Site> sources, BindingSet bindings) {

        // check if there are any sources to query
        if (sources.size() == 0) {
            CalculatePlanQuality.addRealCard((TupleExpr) expr.getParentNode(),0.0);
            return new EmptyIteration<BindingSet, QueryEvaluationException>();
        }

//		if (expr instanceof StatementPattern)
//			LOGGER.error("is statement pattern");

        // TODO: need to know actual projection and join variables to reduce transmitted data

        CloseableIteration<BindingSet, QueryEvaluationException> cursor;
        List<CloseableIteration<BindingSet, QueryEvaluationException>> cursors = new ArrayList<CloseableIteration<BindingSet, QueryEvaluationException>>(sources.size());
        final String query = "SELECT REDUCED (COUNT(*) AS ?triples) WHERE {" + SparqlPrinter.print(expr) + "}";

        CalculatePlanQuality.tpQueryMap.put(query, (TupleExpr) expr.getParentNode());
        CalculatePlanQuality.recentQuery = query;

//        if (LOGGER.isDebugEnabled())
//            LOGGER.debug("Sending SPARQL query to '" + sources + " with bindings " + bindings + "\n" + query);

        for (final Site rep : sources) {
                cursors.add(QueryExecutor.eval(((SPARQLSite) rep).getURI().stringValue(), query, bindings));
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
