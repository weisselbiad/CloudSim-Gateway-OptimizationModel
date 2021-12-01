package pl.edu.agh.csg;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SimProxy {

    /**
     * Initialize the Logger
     */

    private  final Logger logger = LoggerFactory.getLogger(SimProxy.class.getName());

    /**
     * Instantiate and declare needed variables
     */

    Random rand = new Random();
    private String identifier;
    private int created = 0;

    /**
     * Instantiate the Setting Class which contain settings variables
     */

    public SimSettings settings = new SimSettings();

    /**
     * Class declaration of the Cloud Components and Simulation
     */

    private List<Vm> vmList ;
    private  List<Cloudlet> Cloudletlist;
    private Datacenter datacenter;
    private DatacenterBroker broker;
    private final CloudSim simulation;

    /**
     * Initializing variables from the Setting Class
     */

    private int cloudletCnt = settings.getCloudletCnt();
    private int cloudletLength = settings.getCloudletLength();
    private long cloudletSize = settings.getCloudletSize();
    private int cloudletPes = settings.getCloudletPes();

    private int hostCnt = settings.getHostCnt();
    private long hostRam = settings.getHostRam();
    private long hostBw = settings.getHostBw();
    private long hostSize = settings.getHostSize();
    private int hostPes = settings.getHostPes();
    private long hostPeMips = settings.getHostPeMips();

    private int vmCnt = settings.getVmCnt();
    private long vmRam= settings.getVmRam();
    private long vmBw = settings.getVmBw();
    private long vmSize = settings.getVmSize();
    private long vmPes = settings.getVmPes();



    public SimProxy( String identifier
    ) {

        /**
         * Simulation identifier in case of instancing more than one simulation
         */

        this.identifier = identifier;

        /**
         * Initializing the Simulation, as parameter a double should be passed which is the minimum time between events
         */

        this.simulation = new CloudSim(0.1);

        /**
         * Creating the Datacenter and calling the Broker
         */

        this.datacenter =  createDatacenter();
        this.broker = new DatacenterBrokerSimple(this.simulation);

        /**
         * Creating a List of Virtual machines and Cloudlets
         */

        this.vmList = createVmList();
        this.Cloudletlist = createCloudList();

        /**
         * Submition of the Lists to the Broker
         */

        this.broker.submitVmList(this.vmList);
        this.broker.submitCloudletList(this.Cloudletlist);

       info("Creating simulation: " + identifier);
    }

    /**
     * Methode to run the Simulation used start it through sockets of Py4j
     * by need is a Table builder included to print the results
     */

    public void runSim(){

        this.simulation.start();
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        }

    /**
     * Creation of the Datacenter using a List of Hosts, which will be passed as a parameter
     * Uses a VmAllocationPolicySimple by default to allocate VMs
     *  @return Object Datacentersimple
     */

    private DatacenterSimple createDatacenter() {
        final List<Host> hostList = new ArrayList<>(hostCnt);
        for(int i = 0; i < hostCnt; i++) {
            Host host = createHost();
            hostList.add(host);
        }
        return new DatacenterSimple(this.simulation, hostList);
    }

    /**
     * Host contain a List of Processing elements passed as a parameter to PeSimple which create basically a CPU
     * Uses VmSchedulerSpaceShared for VM scheduling.
     * @return Object Hostsimple
     */

    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(hostPes);
        for (int i = 0; i < hostPes; i++) {
            peList.add(new PeSimple(hostPeMips));
        }
        return new HostSimple(hostRam,hostBw,hostSize, peList);
    }

    /**
     * Creating a List of virtual machines with a randomly passed hardware configuration
     * @return List of Virtual machines
     */

    private List<Vm> createVmList() {
        final List<Vm> list = new ArrayList<>(vmCnt);

        for (int i = 0; i < vmCnt; i++) {

            long RandVmPes = rand.nextLong(vmPes)+1;
            final Vm vm = new VmSimple(hostPeMips,RandVmPes);
            long RandvmRam = rand.nextLong(vmRam)+32;
            long RandvmBw = rand.nextLong(vmBw)+32;
            System.out.println("Ram: "+RandvmRam+" Bw: "+RandvmBw);
            vm.setRam(RandvmRam).setBw(RandvmBw).setSize(vmSize);

            list.add(vm);
        }
        return list;
    }

    /**
     * Creating a List of cloudlets
     * UtilizationModel defining the Cloudlets use only 50% of any resource all the time
     * @return List of Cloudlets
    */

    private List<Cloudlet> createCloudList() {
        final List<Cloudlet> list = new ArrayList<>(cloudletCnt);
        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);
        for (int i = 0; i < cloudletCnt; i++) {
            final Cloudlet cloudlet = new CloudletSimple(cloudletLength,cloudletPes, utilizationModel);
            cloudlet.setSizes(cloudletSize);
            list.add(cloudlet);
        }
        return list;
    }

    /**
     * Methode for Logger info
     * @param message
     */

    private void info(String message) { logger.info(getIdentifier() + " " + message);    }

    /**
     * List of geters. will be called by need from other classes
     * @return value
     */

    public String getIdentifier() { return identifier; }
    public List<Vm> getVmList() { return this.vmList; }
    public DatacenterBroker getBroker() { return this.broker; }
    public Datacenter getDatacenter() { return this.datacenter; }
    public List<Cloudlet> getCloudletlist() { return this.Cloudletlist; }

    public CloudletsTableBuilder getTableBuilder(){
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        CloudletsTableBuilder table = new CloudletsTableBuilder(finishedCloudlets);
        return table;
    }

}
