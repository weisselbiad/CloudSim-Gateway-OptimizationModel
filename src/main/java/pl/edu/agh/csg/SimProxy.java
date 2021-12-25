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
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmCost;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudbus.cloudsim.core.Identifiable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.util.*;

import static java.util.Comparator.comparingLong;

public class  SimProxy {

    public static final String SMALL = "S";
    public static final String MEDIUM = "M";
    public static final String LARGE = "L";

    /**
     * Initialize the Logger
     */

    private  final Logger logger = LoggerFactory.getLogger(SimProxy.class.getName());

    /**
     * Instantiate and declare needed variables
     */

    private String identifier;
    private int created = 0;

    /**
     * Instantiate the Setting Class which contain settings variables
     */

    public SimSettings settings = new SimSettings();

    /**
     * Class declaration of the Cloud Components and Simulation
     */

    private Object VmType ;
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

    private Object hostCnt ;
    private Object hostType;
    private long hostRam = settings.getHostRam();
    private long hostBw = settings.getHostBw();
    private long hostSize = settings.getHostSize();
    private int hostPes = settings.getHostPes();
    private long hostPeMips = settings.getHostPeMips();

    private Object vmCnt ;
    private long vmRam= settings.getVmRam();
    private long vmBw = settings.getVmBw();
    private long vmSize = settings.getVmSize();
    private long vmPes = settings.getVmPes();

    public SimProxy( String identifier,
                     Object vmCnt,
                     Object VmType,
                     Object hostCnt,
                     Object hostType){

        /**
         * Simulation identifier in case of instancing more than one simulation
         */

        this.identifier = identifier;
        this.vmCnt = vmCnt;
        this.VmType = VmType;
        this.hostCnt = hostCnt;
        this.hostType = hostType;

        /**
         * Initializing the Simulation, as parameter a double should be passed which is the minimum
         * time between events
         */

        this.simulation = new CloudSim(0.1);

        /**
         * Creating the Datacenter and calling the Broker
         */

        this.datacenter =  createDatacenter((Integer) this.hostCnt, (String) this.hostType);
        this.broker = new DatacenterBrokerSimple(this.simulation);

        /**
         * Creating a List of Virtual machines and Cloudlets
         */
        System.out.println(VmType);
        this.vmList = createVmList((Integer) this.vmCnt,(String) this.VmType);
        this.Cloudletlist = createCloudList();

        /***
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

    private DatacenterSimple createDatacenter(int hostCnt, String hostType) {
        final List<Host> hostList = new ArrayList<>(hostCnt);
        for(int i = 0; i <hostCnt; i++) {
            Host host = createHost(hostType);
            hostList.add(host);
        }
        final var datacenter = new DatacenterSimple(simulation, hostList);
        datacenter.setSchedulingInterval(10)
   // Those are monetary values. Consider any currency you want (such as Dollar)
                  .getCharacteristics()
                  .setCostPerSecond(0.01)
                  .setCostPerMem(0.02)
                  .setCostPerStorage(0.001)
                  .setCostPerBw(0.005);
        return datacenter;
    }

    /**
     * Host contain a List of Processing elements passed as a parameter to PeSimple which create basically a CPU
     * Uses VmSchedulerSpaceShared for VM scheduling.
     * @return Object Hostsimple
     */

    private Host createHost(String type) {

        int factor = getSizeFactor(type);

        final List<Pe> peList = new ArrayList<>(hostPes*factor);
        for (int i = 0; i < hostPes; i++) {
            peList.add(new PeSimple(hostPeMips));
        }
        final var vmScheduler = new VmSchedulerTimeShared();
        final var host = new HostSimple(hostRam*factor, hostBw*factor, hostSize*factor, peList);

        final var powerModel = new PowerModelHostSimple(50, 35);
        powerModel.setStartupDelay(5)
                .setShutDownDelay(3)
                .setStartupPower(5)
                .setShutDownPower(3);

        host.setVmScheduler(vmScheduler).setPowerModel(powerModel);
        host.enableUtilizationStats();

        return host;
    }

    /**
     * Creating a List of virtual machines with a randomly passed hardware configuration
     * @return List of Virtual machines
     */

    private List<Vm> createVmList(int vmCnt, String type) {

        int factor = getSizeFactor(type);

       final List<Vm> list = new ArrayList<>(vmCnt);

        for (int i = 0; i < vmCnt; i++) {

            final Vm vm = new VmSimple(hostPeMips,vmPes*factor);
            vm.setRam(vmRam*factor).setBw(vmBw*factor).setSize(vmSize*factor).enableUtilizationStats();

            list.add(vm);
        }
        return list;
    }

    /***
     * define the factor (Multiplier)
     * which identify the Size of the Vm
     * @return factor
     */
    private int getSizeFactor(String type){
        int factor;

        switch (type) {
            case MEDIUM:
                factor = 2;
                break;
            case LARGE:
                factor = 4;
                break;
            case SMALL:
            default:
                factor = 1;
        }
        return factor;

    }

    /**
     * Creating a List of cloudlets
     * UtilizationModel defining the Cloudlets use only 50% of any resource all the time
     * @return List of Cloudlets
    */

    private List<Cloudlet> createCloudList() {
        final List<Cloudlet> list = new ArrayList<>(cloudletCnt);
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.2);
        for (int i = 0; i < cloudletCnt; i++) {
            final Cloudlet cloudlet =
                    new CloudletSimple(cloudletLength,cloudletPes)
                            .setFileSize(1024)
                            .setOutputSize(1024)
                            .setUtilizationModelCpu(new UtilizationModelFull())
                            .setUtilizationModelRam(utilizationModel)
                            .setUtilizationModelBw(utilizationModel);
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
    public List<Integer> getVmCost(){
        final List<Integer> listcost = new ArrayList<Integer>();
       int ProcessingCost=0,MemoryCost=0,StorageCost=0,BwCost=0;
       int TotalCost=0,TotalExTime=0;

        for (int i = 0; i < this.vmList.size(); i++) {
            final VmCost cost = new VmCost(this.vmList.get(i));
            ProcessingCost += cost.getProcessingCost();
            MemoryCost += cost.getMemoryCost();
            StorageCost += cost.getStorageCost();
            BwCost += cost.getBwCost();
            TotalCost += cost.getTotalCost();
            int vmExtime= this.vmList.get(i).getTotalExecutionTime() > 0 ? 1 : 0;
            TotalExTime+=vmExtime;
         }
        System.out.println("Processing Cost: "+ProcessingCost);
        System.out.println("Memory Cost: "+MemoryCost);
        System.out.println("Storage Cost: "+StorageCost);
        System.out.println("Bw Cost: "+BwCost);

        System.out.println("Total Cost: "+TotalCost);
        listcost.add(TotalCost);
        System.out.println("Total Execution Time: "+TotalExTime);
        listcost.add(TotalExTime);

        return listcost ;
     }

    public double getmakespan(){
        double SD = this.datacenter.getShutdownTime();
        double ST = this.datacenter.getStartTime();
        System.out.println("ShutdownTime: "+ SD+ ", Start time:"+ ST);
        double makespan =  SD - ST;

        System.out.println("makespan: "+makespan);
        return makespan; }

    public double getPowerConsumption(){
        double PC = this.datacenter.getPowerModel().getPowerMeasurement().getTotalPower();
        System.out.println("Power Cosumption of DC: "+ PC +" Watts");
        return PC;
    }

    public CloudletsTableBuilder getTableBuilder(){
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        CloudletsTableBuilder table = new CloudletsTableBuilder(finishedCloudlets);
        return table;
    }

}
