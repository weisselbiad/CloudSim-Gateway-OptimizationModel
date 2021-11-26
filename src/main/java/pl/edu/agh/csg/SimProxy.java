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
    private  final Logger logger = LoggerFactory.getLogger(SimProxy.class.getName());

    private String identifier;
    private int created = 0;

    public SimSettings settings = new SimSettings();

    private List<Vm> vmList ;
    private  List<Cloudlet> Cloudletlist;
    private Datacenter datacenter;
    private DatacenterBroker broker;
    private final CloudSim simulation;

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
    Random rand = new Random();

    public SimProxy( String identifier
    ) {
        this.identifier = identifier;
        this.simulation = new CloudSim(0.1);

        this.datacenter =  createDatacenter();
        this.broker = new DatacenterBrokerSimple(this.simulation);

        this.vmList = createVmList();
        this.Cloudletlist = createCloudList();

        this.broker.submitVmList(this.vmList);
        this.broker.submitCloudletList(this.Cloudletlist);
        //this.simulation.start();

       info("Creating simulation: " + identifier);
    }

    public void runSim(){

        this.simulation.start();
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        }

    private DatacenterSimple createDatacenter() {
        final List<Host> hostList = new ArrayList<>(hostCnt);
        for(int i = 0; i < hostCnt; i++) {
            Host host = createHost();
            hostList.add(host);
        }
        //if (hostList == null){System.out.println("HostsList is Empty");}else System.out.println("HostsList is OK");
        //Uses a VmAllocationPolicySimple by default to allocate VMs
        return new DatacenterSimple(this.simulation, hostList);
    }
    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(hostPes);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < hostPes; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            ;
            peList.add(new PeSimple(hostPeMips));
        }
       // if (peList == null){System.out.println("peList is Empty");}else System.out.println("peList is OK");
        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(hostRam, hostBw, hostSize, peList);
    }
    private List<Vm> createVmList() {
        final List<Vm> list = new ArrayList<>(vmCnt);
        for (int i = 0; i < vmCnt; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final Vm vm = new VmSimple(hostPeMips,vmPes);
            vm.setRam(vmRam).setBw(vmBw).setSize(vmSize);
            list.add(vm);
        }
     //  if (list == null){System.out.println("VmList is Empty");}else System.out.println("VmList is OK");

        return list;
    }
    private List<Cloudlet> createCloudList() {
        final List<Cloudlet> list = new ArrayList<>(cloudletCnt);
        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);
        for (int i = 0; i < cloudletCnt; i++) {
            final Cloudlet cloudlet = new CloudletSimple(cloudletLength, cloudletPes, utilizationModel);
            cloudlet.setSizes(cloudletSize);
            list.add(cloudlet);
        }
       // if (list == null){System.out.println("CloudletList is Empty");}else System.out.println("CloudletList is OK");

        return list;
    }


   private void info(String message) {   logger.info(getIdentifier() + " " + message);    }

    public String getIdentifier() {
        return identifier;
    }
    public List<Vm> getVmList() { return this.vmList; }
    public DatacenterBroker getBroker() { return this.broker; }
    public Datacenter getDatacenter() { return this.datacenter; }
    public List<Cloudlet> getInputJobs() { return this.Cloudletlist; }

    public CloudletsTableBuilder getTableBuilder(){
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        CloudletsTableBuilder table = new CloudletsTableBuilder(finishedCloudlets);
        return table;
    }

}
