package pl.edu.agh.csg;

public interface Listener {
    Object notifyVmSize(Object source);
    Object notifyHost(Object source);
    Object notifyVmCnt (Object source);
    Object notifyhostType(Object source);
}
