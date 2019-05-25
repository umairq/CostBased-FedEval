
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
    public static String proj =  "Odyssey";
    public static String literal_type = "^^<http://www.w3.org/2001/XMLSchema#Double>";
    public static String file_name = proj+"-RelativeError.txt";
    public static String query_name =  aksw_res+"query#b1>  "+aksw_schema+"queryPlan> ";


    public static void setQuery_name(String qrytext){
        query_name.replace("b1",qrytext);
    }
    public static void pre_initfileRelativeError(String path, String q_name){

        WriteErrorData.pre_initfileRelativeError(path,q_name);
    }
    public static void initializeFile(String  query_name, String query_txt){
        RelativeError.query_name = query_name;
        WriteErrorData.initializeFile(query_txt,query_name);

//        }
    }
    public static void writeJoinInfo(long est, long real) {
    	WriteErrorData.writeJoinInfoToFile(est, real);
    }
    public static void writeJoinTPInfo(long est, long real, int tp) {
    	WriteErrorData.writeJoinTPInfoToFile(est, real, tp);//(est, real);
    }
    public static void writeTPInfo(long est, long real,int num) {
    	WriteErrorData.writeIndvidualTPInfoToFile(est, real,num);//(est, real);//(est, real);
    }
    
}
