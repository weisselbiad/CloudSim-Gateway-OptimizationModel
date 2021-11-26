package pl.edu.agh.csg;

import org.cloudbus.cloudsim.core.CloudSim;
import py4j.GatewayServer;

public class Main {

       SimProxy simulation;
    public Main() {
        simulation = new SimProxy("Sim1");

        // simulation.runSim();
        //simulation.getTableBuilder();
    }
   public SimProxy getsimulation() { return this.simulation;  }

    public static void main(String[] args) throws Exception {
      //  SimProxy sim = new SimProxy("Sim1");
        GatewayServer gatewayServer = new GatewayServer(new Main());
        gatewayServer.start();
     System.out.println("Initialising Simulation ......");

    }

}

