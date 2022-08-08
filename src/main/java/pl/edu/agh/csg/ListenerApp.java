package pl.edu.agh.csg;

import java.io.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ListenerApp {

    /**
     * creating a List of interfaces to register listeners
     */
    public Object FilePath;
    public Object indiv;
    JobsSet jobsSet;
    int NumofJobs;
    SimProxy3 simulation;

    /**
     * Init methode instantiate SimProxy, checking the Path value,
     * reading the json file and spliting the vm parameters array
     * from the host parameters array
     *
     * @return simulation
     */
    public SimProxy3 Init(){
        Lock lock=new ReentrantLock();
        Condition cond=lock.newCondition();
        Gson gson = new Gson();
        //where you wait
        try{
            lock.lock();
            //check condition
            while (FilePath == null)
            cond.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        try {
            String file = (String) FilePath;
            System.out.println("From Java Path : "+ file);
            BufferedReader br = new BufferedReader(new FileReader(file));
            JsonObject obj = gson.fromJson(br, JsonObject.class);
            setIndiv(obj.get("Indiv"));


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("First Cl in Seq1: "+jobsSet.getSeqList1().get(0)+ " first length: "+jobsSet.getSeqList1().get(0).get(0).getLength()+
                " First Cls in Seq2: "+jobsSet.getSeqList2().get(0)+ " first length: "+jobsSet.getSeqList2().get(0).get(0).getLength()+
                " First Cls in Seq3: "+jobsSet.getSeqList3().get(0)+" first length: "+jobsSet.getSeqList3().get(0).get(0).getLength()+
                " First Cls in Seq4: "+jobsSet.getSeqList4().get(0)+" first length: "+jobsSet.getSeqList4().get(0).get(0).getLength()+
                " First Cls in Seq5: "+jobsSet.getSeqList5().get(0)+" first length: "+jobsSet.getSeqList5().get(0).get(0).getLength()+
                "First Cls in Seq6: "+jobsSet.getSeqList6().get(0)+" first length: "+jobsSet.getSeqList6().get(0).get(0).getLength());
        //after you change it
        System.out.println("New Simulation Instance");
        return simulation = new SimProxy3("Sim1", indiv, jobsSet);

    }

 /*   public JobsSet getJobsSet(){
        return jobsSet;
    }*/
    /**
     * notify all listeners and get objects from outside
     */


    public Object notifyFilePath(Listener listener) {

        return FilePath = listener.notifyFilePath(this);
    }

    /**
     * set vm and host parameters array
     */
    public Object setIndiv(Object obj){ return indiv = obj; }
    public JobsSet setJobset(JobsSet jobset){return this.jobsSet = jobset;}
    public SimProxy3 getSimulation(){
        return simulation;
    }

}
