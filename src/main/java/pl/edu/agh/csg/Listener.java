package pl.edu.agh.csg;

public interface Listener {
    Object notifyVmSize(Object source);
    Object notifygpuVmSize(Object source);
    Object notifyHostSimpleCnt(Object source);
    Object notifygpuHostCnt(Object source);
    Object notifyVmCnt (Object source);
    Object notifygpuVmCnt (Object source);
    Object notifyhostType(Object source);
    Object notifygpuhostType(Object source);
    Object notifyvmTuple(Object source);
    Object notifygpuvmTuple(Object source);
    Object notifyhostTuple(Object source);
    Object notifygpuhostTuple(Object source);
    Object notifyFilePath(Object source);

}
