import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by Ahmed Amr Abdul-Fattah on 1/24/17.
 */
public class NaiveBayesAgent extends Agent{
    static String DEFAULT_FILE_PATH = "data.txt";
    static ArrayList<String> agents ;
    static String MASTER_NAME = "master";
    String agentName ;
    Object[] args ;
    NaiveBayes naiveBayesModel;
    ArrayList<String> results;
    boolean allReceived = false;


    protected void setup() {
        agentName = getAID().getLocalName();
        args = getArguments();
        System.out.println("AGENT "+agentName+" HAS STARTED");
        if(agentName.equals(MASTER_NAME)) {
            //This is the master agent
            //init the agents list
            agents = new ArrayList<>();
            results = new ArrayList<>();
        } else {
            agents.add(agentName);
        }
        //Loads the Model
        naiveBayesModel = new NaiveBayes();
        naiveBayesModel.init(getFilePath());

        if(agentName.equals(MASTER_NAME)){
            //Create Receving Behavoiur
            addBehaviour(new RequestServer());
        }else{
            //add message handling behaviour
            addBehaviour(new ClassificationRequestServer());
        }
    }

    private String getFinalResult() {
        HashMap<String, Integer> votes = new HashMap<>();
        for (String result : results) {
            if (votes.containsKey(result)) {
                int val = votes.get(result);
                votes.put(result, val + 1);
            }else {
                votes.put(result, 1);
            }
        }

        //Getting the class with the max vote
        String maxVoteClass = "";
        int maxVote = -1;
        for (String s : votes.keySet()) {
            int count = votes.get(s);
            if(count>maxVote){
                maxVoteClass = s;
                maxVote = count;
            }
        }
        return maxVoteClass;
    }

    private void requestAllAgents(String test){
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        for (String agent : agents) {
            message.addReceiver(new AID(agent,AID.ISLOCALNAME));
        }
        message.setContent("requestClassification");
        message.setLanguage("English");
        message.setContent(test);
        send(message);
    }

    protected void takeDown() {
        //remove agent from the list of agents
        results.remove(agentName);
    }

    private String getFilePath(){
        if(args!=null && args.length>0){
            return args[0]+".txt";
        }
        return DEFAULT_FILE_PATH;
    }

    //INNER CLASS //SLAVES
    private class ClassificationRequestServer extends CyclicBehaviour{

        @Override
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                // Message received. Process it
                String[] testRecord = message.getContent().split(" ");
                String classificationResult = naiveBayesModel.classify(testRecord);
                ACLMessage reply = message.createReply();
                reply.setContent(classificationResult);
                send(reply);
            }
        }
    }

    private class RequestServer extends CyclicBehaviour{
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    results = new ArrayList<>();
                    allReceived = false;
                    ClassificationResponseServer resultReceiver = new ClassificationResponseServer();
                    System.out.println("#####################################");
                    System.out.println("#   HERE IS MASTER AGENT SERVING YOU#");
                    System.out.println("#####################################");
                    System.out.println("Please Enter The Record That You Want To Classify:: (Format is \"X Y Z K\")");
                    String test ;
                    try {
                        test = in.readLine();
                        resultReceiver.initRepliedNeeded(agents.size());
                        myAgent.addBehaviour(resultReceiver);
                        String result = naiveBayesModel.classify(test.split(" "));
                        results.add(result);
                        //request result from all agents
                        requestAllAgents(test);
                        step++;
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 1:
                    if(allReceived)step++;
                    break;
                case 2:
                    System.out.println("##################################");
                    System.out.println("FINAL CLASS IS "+getFinalResult());
                    step=0;
                    break;
            }
        }
    }

    //INNER CLASS //SLAVES
    private class ClassificationResponseServer extends Behaviour {

        private int repliesCount = 0;
        private int repliedNedded;


        public void initRepliedNeeded(int count){
            repliedNedded = count;
        }

        @Override
        public void action() {
            ACLMessage message = receive();
            if (message != null) {
                String classificationResult = message.getContent();
                results.add(classificationResult);
                repliesCount++;
            }
        }

        @Override
        public boolean done() {
            if(repliesCount==repliedNedded){
                allReceived=true;
                return true;
            }
            return false;
        }
    }

}
class NaiveBayes {
    int tuplesCount = 0;
    ArrayList<String[]> data;
    HashMap<String,Integer>classCount;
    HashMap<String,Double> prob;

    public void init(String filePath){
        data = loadData(filePath);
        tuplesCount = data.size();
        classCount = new HashMap<>();
        initClasses();
    }

    public void initProb(){
        prob = new  HashMap<String,Double>();
        for (String s : classCount.keySet()) {
            prob.put(s, (classCount.get(s) * 1.0 / tuplesCount));
        }

    }

    public String classify(String[] test){
        initProb();
        int attributes = test.length;//number of attributes
        Set<String> classNames = classCount.keySet();
        for (String className:classNames) {
            int currentCountForClass = classCount.get(className);
            for (int i = 0; i < attributes; i++) {
                double recent = getMatchedTupleCount(i,className,test);
                prob.put(className,prob.get(className)*(recent/currentCountForClass));
            }
        }
        double max = Double.MIN_VALUE;
        String winner = "";
        for (String s : prob.keySet()) {
            double value = prob.get(s);
            if(value>max){
                max = value;
                winner = s;
            }
        }
        System.out.println("CLASSIFIED TUPLE TO CLASS " + winner + " WITH PROB " + max);
        return winner;

    }

    public int getMatchedTupleCount(int i , String className,String[] test){
        int count = 0;
        for (String[] strings : data) {
            int last = strings.length-1;
            if(strings[i].equals(test[i]) && strings[last].equals(className)){
                count++;
            }
        }
        return count;
    }

    public void initClasses() {
        for (String[] strings : data) {
            String key = strings[strings.length-1];
            if(classCount.containsKey(key)) {
                int value = classCount.get(key);
                classCount.put(key,value+1);
            }else{
                classCount.put(key,1);
            }
        }
    }


    public ArrayList<String[]> loadData(String path) {
        ArrayList<String[]> data = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(path));
            String line;
            while(in.ready()) {
                line= in.readLine();
                data.add(line.split(" "));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}

