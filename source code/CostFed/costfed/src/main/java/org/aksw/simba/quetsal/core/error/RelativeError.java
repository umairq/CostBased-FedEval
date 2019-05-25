package org.aksw.simba.quetsal.core.error;


import com.fluidops.fedx.Config;
import com.fluidops.fedx.algebra.ExclusiveStatement;
import org.aksw.simba.quetsal.core.CardinalityVisitor;
import org.aksw.simba.quetsal.core.JoinOrderOptimizer;
import org.aksw.simba.quetsal.util.CountQuery;
import org.aksw.simba.quetsal.util.SimilarityConstants;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class RelativeError {

    public static String aksw_schema = "<http://aksw.org/schema/";
    public static String aksw_res = "<http://aksw.org/res/";
    public static String proj =  "CostFed";
    public static String literal_type = "^^<http://www.w3.org/2001/XMLSchema#Double>";
    public static String file_name = proj+"-RelativeError.txt";
    public static String query_name =  aksw_res+"query#b1>  "+aksw_schema+"queryPlan> ";


    public static Config config=null;
    public static void setQuery_name(String qrytext){
        query_name.replace("b1",qrytext);
    }
    public static float getRelativeError(float estimatedCord,float realCord) {
        return (estimatedCord - realCord)/ realCord;
    }

    public static HashMap<CardinalityVisitor.CardPair,Integer> tp_cardinalities = new HashMap<CardinalityVisitor.CardPair, Integer>();
    public static void relativeErrorCardPair(List<CardinalityVisitor.CardPair> estimatedCord, int realCord){
        String errText ="";
        for(CardinalityVisitor.CardPair pair:estimatedCord) {
            if(!pair.expr.toString().contains("HashJoin")&&!pair.expr.toString().contains("BindJoin")) {

//                errText = "\n Branch:" +
////                        "" + pair.expr + "" +
//                        "\n" + "Real Cardinality: " + realCord + "\nEstimated Cardinality: " +
//                        pair.nd.card + "\nRelative Error: " +
//                        RelativeError.getRelativeError(pair.nd.card, realCord)+
//                        "\n-------------------\n";
                tp_cardinalities.put(new CardinalityVisitor.CardPair(pair),realCord);
                SimilarityConstants.estimatedValues.add(pair.nd.card);
                SimilarityConstants.realValues.add(realCord);
            }
        }
//        try {
//            writeToFile(errText);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    public static void pre_initfileRelativeError(String path){

        WriteErrorData.pre_initfileRelativeError(path);
    }
    public static void relativeErrorCardPairTP(CardinalityVisitor.CardPair estimatedCord, int realCord){
        String errText ="";
            if(!estimatedCord.expr.toString().contains("HashJoin")&&!estimatedCord.expr.toString().contains("BindJoin")) {
                tp_cardinalities.put(new CardinalityVisitor.CardPair(estimatedCord),realCord);
                SimilarityConstants.estimatedValues.add(estimatedCord.nd.card);
                SimilarityConstants.realValues.add(realCord);
            }
    }


    public static void writeQueryPlan(){
        WriteErrorData.writeQueryPlan(query_name);
    }
    /*
    * Writing similarity to file
    * */
    public static void writeSimilarityValue(){
        WriteErrorData.calculateSimilarity();
    }
    public static int join_number = 0;
    public static void relativeErrorJoinCard(String joinType, List<CardinalityVisitor.JoinCard> estimatedCord, int real_cord){
        String errText2="";
        String plan = "";
        String plan_join = "";
        String plan_join_tp ="";
        for (int i=0;i<estimatedCord.size();i++) {
            plan = aksw_res+proj+ "-"+ query_name +"-plan"+">";
            plan_join = aksw_res+proj+"-" + query_name + "-plan"+"-join#" + (join_number + 1) + ">";
            plan_join_tp = aksw_res+proj+"-" + query_name + "-plan"+"-join#" + (join_number + 1) + "-tp";


//            errText2 += "\n-------------------\n"+joinType+" Real Cardinality: " + real_cord +
//                    "\nJoin Estimated: " + estimatedCord.get(i).nd.card +
//                    "\nJoin Relative Error: " + RelativeError.getRelativeError(estimatedCord.get(i).nd.card, real_cord) +
//                    "\n-------------------\n";
//            errText2 =
            errText2 += "" + plan + " " + aksw_schema + "hasJoin> " + plan_join + " .\n"
                    + plan_join + " " + aksw_schema + "hasActualCardinality> \"" + real_cord + "\"" + literal_type + " .\n" +
                    plan_join + " " + aksw_schema + "hasEstimatedCardinality> \"" + estimatedCord.get(i).nd.card + "\"" + literal_type + " .\n";
            WriteErrorData.writeJoinInfoToFile(estimatedCord.get(i).nd.card,real_cord);

            Iterator iterator = tp_cardinalities.keySet().iterator();
            int j =1;
            while (iterator.hasNext()){
                String tp = plan_join_tp + j + ">";
                CardinalityVisitor.CardPair cardPair = (CardinalityVisitor.CardPair)iterator.next();
                if(estimatedCord.get(i).expr1.equals(cardPair.expr)){

                    errText2 += plan_join + " " + aksw_schema + "hasTriplePattern> " + tp + " .\n";
                    errText2 += tp + " " + aksw_schema + "hasActualCardinality> \"" + tp_cardinalities.get(cardPair) + "\"" + literal_type + " .\n";
                    errText2 += tp + " " + aksw_schema + "hasEstimatedCardinality> \"" + cardPair.nd.card  + "\"" + literal_type + " .\n";
                    WriteErrorData.writeJoinTPInfoToFile(cardPair.nd.card,tp_cardinalities.get(cardPair),j);
                }
                if(estimatedCord.get(i).expr2.equals(cardPair.expr)){

                    errText2 += plan_join + " " + aksw_schema + "hasTriplePattern> " + tp + " .\n";
                    errText2 += tp + " " + aksw_schema + "hasActualCardinality> \"" + tp_cardinalities.get(cardPair) + "\"" + literal_type + " .\n";
                    errText2 += tp + " " + aksw_schema + "hasEstimatedCardinality> \"" + cardPair.nd.card  + "\"" + literal_type + " .\n";
                    WriteErrorData.writeJoinTPInfoToFile(cardPair.nd.card,tp_cardinalities.get(cardPair),j);
                }
                j++;
            }
            SimilarityConstants.realValues.add(real_cord);
            SimilarityConstants.estimatedValues.add(estimatedCord.get(i).nd.card);
            join_number++;
        }
//        try {
//            writeToFile(errText2);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    public static void remainingTPError(){
        if(JoinOrderOptimizer.estCardPairs2.size()>0){
            String plan = "";
            String plan_tp = "";
            String errText2="";

            plan = aksw_res+proj+ "-"+ query_name +"-plan"+">";
            List<CardinalityVisitor.CardPair> estCardPairs = new ArrayList<CardinalityVisitor.CardPair>(JoinOrderOptimizer.estCardPairs2);

            for(int i=0; i<estCardPairs.size();i++){
                if(estCardPairs.get(i).expr instanceof ExclusiveStatement ){
                    CountQuery countQuery = new CountQuery(estCardPairs.get(i).expr);
                    long realCard = countQuery.evaluate2();

                    plan_tp = aksw_res+proj+"-" + query_name + "-plan"+"-tp#" + (i+1) +">" ;
                    errText2 += "" + plan + " " + aksw_schema + "hasTriplePattern> " + plan_tp + " .\n"
                            + plan_tp + " " + aksw_schema + "hasActualCardinality> \"" + realCard + "\"" + literal_type + " .\n" +
                            plan_tp + " " + aksw_schema + "hasEstimatedCardinality> \"" + estCardPairs.get(i).nd.card + "\"" + literal_type + " .\n";
                    WriteErrorData.writeIndvidualTPInfoToFile(estCardPairs.get(i).nd.card,realCard,(i+1));
                    SimilarityConstants.realValues.add((int)realCard);
                    SimilarityConstants.estimatedValues.add(estCardPairs.get(i).nd.card);
                    JoinOrderOptimizer.estCardPairs2.remove(estCardPairs.get(i));
                }

            }
//            JoinOrderOptimizer.estCardPairs2.clear();
//            try {
//                writeToFile(errText2);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }


    }
//    private static void writeToFile(String result) throws IOException {
//        BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
//        writer.append(result);
//        writer.close();
//    }

    public static void initializeFile(String  query_name, String query_txt){
        tp_cardinalities = new HashMap<CardinalityVisitor.CardPair, Integer>();
        RelativeError.query_name = query_name;
        WriteErrorData.initializeFile(query_txt,query_name);

//        try {
//            tp_cardinalities = new HashMap<CardinalityVisitor.CardPair, Integer>();
//            join_number =0;
//            BufferedWriter writer = new BufferedWriter(new FileWriter(file_name,true));
//            writer.append("\n\n"+aksw_res+"query#"+query_name+">  "+aksw_schema+"text> \""+query_txt.replace("\n","").replace("\r","")+"\" .\n");
//            RelativeError.query_name = query_name;
////            writer.append("\n====================================\n\t\nQuery=");
////            writer.append( query + "\n");
////            writer.append("\n====================================\nCardinality Data:\n");
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    //Not used yet
//    public static void initializeWriter(String query){
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter("RelativeError.txt"));
//            writer.append("\tRelative Errors File \n====================================\n\t\nQuery Plan\n\n====================================\n");
//            writer.append("\n" + query + "\n");
//            writer.append("\n====================================\nData\n");
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
