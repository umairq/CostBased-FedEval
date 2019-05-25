import java.util.ArrayList;

public class SimilarityConstants {
	public static ArrayList<Long> estimatedValues = new ArrayList<>();
	public static ArrayList<Long> realValues = new ArrayList<>();
	
	public static double calculateAngleBetweenVector() {
		 double result = 0;
	        long ab=0;
	        double a_norm =0;
	        double b_norm =0;
	        for(int i=0;i < estimatedValues.size(); i++){
	            ab += estimatedValues.get(i)*realValues.get(i);
	            a_norm += Math.pow(realValues.get(i),2);
	            b_norm += Math.pow(estimatedValues.get(i),2);
	        }
//	        System.out.println(realValues);
//	        System.out.println(estimatedValues);
//	        System.out.println(ab);
	        a_norm = Math.sqrt(a_norm);
	        b_norm = Math.sqrt(b_norm);
	        result = ab / (a_norm*b_norm);
//	        System.out.println(a_norm);
//	        System.out.println(b_norm);


	        return result;
	}
}
