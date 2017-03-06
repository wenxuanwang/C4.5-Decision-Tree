import java.util.*;
import java.io.*;

/**
 * Author Wenxuan Wang
 * wenxuan.wang@emory.edu
 * Date 3/5/2017
 */
public class BayesianClassifier {
    private static int correct = 0;
    private static int wrong = 0;

    /**
     * tuple structure used for storing the counts
     */
    static class Tuple{
        private Map<String,Value> attributeMap = new HashMap<>();

        public void add(String attribute, String value) {
            if(attributeMap.containsKey(attribute)) {
                attributeMap.get(attribute).update(value);
            }
            else {
                attributeMap.put(attribute, new Value());
                attributeMap.get(attribute).init(value);
            }
        }

        public int get(String attribute, String value) {
            return attributeMap.containsKey(attribute) ? attributeMap.get(attribute).get(value) : 1;
        }

        class Value {
            private Map<String,Integer> valueCount = new HashMap<>();

            public void init(String value) { valueCount.put(value, 1); }

            public void update(String value) { valueCount.put(value, valueCount.getOrDefault(value,0) + 1); }

            public int get(String value) { return valueCount.containsKey(value) ? valueCount.get(value) : 1; }
        }

    }

    private static Map<String, Integer> classCounter = new HashMap<>();
    private static Map<Double, String> resultMap = new HashMap<>();
    private static Set<String> classLabel = new HashSet<>();
    private static List<String []> entry = new ArrayList<>();
    private static List<String []> test = new ArrayList<>();
    private static List<Tuple> attribute = new ArrayList<>();

    public static void main(String[] args) {
        try{
            preprocess(args);
            run();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally{
            if(correct + wrong != 0)
                System.out.printf("%.2f%%\n", correct * 100.0 / (correct + wrong));
            else
                throw new ArithmeticException();
        }
    }

    /**
     * calculate and compare the result using naive bayes method
     */
    public static void run() {
        List<String> classLabelList = new ArrayList<>(classLabel);
        for(String[] row : test) {
            double[] conditionalProb = new double[classCounter.size()];
            Arrays.fill(conditionalProb,1);

            for(int i = 1; i < row.length; i++) {
                for(int j = 0; j < classLabelList.size(); j++) {
                    conditionalProb[j] *= attribute.get(i).get(row[i], classLabelList.get(j)) * 1.0 / classCounter.get(classLabelList.get(j));
                }
            }

            for(int i = 0; i < classLabelList.size(); i++) {
                conditionalProb[i] *= classCounter.get(classLabelList.get(i)) * 1.0 / entry.size();
                resultMap.put(conditionalProb[i], classLabelList.get(i));
            }

            double max = conditionalProb[0];
            for(int i = 1; i < conditionalProb.length; i++) {
                max = Math.max(max, conditionalProb[i]);
            }

            if(resultMap.get(max).equals(row[0])) {
                correct++;
                System.out.printf("%-10s %s\n", "Correct:", Arrays.toString(row));
            }
            else {
                wrong++;
                System.out.printf("%-10s %s\n", "Incorrect:", Arrays.toString(row));
            }
        }
    }

    /**
     * read the training file and build the attribute structure
     * @param args command line arguments
     * @throws Exception propagate exception to main method
     */
    public static void preprocess(String[] args) throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        String line;
        while((line = br.readLine()) != null) {
            String[] row = line.split("\\t");
            entry.add(row);
            classLabel.add(row[0]);
            classCounter.put(row[0], classCounter.getOrDefault(row[0], 0) + 1);
        }

        for(int i = 0; i < entry.get(0).length; i++) { attribute.add(new Tuple()); }

        for(int i = 1; i < entry.get(0).length; i++) {
            for(int j = 0; j < entry.size(); j++) {
                attribute.get(i).add(entry.get(j)[i], entry.get(j)[0]);
            }
        }

        br = new BufferedReader(new FileReader(args[1]));
        while((line = br.readLine()) != null) { test.add(line.split("\\t")); }
    }
}
