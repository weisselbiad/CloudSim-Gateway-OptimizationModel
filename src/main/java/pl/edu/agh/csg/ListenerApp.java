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
    public Object vmTuple;

    public Object gpuvmTuple;
    public Object hosTuple;

    public Object gpuhosTuple;

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
            setVmTuple(obj.get("Vm"));
            setgpuVmTuple(obj.get("GpuVm"));
            setHostTuple(obj.get("Host"));
            setgpuHostTuple(obj.get("GpuHost"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //after you change it
        return simulation = new SimProxy3("Sim1", vmTuple, gpuvmTuple, hosTuple, gpuhosTuple);
    }

    /**
     * notify all listeners and get objects from outside
     */

    public Object notifyFilePath(Listener listener) {

        return FilePath = listener.notifyFilePath(this);
    }

    /**
     * set vm and host parameters array
     */
    public Object setVmTuple(Object obj){ return vmTuple = obj; }
    public Object setgpuVmTuple(Object obj){ return gpuvmTuple = obj; }
    public Object setHostTuple(Object obj){ return hosTuple = obj; }
    public Object setgpuHostTuple(Object obj){ return gpuhosTuple = obj; }

    public SimProxy3 getSimulation(){
        return simulation;
    }

}
