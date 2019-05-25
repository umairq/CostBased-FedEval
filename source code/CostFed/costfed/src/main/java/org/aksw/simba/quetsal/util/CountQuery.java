package org.aksw.simba.quetsal.util;

import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.ExclusiveStatement;
import com.fluidops.fedx.algebra.StatementSource;
import com.fluidops.fedx.algebra.StatementSourcePattern;
import org.aksw.simba.quetsal.core.Cardinality;
import org.aksw.simba.quetsal.core.CardinalityVisitor;
import org.aksw.simba.quetsal.core.JoinOrderOptimizer;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import java.util.HashMap;
import java.util.List;

public  class CountQuery<T extends Object>{
    private T expression;
    private T expression2;
    public CountQuery(T obj,T obj2){
        expression = obj;
        expression2 =obj2;
    }
    public CountQuery(T obj){
        expression = obj;
    }
    public void evaluate(){
        Long t1 = evaluateQuery(expression!=null?expression:"");
        Long t2 = evaluateQuery(expression2!=null?expression2:"");
        CardinalityVisitor.NodeDescriptor nodeDescriptor = new CardinalityVisitor.NodeDescriptor();
        nodeDescriptor.card = t1;
        CardinalityVisitor.CardPair cardPair = new CardinalityVisitor.CardPair((TupleExpr) expression, nodeDescriptor);
        JoinOrderOptimizer.cardRealBind2.add(cardPair);

        CardinalityVisitor.NodeDescriptor nodeDescriptor2 = new CardinalityVisitor.NodeDescriptor();
        nodeDescriptor2.card = t2;
        CardinalityVisitor.CardPair cardPair2 = new CardinalityVisitor.CardPair((TupleExpr) expression2, nodeDescriptor2);
        JoinOrderOptimizer.cardRealBind2.add(cardPair2);

    }
    public Long evaluate2(){
        return evaluateQuery(expression!=null?expression:"");

    }


    public static String tupleExp = "";
    public static String prefixes = "";
    public static HashMap<String, String> prefixes1 = new HashMap<String, String>();

    public static long evaluateQuery(Object expr) {
        if (expr instanceof ExclusiveGroup) {
            return getTripleCardinalityExGroups(((ExclusiveGroup) expr).getStatementSources().get(0).getEndpointID().toString(),
                    expr);
        } else if (expr instanceof ExclusiveStatement) {
            return getTripleCardinalityExStatement(((ExclusiveStatement) expr).getStatementSources().get(0).getEndpointID().toString(),
                    expr);
        } else if (expr instanceof StatementSourcePattern) {
            return getTripleCardinalitySources(((StatementSourcePattern) expr).getStatementSources().get(0).getEndpointID().toString(),
                    expr);
        }
        return 0;
    }

    public static long getTripleCardinalityExGroups(String endPoint, Object expr) {
        //((ExclusiveStatement) ((ArrayList) ((ExclusiveGroup) leftArg.expr).owned).get(0)).objectVar
        String subject, predicate, object;
        int counter = 0;
        String[] test2;
        tupleExp = "";
        for (int i = 0; i < ((ExclusiveGroup) expr).getStatements().size(); i++) {
            subject = "";
            predicate = "";
            object = "";

            //subject value
            if (!((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().isConstant()) {
                subject = "?" + ((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getName();
            } else {
                if (((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString().contains("/")) {
                    if (((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString().contains("#")) {
                        subject = "ds" + counter + ":" + ((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString().split("#")[1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString().split("#")[0] + "#> \n");
                        counter++;
                    } else {
                        String test[] = ((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString().split("/");
                        subject = "ds" + counter + ":" + test[test.length - 1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                        counter++;
                    }

                } else {
                    subject = ((ExclusiveGroup) expr).getStatements().get(i).getSubjectVar().getValue().toString();
                }
            }


            ///Predicate Value
            if (!((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().isConstant()) {
                predicate = "?" + ((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getName();
            } else {
                if (((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString().contains("/")) {
                    if (((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString().contains("#")) {
                        predicate = "ds" + counter + ":" + ((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString().split("#")[1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString().split("#")[0] + "#> \n");
                        counter++;
                    } else {
                        String test[] = ((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString().split("/");
                        predicate = "ds" + counter + ":" + test[test.length - 1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                        counter++;
                    }

                } else {
                    predicate = ((ExclusiveGroup) expr).getStatements().get(i).getPredicateVar().getValue().toString();
                }
            }
            if (!((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().isConstant()) {
                object = "?" + ((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getName();
            } else {
                if (((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString().contains("/")) {
                    if (((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString().contains("#")) {
                        object = "ds" + counter + ":" + ((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString().split("#")[1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString().split("#")[0] + "#> \n");
                        counter++;
                    } else {
                        String test[] = ((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString().split("/");
                        object = "ds" + counter + ":" + test[test.length - 1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                        counter++;
                    }

                } else {
                    object = ((ExclusiveGroup) expr).getStatements().get(i).getObjectVar().getValue().toString();
                }
            }
            tupleExp += subject + " " + predicate + " " + object + ". " + "\n";
        }
        endPoint = endPoint.replace("sparql_", "http://").replace("_sparql", "/sparql");
        return Cardinality.getTripleCount(endPoint, tupleExp, prefixes1);
    }

    public static long getTripleCardinalityExStatement(String endPoint, Object expr) {
        //((ExclusiveStatement) ((ArrayList) ((ExclusiveGroup) leftArg.expr).owned).get(0)).objectVar
        String subject, predicate, object;
        int counter = 0;
        String[] test2;
        tupleExp = "";
//        for(int i=0; i < ((ExclusiveStatement)expr)..getStatements().size(); i++)
        {
            subject = "";
            predicate = "";
            object = "";

            //subject value
            if (!((ExclusiveStatement) expr).getSubjectVar().isConstant()) {
                subject = "?" + ((ExclusiveStatement) expr).getSubjectVar().getName();
            } else {
                if (((ExclusiveStatement) expr).getSubjectVar().getValue().toString().contains("/")) {
                    if (((ExclusiveStatement) expr).getSubjectVar().getValue().toString().contains("#")) {
                        subject = "ds" + counter + ":" + ((ExclusiveStatement) expr).getSubjectVar().getValue().toString().split("#")[1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveStatement) expr).getSubjectVar().getValue().toString().split("#")[0] + "#> \n");
                        counter++;
                    } else {
                        String test[] = ((ExclusiveStatement) expr).getSubjectVar().getValue().toString().split("/");
                        subject = "ds" + counter + ":" + test[test.length - 1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveStatement) expr).getSubjectVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                        counter++;
                    }

                } else {
                    subject = ((ExclusiveStatement) expr).getSubjectVar().getValue().toString();
                }
            }


            ///Predicate Value
            if (!((ExclusiveStatement) expr).getPredicateVar().isConstant()) {
                predicate = "?" + ((ExclusiveStatement) expr).getPredicateVar().getName();
            } else {
                if (((ExclusiveStatement) expr).getPredicateVar().getValue().toString().contains("/")) {
                    if (((ExclusiveStatement) expr).getPredicateVar().getValue().toString().contains("#")) {
                        predicate = "ds" + counter + ":" + ((ExclusiveStatement) expr).getPredicateVar().getValue().toString().split("#")[1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveStatement) expr).getPredicateVar().getValue().toString().split("#")[0] + "#> \n");
                        counter++;
                    } else {
                        String test[] = ((ExclusiveStatement) expr).getPredicateVar().getValue().toString().split("/");
                        predicate = "ds" + counter + ":" + test[test.length - 1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveStatement) expr).getPredicateVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                        counter++;
                    }

                } else {
                    predicate = ((ExclusiveStatement) expr).getPredicateVar().getValue().toString();
                }
            }
            if (!((ExclusiveStatement) expr).getObjectVar().isConstant()) {
                object = "?" + ((ExclusiveStatement) expr).getObjectVar().getName();
            } else {
                if (((ExclusiveStatement) expr).getObjectVar().getValue().toString().contains("/")) {
                    if (((ExclusiveStatement) expr).getObjectVar().getValue().toString().contains("#")) {
                        object = "ds" + counter + ":" + ((ExclusiveStatement) expr).getObjectVar().getValue().toString().split("#")[1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveStatement) expr).getObjectVar().getValue().toString().split("#")[0] + "#> \n");
                        counter++;
                    } else {
                        String test[] = ((ExclusiveStatement) expr).getObjectVar().getValue().toString().split("/");
                        object = "ds" + counter + ":" + test[test.length - 1];
                        prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((ExclusiveStatement) expr).getObjectVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                        counter++;
                    }

                } else {
                    object = ((ExclusiveStatement) expr).getObjectVar().getValue().toString();
                }
            }
            tupleExp += subject + " " + predicate + " " + object + ". " + "\n";
        }
        endPoint = endPoint.replace("sparql_", "http://").replace("_sparql", "/sparql");
        return Cardinality.getTripleCount(endPoint, tupleExp, prefixes1);
    }

    public static long getTripleCardinalitySources(String endPoint, Object expr) {
        //((ExclusiveStatement) ((ArrayList) ((ExclusiveGroup) leftArg.expr).owned).get(0)).objectVar
        String subject, predicate, object;
        int counter = 0;
        String[] test2;
        tupleExp = "";
        subject = "";
        predicate = "";
        object = "";

        //subject value
        if (!((StatementSourcePattern) expr).getSubjectVar().isConstant()) {
            subject = "?" + ((StatementSourcePattern) expr).getSubjectVar().getName();
        } else {
            if (((StatementSourcePattern) expr).getSubjectVar().getValue().toString().contains("/")) {
                if (((StatementSourcePattern) expr).getSubjectVar().getValue().toString().contains("#")) {
                    subject = "ds" + counter + ":" + ((StatementSourcePattern) expr).getSubjectVar().getValue().toString().split("#")[1];
                    prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((StatementSourcePattern) expr).getSubjectVar().getValue().toString().split("#")[0] + "#> \n");
                    counter++;
                } else {
                    String test[] = ((StatementSourcePattern) expr).getSubjectVar().getValue().toString().split("/");
                    subject = "ds" + counter + ":" + test[test.length - 1];
                    prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((StatementSourcePattern) expr).getSubjectVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                    counter++;
                }

            } else {
                subject = ((StatementSourcePattern) expr).getSubjectVar().getValue().toString();
            }
        }


        ///Predicate Value
        if (!((StatementSourcePattern) expr).getPredicateVar().isConstant()) {
            predicate = "?" + ((StatementSourcePattern) expr).getPredicateVar().getName();
        } else {
            if (((StatementSourcePattern) expr).getPredicateVar().getValue().toString().contains("/")) {
                if (((StatementSourcePattern) expr).getPredicateVar().getValue().toString().contains("#")) {
                    predicate = "ds" + counter + ":" + ((StatementSourcePattern) expr).getPredicateVar().getValue().toString().split("#")[1];
                    prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((StatementSourcePattern) expr).getPredicateVar().getValue().toString().split("#")[0] + "#> \n");
                    counter++;
                } else {
                    String test[] = ((StatementSourcePattern) expr).getPredicateVar().getValue().toString().split("/");
                    predicate = "ds" + counter + ":" + test[test.length - 1];
                    prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((StatementSourcePattern) expr).getPredicateVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                    counter++;
                }

            } else {
                predicate = ((StatementSourcePattern) expr).getPredicateVar().getValue().toString();
            }
        }

        //Object Value
        if (!((StatementSourcePattern) expr).getObjectVar().isConstant()) {
            object = "?" + ((StatementSourcePattern) expr).getObjectVar().getName();
        } else {
            if (((StatementSourcePattern) expr).getObjectVar().getValue().toString().contains("/")) {
                if (((StatementSourcePattern) expr).getObjectVar().getValue().toString().contains("#")) {
                    object = "ds" + counter + ":" + ((StatementSourcePattern) expr).getObjectVar().getValue().toString().split("#")[1];
                    prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((StatementSourcePattern) expr).getObjectVar().getValue().toString().split("#")[0] + "#> \n");
                    counter++;
                } else {
                    String test[] = ((StatementSourcePattern) expr).getObjectVar().getValue().toString().split("/");
                    object = "ds" + counter + ":" + test[test.length - 1];
                    prefixes1.put("ds" + counter, "Prefix ds" + counter + ":<" + ((StatementSourcePattern) expr).getObjectVar().getValue().toString().replace(test[test.length - 1], "") + "> \n");
                    counter++;
                }

            } else {
                object = ((StatementSourcePattern) expr).getObjectVar().getValue().toString();
            }
        }
        tupleExp += subject + " " + predicate + " " + object + ". " + "\n";
        long totalCard = (long) 0;
        List<StatementSource> endpointList = ((StatementSourcePattern) expr).getStatementSources();
        for (int i = 0; i < endpointList.size(); i++) {
            endPoint = endpointList.get(i).getEndpointID();
            endPoint = endPoint.replace("sparql_", "http://").replace("_sparql", "/sparql");
            totalCard += Cardinality.getTripleCount(endPoint, tupleExp, prefixes1);
        }
        return totalCard;
    }
}



