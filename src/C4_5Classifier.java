import java.util.*;
import java.io.*;

/**
 * Author Wenxuan Wang
 * wenxuan.wang@emory.edu
 * Date 3/5/2017
 */
public class C4_5Classifier {
    private static int correct = 0;
    private static int wrong = 0;

    /**
     * store values belonging to a given attribute
     * use another Value type to store appearance of values with different classes
     */
    static class Attribute{
        class Value{
            Map<String, Integer> count = new HashMap<>();
        }

        public int attrNumber;
        Map<String, Value> valueMap;

        /**
         * initialization
         * @param attrNumber
         */
        public Attribute(int attrNumber){
            this.attrNumber = attrNumber;
            this.valueMap = new HashMap<>();
        }

        /**
         * update the count by using the key and the class of the key
         * @param key the value under attribute
         * @param cat the classification of the entry
         */
        public void add(String key, String cat) {
            if(!valueMap.containsKey(key)) {
                valueMap.put(key, new Value());
            }

            Value v = valueMap.get(key);
            v.count.put(cat, v.count.getOrDefault(cat, 0) + 1);
        }
    }

    /**
     *  n-ary tree structure
     *  used for storing the label, classification and child nodes
     */
    static class TreeNode{
        public int level;
        public String val;
        public Attribute attribute;
        public String classification;
        public List<TreeNode> child = new ArrayList<>();
        public List<String[]> entry = new ArrayList<>();

        public TreeNode(String val) {
            this.val = val;
        }

        public void setLevel(int level) { this.level = level; }
        public void setAttribute(Attribute attribute) { this.attribute = attribute; }
        public void setClassification(String classification) { this.classification = classification; }
        public void setEntry(List<String[]> entry) { this.entry = entry; }
    }

    private static List<String[]> entry = new ArrayList<>();
    private static List<String[]> test = new ArrayList<>();
    private static List<Attribute> attributeList = new ArrayList<>();
    private static Map<Attribute, Boolean> attributeUsage = new HashMap<>();
    private static Map<String, Integer> classCount = new HashMap<>();

    private static TreeNode root = new TreeNode("null");

    public static void main(String[] args) {
        try{
            preprocess(args[0]);
            initRoot();
            treeBuilder(root);
            show();
            test(args[1]);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if(correct + wrong != 0)
                System.out.printf("%.2f%%\n", correct * 100.0 / (correct + wrong));
            else
                throw new ArithmeticException();
        }
    }

    /**
     * use the testing file and test against the built decision tree
     * @param testingFile testing file
     * @throws IOException
     */
    public static void test(String testingFile) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(testingFile));
        String line;
        while((line = br.readLine()) != null) {
            test.add(line.split("\\t"));
        }

        for(int i = 0; i < test.size(); i++) {
            testHelper(root, test.get(i));
        }
    }

    /**
     * helper method for test
     * @param root the starting node the method call
     * @param row entry
     */
    public static void testHelper(TreeNode root, String[] row) {
        if(root.classification != null) {
            if(root.classification.equals(row[0])) {
                correct++;
                System.out.printf("Correct: %s\n", Arrays.toString(row));
            }
            else {
                wrong++;
                System.out.printf("Wrong:   %s\n", Arrays.toString(row));
            }
        }
        else{
            for(int i = 0; i < root.child.size(); i++) {
                TreeNode childNode = root.child.get(i);
                if(row[childNode.attribute.attrNumber].equals(childNode.val))
                    testHelper(childNode, row);
            }
        }
    }

    /**
     * read in files and generate attribute structures
     * @param trainingFile training file
     * @throws IOException
     */
    public static void preprocess(String trainingFile) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(trainingFile));
        String line;

        while((line = br.readLine()) != null) {
            String[] row = line.split("\\t");

            if(entry.size() == 0) {
                for (int i = 0; i < row.length - 1; i++) {
                    attributeList.add(new Attribute(i + 1));
                    attributeUsage.put(attributeList.get(attributeList.size() - 1), true);
                }
            }
            entry.add(row);
        }

        for(String[] row : entry) {
            for(int i = 0; i < row.length - 1; i++) {
                attributeList.get(i).add(row[i + 1], row[0]);
            }
        }
    }

    /**
     * initialize the root
     */
    public static void initRoot() {
        root.setEntry(entry);
        root.setAttribute(null);
        root.setLevel(0);
    }

    /**
     * build the decision tree by calculating gainInfo
     * @param node
     */
    public static void treeBuilder(TreeNode node) {
        if(node.entry.size() == 0)  return;

        classCount.clear();
        int availableAttr = 0;
        for(Attribute a : attributeUsage.keySet()) {
            if(attributeUsage.get(a))   availableAttr++;
        }
        if(availableAttr == 0)   return;

        if(!node.val.equals("null")) {
            for(Attribute a : attributeList) {
                if(attributeUsage.get(a))   a.valueMap.clear();
            }
        }

        for(String[] row : node.entry) {
            if(!node.val.equals("null")) {
                for(int i = 0; i < row.length - 1; i++) {
                    Attribute a = attributeList.get(i);
                    if(attributeUsage.get(a))
                        a.add(row[i + 1], row[0]);
                }
            }
            classCount.put(row[0], classCount.getOrDefault(row[0], 0) + 1);
        }

        if(classCount.size() == 1) {
            node.setClassification(new ArrayList<>(classCount.keySet()).get(0));
            return;
        }

        Attribute nextAttr = nextAttribute(classCount, node.entry);
        if(nextAttr == null)    return;
        attributeUsage.put(nextAttr, false);

        for(String s : nextAttr.valueMap.keySet()) {
            TreeNode childNode = new TreeNode(s);
            childNode.level = node.level + 1;
            childNode.attribute = nextAttr;
            node.child.add(childNode);
        }

        for(String[] row : node.entry) {
            for(TreeNode childNode : node.child) {
                if(row[nextAttr.attrNumber].equals(childNode.val))
                    childNode.entry.add(row);
            }
        }

        for(TreeNode childNode : node.child)
            treeBuilder(childNode);
    }

    /**
     * select next Attribute by calculating gain ratio
     * @param classCount hashmap containing the class labels and their counts
     * @param partition parition of the entry list
     * @return next Attribute
     */
    public static Attribute nextAttribute(Map<String, Integer> classCount, List<String[]> partition) {
        List<Double> doubleClassList = new ArrayList<>();
        for(Integer i : classCount.values()) {
            doubleClassList.add(i * 1.0);
        }
        double classInfo = calculateInfo(doubleClassList);

        double maxGainRatio = Integer.MIN_VALUE * 1.0;
        Attribute nextAttribute = null;

        for(Attribute a : attributeList) {
             if(!attributeUsage.get(a)) continue;
             double attributeInfo = 0;
             double splitInfo = 0;

             for(String s : a.valueMap.keySet()) {
                List<Integer> count = new ArrayList<>(a.valueMap.get(s).count.values());

                int sum = count.stream().mapToInt(Integer::intValue).sum();
                List<Double> doubleCount = new ArrayList<>();
                for(Integer i : count)
                    doubleCount.add(i * 1.0);

                attributeInfo += (1.0 * sum / partition.size()) * calculateInfo(doubleCount);
                splitInfo += (-1.0 * sum / partition.size()) * (Math.log(1.0 * sum / partition.size()) / Math.log(2));
             }
             double gainRatio = (classInfo - attributeInfo) / (splitInfo + 1 / Double.MAX_VALUE);
             if(gainRatio > maxGainRatio) {
                 maxGainRatio = gainRatio;
                 nextAttribute = a;
             }
        }
        return nextAttribute;
    }

    /**
     * calculate the info of a given list
     * @param list a list of Double object
     * @return a value representing the info the input list
     */
    public static double calculateInfo(List<Double> list) {
        double sum = list.stream().mapToDouble(d->d).sum();
        double info = 0.0;
        for(int i = 0; i < list.size(); i++) {
            Double num = list.get(i);
            info += (-1.0 * num / sum ) * (Math.log(num / sum) / Math.log(2));
        }
        return info;
    }

    /**
     * used for printing the tree in the way of weka
     * @param node TreeNode to start
     * @param level level of the current node, used for spacing
     */
    public static void traverse(TreeNode node, int level) {
        for(int i = 0; i < node.child.size(); i++) {
            if(node.attribute != null) {
                for(int j = -1; j < level - 1; j++)
                    System.out.print("|        ");

                System.out.println(node.attribute.attrNumber + " : " + node.val);
            }
            traverse(node.child.get(i), level+1);
        }
    }

    /**
     * print the structure of the decision tree
     */
    public static void show() {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while(!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if(node.attribute != null)
                System.out.printf("attribute index: %3d ,value: %4s\n", node.attribute.attrNumber, node.val);

            for(int i = 0; i < node.child.size(); i++) {
                queue.offer(node.child.get(i));
            }
        }
    }
}
