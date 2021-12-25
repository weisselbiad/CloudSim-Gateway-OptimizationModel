package pl.edu.agh.csg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ListenerApp {


    /**
     * creating a List of interfaces to register listeners
     */
    List<Listener> listeners = new ArrayList<Listener>();
    public Object VmType;
    public Object hostCnt;
    public Object VmCnt;
    public Object hostType;

    SimProxy simulation;

   /* public void registerListener(Listener listener) {

        listeners.add(listener);
    }*/

    /**
     * notify all listeners and get objects from outside
     */

    /*public Object notifyAllListeners() {
        for (Listener listener: listeners) {
                Object VmType = listener.notify(this);
                  System.out.println(""+VmType);
        }return VmType ;

    }
    public String toString() {
        return "<ListenerApplication> instance"+"";
    }*/
    public SimProxy Init(){
        Lock lock=new ReentrantLock();
        Condition cond=lock.newCondition();
        //where you wait
        try{
            lock.lock();
            //check condition
            while (VmType == null || hostCnt == null || VmCnt == null || hostType == null)
            cond.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        //after you change it
        return simulation = new SimProxy("Sim1", VmCnt, VmType, hostCnt, hostType);
    }

   public Object notifyVmType(Listener listener) {
       return VmType = listener.notifyVmSize(this);
    }
   public Object notifyhostCnt(Listener listener){
        return hostCnt = listener.notifyHost(this);
   }
   public Object notifyVmCnt (Listener listener){
        return VmCnt = listener.notifyVmCnt(this);
   }
   public Object notifyhostType (Listener listener){return  hostType = listener.notifyhostType(this);}
    public SimProxy getSimulation(){
        return simulation;
    }
}
