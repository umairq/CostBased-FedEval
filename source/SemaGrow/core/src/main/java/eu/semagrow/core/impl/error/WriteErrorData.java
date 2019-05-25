package eu.semagrow.core.impl.error;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * This class is for writing estimated and real cardinality of query plans generated by query optimizers and calculating the
 * cosine similarity.
 */
public class WriteErrorData {
    public static String q_name ="q";
    public static String approach_name = "semagrow";
    public static String file_name = approach_name.toUpperCase()+"-RelativeError.txt";
    public static String aksw_schema = "<http://aksw.org/schema/";
    public static String aksw_res = "<http://aksw.org/res/";
    public static String plan = aksw_res+approach_name+"-"+ q_name +"-plan>";
    public static int join_number = 1;
    public static int union_number = 1;
    public static String hasJoin =aksw_schema+"hasJoin> ";
    public static String hasUnion =aksw_schema+"hasUnion> ";
    public static String hasTP =aksw_schema+"hasTriplePattern> ";
    public static String hasAccCard = aksw_schema + "hasActualCardinality> ";
    public static String hasEstCard = aksw_schema + "hasEstimatedCardinality> ";



//    public static String plan_join = aksw_res+approach_name+"-"+ q_name +"-plan-join#"+join_number+">";
    public static String plan_join_tp = aksw_res+approach_name+"-"+ q_name +"-plan-join#"+join_number+"-tp";

//    public static String queryPlanTxt =  aksw_res+"query#"+ q_name +">  <"+aksw_schema+"queryPlan> ";

    public static String join_text = aksw_res+approach_name+"-"+q_name+"-plan-join#"+join_number;
    public static String literal_type = "^^<http://www.w3.org/2001/XMLSchema#Double>";


    //for calculating the Similarity constant
    static HashMap<HashMap<String,Integer>,Integer> estimated_values = new HashMap<HashMap<String, Integer>, Integer>();
    static HashMap<HashMap<String,Integer>,Integer> real_values = new HashMap<HashMap<String, Integer>, Integer>();
    public static int plan_num = 0;

    private static void writeToFile(String result) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file_name, true));
        writer.append(result);
        writer.close();
    }

    public static void pre_initfileRelativeError(String relativeErrorFilePAth){
        try {
            if(relativeErrorFilePAth!=""){
                file_name = relativeErrorFilePAth+"/"+file_name;
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file_name));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void initializeFile(String  query, String type){
        try {
            WriteErrorData.q_name = type;
            plan_num =0;
            BufferedWriter writer = new BufferedWriter(new FileWriter(file_name,true));
            writer.append("\n"+aksw_res+"query#"+type+"> "+aksw_schema+"text> \""+query.replace("\n","").replace("\r","")+"\" .\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     * Calculating the similarity constant between the real and estimated values
     * */
    public static void calculateSimilarity(){

            System.out.println("Similarity Plan:"+SimilarityConstants.calculateAngleBetweenVector());
            try {
                plan = aksw_res+approach_name+"-"+ q_name +"-plan>"+ " " + aksw_schema + "hasSimilarity> ";

                writeToFile("\n\n" +
                        plan + "\"" +SimilarityConstants.calculateAngleBetweenVector()+ "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            SimilarityConstants.realValues.clear();
            SimilarityConstants.estimatedValues.clear();




    }

    public static void writeQueryPlan(String query){
        q_name = query;
        String queryPlanTxt =  aksw_res+"query#"+ query +"> "+aksw_schema+"queryPlan> ";
        String planText = queryPlanTxt+aksw_res+approach_name+"-"+query+"-plan> .\n";
        //query_name+ queryPlan;
        try {
            writeToFile(planText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeJoinInfoToFile(double est, double real){
        String plan_join = aksw_res+approach_name+"-"+ q_name +"-plan-join#"+join_number+"> ";
        String planText = aksw_res+approach_name+"-"+q_name+"-plan> " + hasJoin + plan_join + " .\n" ;
        planText +=  plan_join + hasAccCard +"\""+ real +"\""+literal_type +" .\n";
        planText +=  plan_join + hasEstCard +"\""+ est +"\""+literal_type +" .\n";
        try {
            writeToFile(planText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeUnionInfoToFile(double est, double real){
        String plan_union = aksw_res+approach_name+"-"+ q_name +"-plan-union#"+union_number+"> ";
        String planText = aksw_res+approach_name+"-"+q_name+"-plan> " + hasJoin + plan_union + " .\n" ;
        planText +=  plan_union + hasAccCard +"\""+ real +"\""+literal_type +" .\n";
        planText +=  plan_union + hasEstCard +"\""+ est +"\""+literal_type +" .\n";
        try {
            writeToFile(planText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeJoinTPInfoToFile(double est, double real, int tp){
        String plan_join = aksw_res+approach_name+"-"+ q_name +"-plan-join#"+join_number+"-tp"+tp+"> ";
        String planText = aksw_res+approach_name+"-"+q_name+"-plan-join#"+join_number+"> " + hasTP + plan_join + " .\n" ;
        planText +=  plan_join + hasAccCard +"\""+ real +"\""+literal_type +" .\n";
        planText +=  plan_join + hasEstCard +"\""+ est +"\""+literal_type +" .\n";
        try {
            writeToFile(planText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeUnionTPInfoToFile(double est, double real, int tp){
        String plan_union = aksw_res+approach_name+"-"+ q_name +"-plan-union#"+union_number+"-tp"+tp+"> ";
        String planText = aksw_res+approach_name+"-"+q_name+"-plan-union#"+union_number+"> " + hasTP + plan_union + " .\n" ;
        planText +=  plan_union + hasAccCard +"\""+ real +"\""+literal_type +" .\n";
        planText +=  plan_union + hasEstCard +"\""+ est +"\""+literal_type +" .\n";
        try {
            writeToFile(planText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeIndvidualTPInfoToFile(double est, double real, int tp){
        String plan_tp = aksw_res+approach_name+"-"+ q_name +"-plan-tp#"+tp+"> ";
        String planText = aksw_res+approach_name+"-"+q_name+"-plan> " + hasTP + plan_tp + " .\n" ;
        planText +=  plan_tp + hasAccCard +"\""+ real +"\""+literal_type +" .\n";
        planText +=  plan_tp + hasEstCard +"\""+ est +"\""+literal_type +" .\n";
        try {
            writeToFile(planText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveDataForSimilarity() {
//        for()
    }

}


