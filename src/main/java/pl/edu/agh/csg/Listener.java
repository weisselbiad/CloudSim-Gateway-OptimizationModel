package pl.edu.agh.csg;

public interface Listener {
    Object notifyVmSize(Object source);
    Object notifyHostSimpleCnt(Object source);
    Object notifyVmCnt (Object source);
    Object notifyhostType(Object source);
    Object notifyvmTuple(Object source);
    Object notifyhostTuple(Object source);
    Object notifyFilePath(Object source);

}
