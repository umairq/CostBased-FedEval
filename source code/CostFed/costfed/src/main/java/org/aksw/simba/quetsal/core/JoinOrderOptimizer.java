package org.aksw.simba.quetsal.core;

import java.io.IOException;
import java.util.*;

import com.fluidops.fedx.algebra.*;
import org.aksw.simba.quetsal.core.algebra.BindJoin;
import org.aksw.simba.quetsal.core.algebra.HashJoin;
import org.aksw.simba.quetsal.core.error.RelativeError;
import org.aksw.simba.quetsal.util.CountQuery;
import org.eclipse.rdf4j.query.algebra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.optimizer.OptimizerUtil;
import com.fluidops.fedx.optimizer.StatementGroupOptimizer;
import com.fluidops.fedx.structures.QueryInfo;

public class JoinOrderOptimizer extends StatementGroupOptimizer {
    public static Logger log = LoggerFactory.getLogger(JoinOrderOptimizer.class);

    protected static double C_HANDLE_TUPLE = 0.0025;
    protected static double C_TRANSFER_TUPLE = 0.01;
    protected static double C_TRANSFER_QUERY = 100;
    public static List<CardinalityVisitor.CardPair> estCardPairs2 = new ArrayList<CardinalityVisitor.CardPair>();
    public static List<CardinalityVisitor.CardPair> cardPairs3 = new ArrayList<CardinalityVisitor.CardPair>();
    public static List<CardinalityVisitor.CardPair> cardRealBind2 = new ArrayList<CardinalityVisitor.CardPair>();
    public static List<CardinalityVisitor.JoinCard> cardJoin = new ArrayList<CardinalityVisitor.JoinCard>();

    Map<QueryModelNode, CardinalityVisitor.NodeDescriptor> ds = new HashMap<QueryModelNode, CardinalityVisitor.NodeDescriptor>();

    public class EstimatorVisitor extends CardinalityVisitor {
        EstimatorVisitor() {
            super(JoinOrderOptimizer.this.queryInfo);
        }

        @Override
        public void meet(Union union) {
            union.getLeftArg().visit(this);
            NodeDescriptor temp = current;
            reset();
            union.getRightArg().visit(this);
            if (temp.card == Long.MAX_VALUE || current.card == Long.MAX_VALUE) {
                current.card = Long.MAX_VALUE;
            } else {
                current.card += temp.card;
            }
            current.sel = Math.min(current.sel, temp.sel);
        }

        public void meet(NJoin nj) {
            throw new Error("NJoins must be removed");
        }

        @Override
        protected void meetNode(QueryModelNode node) {
            if (node instanceof StatementPattern) {
                meet((StatementPattern) node);
            } else if (node instanceof Filter) {
                meet((Filter) node);
            } else if (node instanceof Union) {
                meet((Union) node);
            } else if (node instanceof ExclusiveGroup) {
                meet((ExclusiveGroup) node);
            } else if (node instanceof NJoin) {
                meet((NJoin) node);
            } else {
                super.meetNode(node);
            }
        }
    }


    public JoinOrderOptimizer(QueryInfo queryInfo) {
        super(queryInfo);
    }

    /*
	@Override
	public void checkExclusiveGroup(List<ExclusiveStatement> exclusiveGroupStatements) {
		CardinalityVisitor cvis = new CardinalityVisitor();
		List<CardPair> cardPairs = new ArrayList<CardPair>();
		List<ExclusiveStatement> copy = new ArrayList<ExclusiveStatement>(exclusiveGroupStatements);
		for (ExclusiveStatement es : copy) {
			es.visit(cvis);
			cardPairs.add(new CardPair(es, cvis.getCardinality()));
			cvis.reset();
			if ("http://www.w3.org/2002/07/owl#sameAs".equals(es.getPredicateVar().getValue().toString())) {
				exclusiveGroupStatements.remove(es);
			}
		}
		// sort arguments according their cards
		cardPairs.sort((cpl, cpr) -> cpl.card.compareTo(cpr.card));
		
		for (CardPair cp : cardPairs) {
			log.trace(cp);
		}
	}
	*/


    @Override
    public void optimizeJoinOrderEmptyNJoin(NJoin node, TupleExpr joinArgs) throws IOException {
    }

    @Override
    public void optimizeJoinOrderSingleTriple(NJoin node, TupleExpr joinArgs) throws IOException {
        EstimatorVisitor cvis = new EstimatorVisitor();
        List<CardinalityVisitor.CardPair> cardPairs = new ArrayList<CardinalityVisitor.CardPair>();

        joinArgs.visit(cvis);
        cardPairs.add(new CardinalityVisitor.CardPair(joinArgs, cvis.getDescriptor()));
        cvis.reset();
        for (int i = 0; i < cardPairs.size(); i++) {
            estCardPairs2.add(new CardinalityVisitor.CardPair(cardPairs.get(i).expr, cardPairs.get(i).nd));
            cardPairs3.add(new CardinalityVisitor.CardPair(cardPairs.get(i).expr, cardPairs.get(i).nd));
        }
//            long realCard = getTripleCardinalityExGroups(((ExclusiveGroup)joinArgs).getStatementSources().get(0).getEndpointID().toString(),joinArgs);
        CountQuery countQuery = new CountQuery(joinArgs);
        countQuery.evaluate();
        //handle join
        if (cardPairs3.size() >= 2) {
            CardinalityVisitor.CardPair leftArg = cardPairs3.get(0);
            //result.add(cardPairs.get(0).expr);
            cardPairs3.remove(0); // I expect it isn't too expensive, list is not very long (to do: try linked list)

            Set<String> joinVars = new HashSet<String>();
            joinVars.addAll(OptimizerUtil.getFreeVars(leftArg.expr));

            // look for best bound pattern
            while (!cardPairs3.isEmpty()) {
                int rightIndex = 0;
                Collection<String> commonvars = null;
                for (int i = 0, n = cardPairs3.size(); i < n; ++i) {
                    TupleExpr arg = cardPairs3.get(i).expr;
                    commonvars = CardinalityVisitor.getCommonVars(joinVars, arg);
                    if (commonvars == null || commonvars.isEmpty()) continue;
                    rightIndex = i;
                    break;
                }

                CardinalityVisitor.CardPair rightArg = cardPairs3.get(rightIndex);
                cardPairs3.remove(rightIndex);
                joinVars.addAll(OptimizerUtil.getFreeVars(rightArg.expr));

                if (log.isTraceEnabled()) {
                    log.trace("", rightArg);
                }

                CardinalityVisitor.NodeDescriptor rd = CardinalityVisitor.getJoinCardinality(commonvars, leftArg, rightArg);
                cardJoin.add(new CardinalityVisitor.JoinCard(leftArg.expr, rightArg.expr, rd));

            }
        }


        int size = JoinOrderOptimizer.cardRealBind2.size();
        String errText = "";
        if (!joinArgs.toString().contains("BindJoin") && !joinArgs.toString().contains("HashJoin")) {
            RelativeError.relativeErrorCardPairTP(cardPairs.get(0), (int) JoinOrderOptimizer.cardRealBind2.get(0).nd.card);
            cardRealBind2.remove(0);
            removeItem(joinArgs);
        }
//             writeErrorString(errText);
    }

    @Override
    public void optimizeJoinOrder(NJoin node, List<TupleExpr> joinArgs) {
        EstimatorVisitor cvis = new EstimatorVisitor();
        List<CardinalityVisitor.CardPair> cardPairs = new ArrayList<CardinalityVisitor.CardPair>();

        // pin selectors
        boolean useHashJoin = false;
        boolean useBindJoin = false;

        // find card for arguments
        for (TupleExpr te : joinArgs) {
            te.visit(cvis);
            cardPairs.add(new CardinalityVisitor.CardPair(te, cvis.getDescriptor()));
            cvis.reset();
        }

        // sort arguments according their cards
        cardPairs.sort(new Comparator<CardinalityVisitor.CardPair>() {
            @Override
            public int compare(CardinalityVisitor.CardPair cpl, CardinalityVisitor.CardPair cpr) {
                return Long.compare(cpl.nd.card, cpr.nd.card);
            }
        });
//		cardJoin.add(new CardinalityVisitor.JoinCard())
        for (int i = 0; i < cardPairs.size(); i++) {
            estCardPairs2.add(new CardinalityVisitor.CardPair(cardPairs.get(i).expr, cardPairs.get(i).nd));
        }
        if (log.isTraceEnabled()) {
            log.trace("", cardPairs.get(0));
        }
        //long minCard = cardPairs.get(0).nd.card;
        //long maxCard = cardPairs.get(cardPairs.size() - 1).nd.card;

        CardinalityVisitor.CardPair leftArg = cardPairs.get(0);
        //result.add(cardPairs.get(0).expr);
        cardPairs.remove(0); // I expect it isn't too expensive, list is not very long (to do: try linked list)

        Set<String> joinVars = new HashSet<String>();
        joinVars.addAll(OptimizerUtil.getFreeVars(leftArg.expr));

        // look for best bound pattern
        while (!cardPairs.isEmpty()) {
            int rightIndex = 0;
            Collection<String> commonvars = null;
            for (int i = 0, n = cardPairs.size(); i < n; ++i) {
                TupleExpr arg = cardPairs.get(i).expr;
                commonvars = CardinalityVisitor.getCommonVars(joinVars, arg);
                if (commonvars == null || commonvars.isEmpty()) continue;
                rightIndex = i;
                break;
            }

            CardinalityVisitor.CardPair rightArg = cardPairs.get(rightIndex);
            cardPairs.remove(rightIndex);
            joinVars.addAll(OptimizerUtil.getFreeVars(rightArg.expr));

            if (log.isTraceEnabled()) {
                log.trace("", rightArg);
            }

            CardinalityVisitor.NodeDescriptor rd = CardinalityVisitor.getJoinCardinality(commonvars, leftArg, rightArg);
            cardJoin.add(new CardinalityVisitor.JoinCard(leftArg.expr, rightArg.expr, rd));

            long threads = queryInfo.getFederation().getConfig().getWorkerThreads();

            double hashCost = rightArg.nd.card * C_TRANSFER_TUPLE + (2 + threads - 1) / threads * C_TRANSFER_QUERY + (leftArg.nd.card + rightArg.nd.card) * C_HANDLE_TUPLE;

            long bsize = queryInfo.getFederation().getConfig().getBoundJoinBlockSize();

            long numOfBindRequest = (leftArg.nd.card + bsize - 1) / bsize;
            double bindCost = C_TRANSFER_QUERY + (numOfBindRequest + threads - 1) / threads * C_TRANSFER_QUERY + (leftArg.nd.card /*+ rd.card */) * C_TRANSFER_TUPLE;
            //bindCost = numOfBindRequest * C_TRANSFER_QUERY + rd.card * C_TRANSFER_TUPLE;
            if (log.isTraceEnabled()) {
                log.debug(String.format("join card: %s, hash cost: %s, bind cost: %s", rd.card, hashCost, bindCost));
            }

            NJoin newNode;
            //newNode = new HashJoin(leftArg.expr, rightArg.expr, queryInfo);
            //newNode = new BindJoin(leftArg.expr, rightArg.expr, queryInfo);
            ///*
            if (useHashJoin || (!useBindJoin && hashCost < bindCost && leftArg.nd.card < 1000000 && rightArg.nd.card < 1000000)) {
                newNode = new HashJoin(leftArg.expr, rightArg.expr, queryInfo);
                //useHashJoin = true; // pin
            } else {
                newNode = new BindJoin(leftArg.expr, rightArg.expr, queryInfo);
                if (queryInfo.getFederation().getConfig().isRelativeError()) {
                    CountQuery countQuery = new CountQuery(rightArg.expr, leftArg.expr);
                    countQuery.evaluate();
                    ///// Finding relative Error
                    int size = JoinOrderOptimizer.cardRealBind2.size();
                    String errText = "";
                    int idx = 1;
                    if (!leftArg.expr.toString().contains("BindJoin") && !leftArg.expr.toString().contains("HashJoin")) {
                        RelativeError.relativeErrorCardPairTP(leftArg, (int) JoinOrderOptimizer.cardRealBind2.get(size - idx).nd.card);
                        removeItem(leftArg.expr);
                        idx++;
                    }
                    if (!rightArg.expr.toString().contains("BindJoin") && !rightArg.expr.toString().contains("HashJoin")) {
                        RelativeError.relativeErrorCardPairTP(rightArg, (int) JoinOrderOptimizer.cardRealBind2.get(size - idx).nd.card);
                        removeItem(rightArg.expr);
                        idx++;
                    }
                }

                // sort arguments according their cards
                cardRealBind2.sort(new Comparator<CardinalityVisitor.CardPair>() {
                    @Override
                    public int compare(CardinalityVisitor.CardPair cpl, CardinalityVisitor.CardPair cpr) {
                        return Long.compare(cpl.nd.card, cpr.nd.card);
                    }
                });
                //useBindJoin = true; // pin
            }
            //*/
            leftArg.expr = newNode;
            leftArg.nd = rd;
        }
        node.replaceWith(leftArg.expr);
    }



    private void removeItem(TupleExpr idxremove) {
        int indxremove = 0;
        for (int i = 0; i < JoinOrderOptimizer.estCardPairs2.size(); i++) {
            if (JoinOrderOptimizer.estCardPairs2.get(i).expr.toString().contains(idxremove.toString())) {
                indxremove = i;
            }

        }
//        JoinOrderOptimizer.cardRealBind2.remove(indxremove);
        JoinOrderOptimizer.estCardPairs2.remove(indxremove);
    }

}
//                                errText += "\n Branch:" + rightArg.expr.toString() + "\n" + "Real Cardinality: " + JoinOrderOptimizer.cardRealBind2.get(size - idx).nd.card
//                                        + "\nEstimated Cardinality: " +
//                                        rightArg.nd.card + "\nRelative Error: " +
//                                        RelativeError.getRelativeError(rightArg.nd.card, JoinOrderOptimizer.cardRealBind2.get(size - idx).nd.card)+
//                                        "\n-------------------\n";
//
//                                SimilarityConstants.realValues.add((int) JoinOrderOptimizer.cardRealBind2.get(size-idx).nd.card);
//                                SimilarityConstants.estimatedValues.add(rightArg.nd.card);



//            if(expression instanceof ExclusiveGroup) {
//                Long t1 = getTripleCardinalityExGroups(((ExclusiveGroup) expression).getStatementSources().get(0).getEndpointID().toString(),
//                        expression);
//                CardinalityVisitor.NodeDescriptor nodeDescriptor = new CardinalityVisitor.NodeDescriptor();
//                nodeDescriptor.card = t1;
//                CardinalityVisitor.CardPair cardPair = new CardinalityVisitor.CardPair((TupleExpr) expression, nodeDescriptor);
//                cardRealBind2.add(cardPair);
//            }else if(expression instanceof StatementSourcePattern){
//                Long t1 = getTripleCardinalitySources(((StatementSourcePattern) expression).getStatementSources().get(0).getEndpointID().toString(),
//                        expression);
//                CardinalityVisitor.NodeDescriptor nodeDescriptor = new CardinalityVisitor.NodeDescriptor();
//                nodeDescriptor.card = t1;
//                CardinalityVisitor.CardPair cardPair = new CardinalityVisitor.CardPair((TupleExpr) expression, nodeDescriptor);
//                cardRealBind2.add(cardPair);
//            }else if(expression instanceof ExclusiveStatement){
//                Long t1 = getTripleCardinalityExStatement(((ExclusiveStatement) expression).getStatementSources().get(0).getEndpointID().toString(),
//                        expression);
//                CardinalityVisitor.NodeDescriptor nodeDescriptor = new CardinalityVisitor.NodeDescriptor();
//                nodeDescriptor.card = t1;
//                CardinalityVisitor.CardPair cardPair = new CardinalityVisitor.CardPair((TupleExpr) expression, nodeDescriptor);
//                cardRealBind2.add(cardPair);
//            }
//            if (expression2!=null && expression2 instanceof ExclusiveGroup) {
//                Long t2 = getTripleCardinalityExGroups(((ExclusiveGroup) expression2).getStatementSources().get(0).getEndpointID().toString(),
//                        expression2);
//                CardinalityVisitor.NodeDescriptor nodeDescriptor2 = new CardinalityVisitor.NodeDescriptor();
//                nodeDescriptor2.card = t2;
//                CardinalityVisitor.CardPair cardPair2 = new CardinalityVisitor.CardPair((TupleExpr) expression2, nodeDescriptor2);
//                cardRealBind2.add(cardPair2);
//            }else if(expression2!=null && expression2 instanceof StatementSourcePattern){
//                Long t2 = getTripleCardinalitySources(((StatementSourcePattern) expression2).getStatementSources().get(0).getEndpointID().toString(),
//                        expression2);
//                CardinalityVisitor.NodeDescriptor nodeDescriptor2 = new CardinalityVisitor.NodeDescriptor();
//                nodeDescriptor2.card = t2;
//                CardinalityVisitor.CardPair cardPair2 = new CardinalityVisitor.CardPair((TupleExpr) expression2, nodeDescriptor2);
//                cardRealBind2.add(cardPair2);
//            } if(expression2!=null && expression2 instanceof ExclusiveStatement){
//                Long t1 = getTripleCardinalityExStatement(((ExclusiveStatement) expression2).getStatementSources().get(0).getEndpointID().toString(),
//                        expression2);
//                CardinalityVisitor.NodeDescriptor nodeDescriptor = new CardinalityVisitor.NodeDescriptor();
//                nodeDescriptor.card = t1;
//                CardinalityVisitor.CardPair cardPair = new CardinalityVisitor.CardPair((TupleExpr) expression2, nodeDescriptor);
//                cardRealBind2.add(cardPair);
//            }