package pl.edu.agh.csg;

public interface Listener {
    Object notifyVm(Object source);
    Object notifyHost(Object source);
}
