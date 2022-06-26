package pl.edu.agh.csg;


import gpu.*;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.allocation.VideoCardAllocationPolicyNull;
import gpu.hardware_assisted.GridGpuVmAllocationPolicyBreadthFirst;
import gpu.hardware_assisted.GridVgpuSchedulerFairShare;
import gpu.hardware_assisted.GridVgpuTags;
import gpu.hardware_assisted.GridVideoCardTags;
import gpu.provisioners.*;
import gpu.selection.PgpuSelectionPolicy;
import gpu.selection.PgpuSelectionPolicyNull;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.resources.SanStorage;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A minimal but organized, structured and re-usable CloudSim Plus example
 * which shows good coding practices for creating simulation scenarios.
 *
 * <p>It defines a set of constants that enables a developer
 * to change the number of Hosts, VMs and Cloudlets to create
 * and the number of {@link Pe}s for Hosts, VMs and Cloudlets.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class GpuExperimentFirst {
    private static final int HOSTS = 1;
    private static final int HOST_PES = 8;
    private static final int GPUHOSTSV3 = 1;
    private static final int GPUHOSTSV4 = 5;


    private static final int VMS = 2;
    private static final int GPUVMS = 176;
    private static final int GPUVMS140 =0;
    private static final int GPUVMS240 =0;
    private static final int GPUVMS160 =0;
    private static final int GPUVMS260 =0;
    private static final int GPUVMS180 =10;
    private static final int GPUVMS280 = 10;
    private static final int VM_PES = 4;

    private static final int CLOUDLETS = 10;
    private static final int GPUCLOUDLETS =500;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10000;

    private int lastVmIndex;
    private int lastHostIndex;

    public final static String DUAL_INTEL_XEON_E5_2620_V3 = "Dual Intel Xeon E5-2620 v3 (12 Cores, 2.40 GHz, 1 x NVIDIA GRID K1)";
    public final static String DUAL_INTEL_XEON_E5_2690_V4 = "Dual Intel Xeon E5-2690 v4 (28 Cores, 2.60 GHz, 1 x NVIDIA GRID K2)";

    private final CloudSim simulation;
    private DatacenterBroker broker0;

    private List<Vm> vmList;
    private List<GpuVm> gpuvmList;
    private List<CloudletSimple> cloudletList;
    private List<GpuCloudlet> gpucloudletList;
    private Datacenter datacenter0;

    public int getClassID (Cloudlet cl){
        if(cl.getClass().toString() == "org.cloudbus.cloudsim.cloudlets.CloudletSimple"){
            return 0;
        }else if(cl.getClass().toString() == "cloudsimMixedPeEnv.GpuCloudlet"){
            return 1;
        }else
            return -1;
    }
    public static void main(String[] args) {
        new GpuExperimentFirst();
    }

    private GpuExperimentFirst() throws Exception {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        simulation = new CloudSim();
        datacenter0 = createDatacenter();

        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        GpuDatacenter datacenter = createDatacenter(simulation,"Datacenter");
        GpuDatacenterBroker broker = createGpuBroker(simulation,"Broker");


        gpuvmList = createGpuVms();


        gpucloudletList = createGpuCloudlet((int)broker0.getId());

        broker0.submitVmList(gpuvmList);

        broker0.submitCloudletList(gpucloudletList);

        simulation.start();

        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        new org.cloudsimplus.builders.tables.CloudletsTableBuilder(finishedCloudlets).build();

    }
    private static GpuDatacenterBroker createGpuBroker(CloudSim simulation,String name) {
        GpuDatacenterBroker broker = null;
        try {
            broker = new GpuDatacenterBroker(simulation,name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private Datacenter createDatacenter() {
        final List<GpuHost> hostList = new ArrayList<>();

        for (int j = 0; j < GPUHOSTSV3; j++){
            GpuHost gpuhost = createGpuHost(DUAL_INTEL_XEON_E5_2620_V3);
            hostList.add(gpuhost);
        }
        for (int j = 0; j < GPUHOSTSV4; j++){
            GpuHost gpuhost = createGpuHost(DUAL_INTEL_XEON_E5_2690_V4);
            hostList.add(gpuhost);
        }



        //Uses a VmAllocationPolicySimple by default to allocate VMs
        final var datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        datacenter.setSchedulingInterval(10)
                // Those are monetary values. Consider any currency you want (such as Dollar)
                .getCharacteristics()
                .setCostPerSecond(0.01)
                .setCostPerMem(0.02)
                .setCostPerStorage(0.001)
                .setCostPerBw(0.005);
        return datacenter;
    }
    private static GpuDatacenter createGpuDatacenter(String name) {
        // We need to create a list to store our machines
        List<GpuHost> hostList = new ArrayList<GpuHost>();
        // Number of host's video cards
        int numVideoCards = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_VIDEO_CARDS;
        // To hold video cards
        List<VideoCard> videoCards = new ArrayList<VideoCard>(numVideoCards);
        for (int videoCardId = 0; videoCardId < numVideoCards; videoCardId++) {
            List<Pgpu> pgpus = new ArrayList<Pgpu>();
            // Adding an NVIDIA K1 Card
            double mips = GridVideoCardTags.NVIDIA_K1_CARD_PE_MIPS;
            int gddram = GridVideoCardTags.NVIDIA_K1_CARD_GPU_MEM;
            long bw = GridVideoCardTags.NVIDIA_K1_CARD_BW_PER_BUS;
            for (int pgpuId = 0; pgpuId < GridVideoCardTags.NVIDIA_K1_CARD_GPUS; pgpuId++) {
                List<GpuPe> pes = new ArrayList<GpuPe>();
                for (int peId = 0; peId < GridVideoCardTags.NVIDIA_K1_CARD_GPU_PES; peId++) {
                    pes.add(new GpuPe( peId, new GpuPeProvisionerSimple(mips)));
                }
                pgpus.add(new Pgpu(pgpuId, GridVideoCardTags.NVIDIA_K1_GPU_TYPE, pes,
                        new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw)));
            }
            // Pgpu selection policy
            PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyNull();
            // Vgpu Scheduler
            VgpuScheduler vgpuScheduler = new GridVgpuSchedulerFairShare(GridVideoCardTags.NVIDIA_K1_CARD, pgpus,
                    pgpuSelectionPolicy);
            // PCI Express Bus Bw Provisioner
            VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(BusTags.PCI_E_3_X16_BW);
            // Create a video card
            VideoCard videoCard = new VideoCard(videoCardId, GridVideoCardTags.NVIDIA_K1_CARD, vgpuScheduler,
                    videoCardBwProvisioner);
            videoCards.add(videoCard);
        }

        // Create a host
        int hostId = 0;

        // A Machine contains one or more PEs or CPUs/Cores.
        List<GpuPe> peList = new ArrayList<GpuPe>();

        // PE's MIPS power
        double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

        for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_PES; peId++) {
            // Create PEs and add these into a list.
            peList.add(new GpuPe(0, new GpuPeProvisionerSimple(mips)));
        }

        // Create Host with its id and list of PEs and add them to the list of machines
        // host memory (MB)
        int ram = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_RAM;
        // host storage
        long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
        // host BW
        int bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW;
        // Set VM Scheduler
        VmScheduler vmScheduler = new VmSchedulerTimeShared(peList);
        // Video Card Selection Policy
        VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyNull(videoCards);
        GpuHost newHost = new GpuHost(hostId, GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3, new RamProvisionerSimple(ram),
                new BwProvisionerSimple(Long.MAX_VALUE), storage, peList, vmScheduler, videoCardAllocationPolicy);
        hostList.add(newHost);

        // Create a DatacenterCharacteristics object that stores the
        // properties of a data center: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone
        // and its price (G$/Pe time unit).
        // system architecture
        String arch = "x86";
        // operating system
        String os = "Linux";
        // VM Manager
        String vmm = "Horizen";
        // time zone this resource located (Tehran)
        double time_zone = +3.5;
        // the cost of using processing in this resource
        double cost = 0.0;
        // the cost of using memory in this resource
        double costPerMem = 0.00;
        // the cost of using storage in this resource
        double costPerStorage = 0.000;
        // the cost of using bw in this resource
        double costPerBw = 0.0;
        // we are not adding SAN devices by now
        LinkedList<SanStorage> storageList = new LinkedList<SanStorage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
                cost, costPerMem, costPerStorage, costPerBw);

        // We need to create a Datacenter object.
        GpuDatacenter datacenter = null;
        try {
            datacenter = new GpuDatacenter(name, characteristics, new GridGpuVmAllocationPolicyBreadthFirst(hostList),
                    storageList, schedulingInterval);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }



    /**
     * Creates a list of VMs.
     */

    private List<GpuVm> createGpuVms() {
        final List<GpuVm> gpuvmlist = new ArrayList<>();
        // Create GpuTask Scheduler
        GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();

        for (int i = 0; i < GPUVMS140 ; i++) {

            GpuVm vm = createGpuVm(GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS,2, 4, GridVgpuTags.getK140Q(simulation,i, gpuTaskScheduler));
            vm.setId(i);
            lastVmIndex = i+1;
            gpuvmlist.add( vm);

        }
        //int j0 = (int)gpuvmlist.get(gpuvmlist.size()).getId() +1;
        for (int i = 0 ; i < GPUVMS240  ; i++) {
            GpuVm vm = createGpuVm((long)GpuHostTags.DUAL_INTEL_XEON_E5_2690_V4_PE_MIPS,2, 4, GridVgpuTags.getK240Q(simulation,i, gpuTaskScheduler));
            vm.setId(lastVmIndex);
            lastVmIndex = ++lastVmIndex ;
            gpuvmlist.add( vm);
        }
        //int lastindex = (int)gpuvmlist.get(gpuvmlist.size()).getId() ;
        for (int i = 0; i < GPUVMS160 ; i++) {

            GpuVm vm = createGpuVm(GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS,2, 8, GridVgpuTags.getK160Q(simulation,i, gpuTaskScheduler));
            vm.setId(lastVmIndex);
            lastVmIndex = ++lastVmIndex ;
            gpuvmlist.add( vm);

        }
        for (int i = 0 ; i < GPUVMS260 +1 ; i++) {
            GpuVm vm = createGpuVm((long)GpuHostTags.DUAL_INTEL_XEON_E5_2690_V4_PE_MIPS,4, 8, GridVgpuTags.getK260Q(simulation,i, gpuTaskScheduler));
            vm.setId(lastVmIndex);
            lastVmIndex = ++lastVmIndex ;
            gpuvmlist.add( vm);
        }
        for (int i = 0; i < GPUVMS180 ; i++) {

            GpuVm vm = createGpuVm(GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS,4, 16, GridVgpuTags.getK180Q(simulation,i, gpuTaskScheduler));
            vm.setId(lastVmIndex);
            lastVmIndex = ++lastVmIndex ;
            gpuvmlist.add( vm);

        }
        for (int i = 0 ; i < GPUVMS280  ; i++) {
            GpuVm vm = createGpuVm((long)GpuHostTags.DUAL_INTEL_XEON_E5_2690_V4_PE_MIPS,8, 16, GridVgpuTags.getK280Q(simulation,i, gpuTaskScheduler));
            vm.setId(lastVmIndex);
            lastVmIndex = ++lastVmIndex ;
            gpuvmlist.add( vm);
        }
        return gpuvmlist;
    }

    private GpuVm createGpuVm( long mips, long cores, long ram, Vgpu vgpu){

        // image size (GB)
        int size = 10;
        long bw = 100;
        String vmm = "vSphere";
        GpuCloudletSchedulerTimeShared GCSTS = new GpuCloudletSchedulerTimeShared();
        CloudletSchedulerSpaceShared GCSSS = new CloudletSchedulerSpaceShared();

        // Create a VM
        GpuVm vm = new GpuVm( mips, cores, GCSSS);
        vm.setRam(ram);
        vm.setBw(bw);
        vm.setSize(size);

        // Create a Vgpu
        //Vgpu vgpu = GridVgpuTags.getK180Q(simulation,id, gpuTaskScheduler);
        vm.setVgpu(vgpu);

        return vm;
    }
    /**
     * Creates a list of Cloudlets.
     */
    private List<GpuCloudlet> createGpuCloudlet( int brokerId) {
        final List<GpuCloudlet> list = new ArrayList<>();

        // Cloudlet properties
        long length = (long) (400 * GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS);
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel cpuUtilizationModel = new UtilizationModelFull();
        UtilizationModel ramUtilizationModel = new UtilizationModelFull();
        UtilizationModel bwUtilizationModel = new UtilizationModelFull();

        // GpuTask properties
        long taskLength = (long) (GridVideoCardTags.NVIDIA_K1_CARD_PE_MIPS * 150);
        long taskInputSize = 128;
        long taskOutputSize = 128;
        long requestedGddramSize = 4 * 1024;
        int numberOfBlocks = 2;

        UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
        UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
        UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();

        for (int j = 0; j < GPUCLOUDLETS; j++) {

            GpuTask gpuTask = new GpuTask(simulation, j, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
                    requestedGddramSize, 0, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);

            GpuCloudlet gpuCloudlet = new GpuCloudlet(length, pesNumber,fileSize, outputSize,cpuUtilizationModel,ramUtilizationModel,bwUtilizationModel, gpuTask);
            gpuCloudlet.setId(j);
            gpuCloudlet.setUserId(brokerId);
            list.add(gpuCloudlet);
        }

        return list;
    }




}

