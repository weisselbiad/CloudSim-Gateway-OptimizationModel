package pl.edu.agh.csg;

import java.util.ArrayList;
import java.util.List;

public class ListenerApp {

    /**
     * creating a List of interfaces to register listeners
     */

    List<Listener> listeners = new ArrayList<Listener>();

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * notify all listeners and get objects from outside
     */

    public void notifyAllListeners() {
        for (Listener listener: listeners) {
                Object returnValue = listener.notify(this);
            System.out.println(returnValue);

        }
    }
}
