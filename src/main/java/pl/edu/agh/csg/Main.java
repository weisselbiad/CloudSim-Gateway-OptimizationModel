package pl.edu.agh.csg;

import py4j.GatewayServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    /**
     * Initializing SimProxy which build the Simulation and ListenerApp which
     * contain Methode that could be implemented from outside
     * using the Listener interface
     */
    ListenerApp listenerApp ;


    public Main() {

        listenerApp = new ListenerApp();

    }

    /**
     * returning simulation and ListenerApp so it is possible to call there
     * Methodes using the Gateway Py4j
     * @return simulation and ListenerApp
     */

    public SimProxy getsimulation() {return this.listenerApp.getSimulation();}
    public ListenerApp getListenerApp() {return  this.listenerApp;}
    public static void main(String[] args) {

        /**
         * passing the constructor of the main class as parameter of the GatewayServer
         * so all Objects and Methods would be visible from outside and will be possible
         * to call them using the Py4j Gateway
         * Then starting the gateway
         */

        GatewayServer gatewayServer = new GatewayServer(new Main());
        gatewayServer.start(true);
     System.out.println("Initialising Simulation ......");
    }
}

