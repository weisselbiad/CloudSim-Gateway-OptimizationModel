package pl.edu.agh.csg;

import cloudsimMixedPeEnv.*;

import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicy;
import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicyBreadthFirst;
import cloudsimMixedPeEnv.hardware_assisted.GridPerformanceVgpuSchedulerFairShare;
import cloudsimMixedPeEnv.hardware_assisted.GridVgpuTags;
import cloudsimMixedPeEnv.hardware_assisted.GridVideoCardTags;
import cloudsimMixedPeEnv.hardware_assisted.VideoCardPowerModelNvidiaGridK1;
import cloudsimMixedPeEnv.performance.models.PerformanceModel;
import cloudsimMixedPeEnv.performance.models.PerformanceModelGpuConstant;
import cloudsimMixedPeEnv.power.PowerVideoCard;
import cloudsimMixedPeEnv.power.models.VideoCardPowerModel;
import cloudsimMixedPeEnv.provisioners.GpuBwProvisionerShared;
import cloudsimMixedPeEnv.provisioners.GpuGddramProvisionerSimple;
import cloudsimMixedPeEnv.provisioners.VideoCardBwProvisioner;
import cloudsimMixedPeEnv.provisioners.VideoCardBwProvisionerShared;
import cloudsimMixedPeEnv.selection.PgpuSelectionPolicy;
import cloudsimMixedPeEnv.selection.PgpuSelectionPolicyNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationBestFitStaticThreshold;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationStaticThreshold;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.hosts.HostStateHistoryEntry;
import org.cloudbus.cloudsim.power.models.PowerModelHostSpec;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import cloudsimMixedPeEnv.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.selectionpolicies.VmSelectionPolicyMinimumUtilization;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmCost;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.vms.VmStateHistoryEntry;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.HostHistoryTableBuilder;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.listeners.VmHostEventInfo;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.util.*;

public class  SimProxy2 {

    public static final int SMALL = 1;
    public static final int MEDIUM = 2;
    public static final int LARGE = 3;
    Gson gson = new GsonBuilder().create();

    /**
     * Initialize the Logger
     */

    //  private  final Logger logger = LoggerFactory.getLogger(SimProxy.class.getName());

    /**
     * Instantiate and declare needed variables
     */

    private String identifier;
    private int created = 0;

    /**
     * Instantiate the Setting Class which contain settings variables
     */
    private int lastVmIndex;
    private int lastHostIndex;
    private int lastCloudletindex;
    private int lastvgpuid;

    public SimSettings settings = new SimSettings();

    /**
     * Class declaration of the Cloud Components and Simulation
     */

    private Object VmType ;
    private List<Vm> vmList ;
    private List<GpuVm> gpuvmList;
    private List<Vm> CPUfirstvmList ;
    private List<GpuVm> GPUfisrtgpuvmList;

    private  List<Cloudlet> Cloudletlist;
    private List<Cloudlet> CPUfirtCloudleList;
    private List<GpuCloudlet> GPUfirtCloudleList;
    private List<GpuCloudlet> gpucloudletList;
    List<Cloudlet> submittedCls;
    private Datacenter datacenter;
    private MixedDatacenterBroker broker;
    private Vm sourceVm ;
    private List<Vm> sourceVmList = new ArrayList<>();
    private final CloudSim simulation;

    private Object hostTuple;
    private Object GpuhostTuple;
    private Object vmTuple;
    private Object GpuvmTuple;


    /**
     * Initializing variables from the Setting Class
     */

    private int cloudletCnt = settings.getCloudletCnt();
    private  int gpuclouletCnt = settings.getGpucloudletCnt();
    private int cloudletLength = settings.getCloudletLength();
    private long cloudletSize = settings.getCloudletSize();
    private int cloudletPes = settings.getCloudletPes();

    private long hostRam = settings.getHostRam();
    private long hostBw = settings.getHostBw();
    private long hostSize = settings.getHostSize();
    private int hostPes = settings.getHostPes();
    private long hostPeMips = settings.getHostPeMips();

    private long gpuMips = settings.getGpumips();


    private Object vmCnt ;
    private long vmRam= settings.getVmRam();

    private long gpuvmRam = settings.getGpuvmram();
    private long vmBw = settings.getVmBw();

    private long gpuvmBw = settings.getGpuvmbw();
    private long vmSize = settings.getVmSize();

    private int gpuvmSize = settings.getGpuvmsize();
    private long vmPes = settings.getVmPes();

    List<Double> power = new ArrayList<>(Arrays.asList(93.7, 97.0, 101.0, 105.0, 110.0, 116.0, 121.0, 125.0, 129.0, 133.0, 135.0));

    private List<Double> getPower(int factor){
        List<Double> power;

        switch (factor) {
            case MEDIUM:
                power = new ArrayList<>(Arrays.asList(93.7*2, 97.0*2, 101.0*2, 105.0*2, 110.0*2, 116.0*2, 121.0*2, 125.0*2, 129.0*2, 133.0*2, 135.0*2));
                break;
            case LARGE:
                power = new ArrayList<>(Arrays.asList(93.7*3, 97.0*3, 101.0*3, 105.0*3, 110.0*3, 116.0*3, 121.0*3, 125.0*3, 129.0*3, 133.0*3, 135.0*3));
                break;
            case SMALL:
            default:
                power = new ArrayList<>(Arrays.asList(93.7, 97.0, 101.0, 105.0, 110.0, 116.0, 121.0, 125.0, 129.0, 133.0, 135.0));
        }
        return power;

    }
    private VmAllocationPolicyMigrationStaticThreshold allocationPolicy;
    private static final double HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.7;
    private static final double HOST_UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.1;
    private static final int    VM_MIPS = 1000; //for each PE
    private boolean migrationRequested;
    private int migrationsNumber;
    /**
     * The time to request the destruction of some VM (in seconds).
     * Since we can't control how the simulation clock advances,
     * the VM destruction may not be requested exactly at this time.
     */
    private static double TIME_TO_DESTROY_VM;
    private boolean vmDestructionRequested;

    private final ContinuousDistribution random;
    private static final double INTERVAL = 5;
    Vm vmcopy;
    long temp;
    public SimProxy2(String identifier,
                    Object vmTuple,
                    Object GpuvmTuple,
                    Object hostTuple,
                    Object GpuhostTuple){

        /**
         * Simulation identifier in case of instancing more than one simulation
         */

        this.identifier = identifier;
        this.vmTuple = vmTuple;
        this.hostTuple = hostTuple;
        this.GpuhostTuple = GpuhostTuple;
        this.GpuvmTuple = GpuvmTuple;

        /**
         * Initializing the Simulation, as parameter a double should be passed which is the minimum
         * time between events
         */

        this.simulation = new CloudSim(0.1);
        random = new UniformDistr();
        /**
         * Creating the Datacenter and calling the Broker
         */

        this.datacenter =  createDatacenter (toArray( (JsonArray) this.hostTuple),  toArray ((JsonArray) this.GpuhostTuple) ) ;
        this.broker = new MixedDatacenterBroker(this.simulation);

        /**
         * Creating a List of Virtual machines and Cloudlets
         */

        System.out.println("vm Tuple from Proxy: "+this.vmTuple+ " GpuVm Tuple from Proxy: "+this.GpuvmTuple);
        int[][] ForvmTuple=toArray( (JsonArray) this.vmTuple);
        this.vmList = createVmList( toArray( (JsonArray) this.vmTuple));
        this.gpuvmList = createGpuVmList( toArray( (JsonArray) this.GpuvmTuple));
        int[][] arr = { { 1, 2}, { 2, 2 },  { 3, 2 } };
        this.CPUfirstvmList = createVmList(arr);
        this.GPUfisrtgpuvmList = createGpuVmList( arr);

        this.Cloudletlist = createCloudList(cloudletCnt);
        this.gpucloudletList = createGpuCloudletList(1,(int)this.broker.getId(),gpuclouletCnt);
        this.CPUfirtCloudleList =createAndbindCPUfirstCloudlets(CPUfirstvmList);
        this.GPUfirtCloudleList= createAndbindGPUfirstCloudlets(GPUfisrtgpuvmList);

        /***
         * Submition of the Lists to the Broker
         */
        this.vmList.addAll(this.CPUfirstvmList);
        this.vmList.addAll(this.GPUfisrtgpuvmList);
        this.vmList.addAll(this.gpuvmList);
        this.broker.submitVmList(this.vmList);

        this.Cloudletlist.addAll(this.CPUfirtCloudleList);
        this.Cloudletlist.addAll(this.GPUfirtCloudleList);
        this.Cloudletlist.addAll(this.gpucloudletList);
        this.broker.submitCloudletList(this.Cloudletlist);

//      this.broker.addOnVmsCreatedListener(this::onVmsCreatedListener);

        //info("Creating simulation: " + identifier);
    }

    /**
     * Methode to run the Simulation used start it through sockets of Py4j
     * by need is a Table builder included to print the results
     */

    public void runSim(){
        this.simulation.start();

        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        finishedCloudlets.sort(
                Comparator.comparingLong((Cloudlet c) -> c.getVm().getHost().getId())
                        .thenComparingLong(c -> c.getVm().getId()));
        // final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        new org.cloudsimplus.builders.tables.CloudletsTableBuilder(finishedCloudlets).build();
        System.out.printf("%nHosts CPU usage History (when the allocated MIPS is lower than the requested, it is due to VM migration overhead)%n");

        //this.datacenter.getHostList().forEach(this::printHostHistory);
        System.out.printf("Number of VM migrations: %d%n", migrationsNumber);
        System.out.println(getClass().getSimpleName() + " finished!");
    }
    public int[][] toArray(JsonArray obj){
        int[][] intArray = gson.fromJson(obj, int[][].class);
        return intArray;
    }

    /**
     * Creation of the Datacenter using a List of Hosts, which will be passed as a parameter
     * Uses a VmAllocationPolicySimple by d-efault to allocate VMs
     *  @return Object Datacentersimple
     * @param hostTuple
     */

    private Datacenter createDatacenter(int[][] hostTuple, int[][] gpuhostTuple) {
        this.hostTuple = hostTuple;
        final List<Host> hostList = new ArrayList<>();

        System.out.println("Hlength: "+hostTuple.length);
        for (int i = 0; i< hostTuple.length; i++ ){
            for(int j = 0; j < hostTuple[i][1]; j++) {
                System.out.println("print j : "+ j+ " and HostCnt: "+hostTuple[i][1]+ " and Host Size: "+hostTuple[i][0]);
                Host host = createHost(hostTuple[i][0]);
                System.out.println(host.getClass()+" "+ host);
                hostList.add(host);
            }}

        for (int i = 0; i< gpuhostTuple.length; i++ ){
            for(int j = 0; j < gpuhostTuple[i][1]; j++) {
                System.out.println("print j : " + j + " and GpuHostCnt: " + hostTuple[i][1] + " and GpuHost Size: " + hostTuple[i][0]);
                Host gpuhost = createGpuHost(gpuhostTuple[i][0]);
                System.out.println(gpuhost.getClass()+" "+gpuhost);
                hostList.add(gpuhost);
            }}
        this.allocationPolicy =
                new VmAllocationPolicyMigrationBestFitStaticThreshold(
                        new VmSelectionPolicyMinimumUtilization(),
                        HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION + 0.2);
        this.allocationPolicy.setUnderUtilizationThreshold(HOST_UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);
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

    /**
     * Host contain a List of Processing elements passed as a parameter to PeSimple which create basically a CPU
     * Uses VmSchedulerSpaceShared for VM scheduling.
     * @return Object Hostsimple
     */

    private Host createHost(int type) {

        int factor = getSizeFactor(type);

        final List<Pe> peList = new ArrayList<>(hostPes*factor);
        for (int i = 0; i < hostPes; i++) {
            peList.add(new PeSimple(hostPeMips));
        }
        final var vmScheduler = new VmSchedulerTimeShared();
        final var host = new HostSimple(hostRam*factor, hostBw*factor, hostSize*factor, peList);


        final var powerModel = new PowerModelHostSpec(getPower(type));
        powerModel.setStartupDelay(5)
                .setShutDownDelay(3)
                .setStartupPower(5)
                .setShutDownPower(3);

        host.setVmScheduler(vmScheduler).setPowerModel(powerModel);
        host.enableUtilizationStats();
        host.setId(lastHostIndex);
        host.enableStateHistory();
        lastHostIndex = ++lastHostIndex ;
        return host;
    }//Different from the FirstFit policy, it always increments the host index.

    private GpuHost createGpuHost (int type){

        int factor = getSizeFactor(type);

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
                List<Pe> pes = new ArrayList<Pe>();
                for (int peId = 0; peId < GridVideoCardTags.NVIDIA_K1_CARD_GPU_PES; peId++) {
                    pes.add(peId, new PeSimple(mips));
                }
                pgpus.add(new Pgpu(pgpuId, GridVideoCardTags.NVIDIA_K1_GPU_TYPE, pes,
                        new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw)));
            }
            // Pgpu selection policy
            PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyNull();
            double performanceLoss = 0.1;
            PerformanceModel<VgpuScheduler, Vgpu> performanceModel = new PerformanceModelGpuConstant(performanceLoss);
            // Vgpu Scheduler
            GridPerformanceVgpuSchedulerFairShare vgpuScheduler = new GridPerformanceVgpuSchedulerFairShare(
                    GridVideoCardTags.NVIDIA_K1_CARD, pgpus, pgpuSelectionPolicy, performanceModel);
            // PCI Express Bus Bw Provisioner
            VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(BusTags.PCI_E_3_X16_BW);
            // Video Card Power Model
            VideoCardPowerModel videoCardPowerModel = new VideoCardPowerModelNvidiaGridK1(false);
            // Create a video card
            VideoCard videoCard = new PowerVideoCard(videoCardId, GridVideoCardTags.NVIDIA_K1_CARD, vgpuScheduler,
                    videoCardBwProvisioner,videoCardPowerModel);
            videoCards.add(videoCard);
        }

        // A Machine contains one or more PEs or CPUs/Cores.
        List<Pe> peList = new ArrayList<Pe>();

        // PE's MIPS power
        double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

        for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_PES*factor; peId++) {
            // Create PEs and add these into a list.
            peList.add(0, new PeSimple(mips));
        }

        // Create Host with its id and list of PEs and add them to the list of machines
        // host memory (MB)
        long ram = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_RAM ;
        // host storage
        long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
        // host BW
        long bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW ;
        // Set VM Scheduler
        VmScheduler vmScheduler = new VmSchedulerSpaceShared();

        // Video Card Selection Policy
        VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyBreadthFirst(videoCards);
        GpuHost gpuhost =new GpuHost(ram*factor,bw*factor, storage*factor, peList,vmScheduler, videoCardAllocationPolicy);
        gpuhost.setId(lastHostIndex);
        gpuhost.enableStateHistory();
        lastHostIndex = ++lastHostIndex ;
        return gpuhost;
    }

    /**
     * Creating a List of virtual machines with a randomly passed hardware configuration
     * @return List of Virtual machines
     * @param vmTuple
     */

    private List<Vm> createVmList(int[][] vmTuple) {
        this.vmTuple = vmTuple;
        final List<Vm> list = new ArrayList<>();
        for (int i = 0; i < vmTuple.length; i++) {
            int factor = getSizeFactor(vmTuple[i][0]);

            for (int j = 0; j < vmTuple[i][1]; j++) {

                final Vm vm = new VmSimple(hostPeMips,vmPes*factor);
                vm.setRam(vmRam*factor).setBw(vmBw*factor).setSize(vmSize*factor).enableUtilizationStats();
                vm.setId(lastVmIndex);
                lastVmIndex = ++lastVmIndex ;

                list.add(vm);
            }}
        return list;
    }
    private List<GpuVm> createGpuVmList(int[][] gpuvmTuple) {
        this.GpuvmTuple = gpuvmTuple;
        final List<GpuVm> gpuvmlist = new ArrayList<>();

        for (int i = 0; i < gpuvmTuple.length; i++) {
            int factor = getSizeFactor(gpuvmTuple[i][0]);
            for (int j = 0; j < gpuvmTuple[i][1] ; j++) {

                String vmm = "vSphere";
                //GpuCloudletSchedulerTimeShared GCSTS = new GpuCloudletSchedulerTimeShared();

                // Create a VM
                GpuVm vm = new GpuVm( gpuMips, vmPes*factor, new CloudletSchedulerTimeShared());
                vm.setRam(gpuvmRam*factor);
                vm.setBw(gpuvmBw*factor);
                vm.setSize(gpuvmSize*factor);
                // Create GpuTask Scheduler
                GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
                // Create a Vgpu
                Vgpu vgpu = GridVgpuTags.getK120Q(simulation,j, gpuTaskScheduler);
                vm.setVgpu(vgpu);
                vm.setId(lastVmIndex);
                lastVmIndex = ++lastVmIndex ;
                gpuvmlist.add(vm);

            }}
        return gpuvmlist;
    }
    /***
     * define the factor (Multiplier)
     * which identify the Size of the Vm
     * @return factor
     */
    private int getSizeFactor(int type){
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

    private List<Cloudlet> createCloudList(int numofCls) {
        final List<Cloudlet> list = new ArrayList<>();
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.2);
        for (int i = 0; i < numofCls; i++) {
            final Cloudlet cloudlet =
                    new CloudletSimple(cloudletLength,cloudletPes)
                            .setFileSize(1024)
                            .setOutputSize(1024)
                            .setUtilizationModelCpu(new UtilizationModelFull())
                            .setUtilizationModelRam(utilizationModel)
                            .setUtilizationModelBw(utilizationModel);
            cloudlet.setSizes(cloudletSize);
            cloudlet.setId(lastCloudletindex);
            lastCloudletindex = ++lastCloudletindex;
            list.add(cloudlet);

        }
        return list;
    }

    private List<GpuCloudlet> createGpuCloudletList(int gpuTaskId, int brokerId, int numofCls) {
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

        for (int j = 0; j < numofCls; j++) {

            GpuTask gpuTask = new GpuTask(simulation, gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
                    requestedGddramSize, 0, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);

            GpuCloudlet gpuCloudlet = new GpuCloudlet(length, pesNumber,fileSize, outputSize,cpuUtilizationModel,ramUtilizationModel,bwUtilizationModel, gpuTask);
            gpuCloudlet.setId(lastCloudletindex);
            lastCloudletindex = ++lastCloudletindex;
            gpuCloudlet.setUserId(brokerId);
            list.add(gpuCloudlet);
        }

        return list;
    }


    /**
     * List of geters. will be called by need from other classes
     * @return value
     */

    public String getIdentifier() { return identifier; }
    public List<Vm> getVmList() { return this.vmList; }
    public MixedDatacenterBroker getBroker() { return this.broker; }
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
        System.out.println("ShutdownTime: "+ SD+ ", Start time: "+ ST);
        double makespan =  SD - ST;

        System.out.println("makespan: "+makespan);
        return makespan; }

    public double getPowerConsumption(){
        double PC = this.datacenter.getPowerModel().getPowerMeasurement().getTotalPower();
        System.out.println("Power Cosumption of DC: "+ (int)PC +" Watts");
        return PC;
    }

    public CloudletsTableBuilder getTableBuilder(){
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        CloudletsTableBuilder table = new org.cloudsimplus.builders.tables.CloudletsTableBuilder(finishedCloudlets);
        return table;
    }

    private List<Cloudlet> createAndbindCPUfirstCloudlets(List<Vm> Cpuvmlist){
        List<Cloudlet> CPUfirstCloudlets = new ArrayList<>();
        for(Vm vm : Cpuvmlist){
            //vm.addOnHostDeallocationListener(this::submitNewVmAndCloudletsToBroker);

            List<Cloudlet> interCloudletList = createCloudList(6);
            for(Cloudlet cl: interCloudletList){
                cl.setId(lastCloudletindex);
                lastCloudletindex = ++lastCloudletindex;
                this.broker.bindCloudletToVm(cl,vm);

                CPUfirstCloudlets.add(cl);
            }
            var testcl = CPUfirstCloudlets.get(CPUfirstCloudlets.size()-1);
             testcl.addOnFinishListener(this::submitGpuVmAndCloudletsToBroker);
        }
        CPUfirstCloudlets.get(CPUfirstCloudlets.size()-1).setId(19999);
        Cpuvmlist.get(Cpuvmlist.size()-1).setId(91111);
        return CPUfirstCloudlets;
    }

    private List<GpuCloudlet> createAndbindGPUfirstCloudlets(List<GpuVm> Gpuvmlist){
        List<GpuCloudlet> GPUfirstCloudlets = new ArrayList<>();
        for(Vm vm : Gpuvmlist){
            //vm.addOnHostDeallocationListener(this::submitNewVmAndCloudletsToBroker);

            List<GpuCloudlet> interCloudletList = createGpuCloudletList(1,(int)this.broker.getId(),6);
            for(GpuCloudlet cl: interCloudletList){
                cl.setId(lastCloudletindex);
                lastCloudletindex = ++lastCloudletindex;
                this.broker.bindCloudletToVm(cl,vm);
                GPUfirstCloudlets.add(cl);
            }
            var testcl = GPUfirstCloudlets.get(GPUfirstCloudlets.size()-1);
            testcl.addOnFinishListener(this::submitSimpleVmAndCloudletsToBroker);
        }
        GPUfirstCloudlets.get(GPUfirstCloudlets.size()-1).setId(22229);
        Gpuvmlist.get(Gpuvmlist.size()-1).setId(92222);
        return GPUfirstCloudlets;
    }

    /**
     * Convert the state of a Vm to GpuVm along the simulation time.
     * bind random GpuCloudlets to this GpuVm and prepare it to migration
     ** @param "Vm"
     */
    private GpuVm convertToGpuVm(Vm vm){
        temp=vm.getId();
        vmcopy= vm;

        //this.broker.destroyVm(vm);
        GpuVm gpuvm =  new GpuVm( gpuMips, vmPes, new CloudletSchedulerTimeShared());
        gpuvm.setRam(vmcopy.getRam().getCapacity()).setBw(vmcopy.getBw().getCapacity()).setSize(vmcopy.getStorage().getCapacity()).enableUtilizationStats();
        GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
        // Create a Vgpu
        Vgpu vgpu = GridVgpuTags.getK120Q(simulation,lastvgpuid, gpuTaskScheduler);
        lastvgpuid++;
        gpuvm.setVgpu(vgpu);
        gpuvm.setId(vmcopy.getId());
        gpuvm.setStartTime(vmcopy.getStartTime());
        for (VmStateHistoryEntry state:vmcopy.getStateHistory()){
            gpuvm.addStateHistoryEntry(state);}
        gpuvm.setTimeZone(vmcopy.getTimeZone()).setArrivedTime(vmcopy.getArrivedTime());
        gpuvm.setDescription(vmcopy.getDescription());

        return gpuvm;
    }
    private Vm convertToSimpleVm(Vm vm){

        vmcopy= vm;
        //this.broker.destroyVm(vm);
        Vm Svm =  new VmSimple( gpuMips, vmPes, new CloudletSchedulerTimeShared());
        Svm.setRam(vmcopy.getRam().getCapacity()).setBw(vmcopy.getBw().getCapacity()).setSize(vmcopy.getStorage().getCapacity()).enableUtilizationStats();

        Svm.setId(vmcopy.getId());
        Svm.setStartTime(vmcopy.getStartTime());
        for (VmStateHistoryEntry state:vmcopy.getStateHistory()){
            Svm.addStateHistoryEntry(state);}
        Svm.setTimeZone(vmcopy.getTimeZone()).setArrivedTime(vmcopy.getArrivedTime());
        Svm.setDescription(vmcopy.getDescription());

        return Svm;
    }

    /**
     * Checks if the Cloudlet is finishing his execution.
     * If so, request the simulation interruption.
     * @param eventInfo object containing data about the happened event
     */
    private void submitGpuVmAndCloudletsToBroker(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t# last cloudlet %d finished. Submitting GpuVM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId()/*getVm().getCloudletScheduler().getCloudletFinishedList().get(5).getCloudletId()*/, eventInfo.getVm().getId());
        Vm vmtemp= eventInfo.getVm();

        this.broker.destroyVm(eventInfo.getVm());
        //convertAndsubmitGpuVmsAndCloudlets(eventInfo.getCloudlet().getVm());
        SubmitGpuVmsAndCloudlets(vmtemp);
        vmDestructionRequested=true;
        //  temp =eventInfo.getVm();
    }
    private void submitSimpleVmAndCloudletsToBroker(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t# last cloudlet %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId()/*getVm().getCloudletScheduler().getCloudletFinishedList().get(5).getCloudletId()*/, eventInfo.getVm().getId());
        Vm vmtemp= eventInfo.getVm();

        this.broker.destroyVm(eventInfo.getVm());
        //convertAndsubmitGpuVmsAndCloudlets(eventInfo.getCloudlet().getVm());
        SubmitSimpleVmsAndCloudlets(vmtemp);
        vmDestructionRequested=true;
        //  temp =eventInfo.getVm();
    }
    private void SubmitGpuVmsAndCloudlets(Vm vm) {
        List<GpuCloudlet> newCloudletList = new ArrayList<>();
//          List<GpuCloudlet> newCloudletList = new ArrayList<>();
        GpuVm gpuvm =convertToGpuVm(vm) ;
        gpuvm.setId(temp);
        newCloudletList = createGpuCloudletList(1,(int)this.broker.getId(),3);
        int i = 555555555;
        for (Cloudlet cl: newCloudletList){
            cl.setId(lastCloudletindex+i);
            lastCloudletindex = ++lastCloudletindex;

            //cl.setVm(gpuvm);
            this.broker.bindCloudletToVm(cl, gpuvm);

//            cl.addOnFinishListener(this::printsecondcloudletsHistory);
            i++;
        }
        //gpuvm.addOnHostDeallocationListener(this::printsecondVmssHistory);
        this.broker.submitVm(gpuvm);
        this.broker.submitCloudletList(newCloudletList);
        //newCloudletList.forEach(cl-> this.broker.bindCloudletToVm(cl, gpuvm));
        System.out.printf("%n\t# Submit GpuVm %d and last Gpucloudlet of new cloudlest list %d to the broker %n",gpuvm.getId(),newCloudletList.get(newCloudletList.size()-1).getId());
        migrationsNumber++;
    }

    private void  SubmitSimpleVmsAndCloudlets(Vm vm) {
        List<Cloudlet> newCloudletList = new ArrayList<>();
//          List<GpuCloudlet> newCloudletList = new ArrayList<>();
        Vm newvm =convertToSimpleVm(vm) ;

        newCloudletList = createCloudList(3);
        int i = 7777777;
        for (Cloudlet cl: newCloudletList){
            cl.setId(lastCloudletindex+i);
            lastCloudletindex = ++lastCloudletindex;

            //cl.setVm(gpuvm);
            this.broker.bindCloudletToVm(cl, newvm);

//            cl.addOnFinishListener(this::printsecondcloudletsHistory);
            i++;
        }
        //gpuvm.addOnHostDeallocationListener(this::printsecondVmssHistory);
        this.broker.submitVm(newvm);
        this.broker.submitCloudletList(newCloudletList);
        //newCloudletList.forEach(cl-> this.broker.bindCloudletToVm(cl, gpuvm));
        System.out.printf("%n\t# Submit Vm %d and last cloudlet of new cloudlet list %d to the broker %n",newvm.getId(),newCloudletList.get(newCloudletList.size()-1).getId());
        migrationsNumber++;
    }

    private void printsecondVmssHistory(VmHostEventInfo eventInfo) {
        System.out.printf(
                "%n\t# Vm %d finished. Printing Vm %d History%n",
                eventInfo.getVm().getId(), eventInfo.getVm().getId());

    }
    private void printsecondcloudletsHistory(CloudletVmEventInfo eventInfo) {
        System.out.printf(
                "%n\t# Cloudlet %d finished. Printing Cl %d History%n",
                eventInfo.getCloudlet().getId(), eventInfo.getCloudlet().getId());
        this.broker.getCloudletFinishedList().add(eventInfo.getCloudlet());

    }
    /**
     * Prints the state of a Host along the simulation time.
     * <p>Realize that the Host State History is just collected
     * if {@link Host#isStateHistoryEnabled() history is enabled}
     * by calling {@link Host#enableStateHistory()}.</p>
     *
     * @param host
     */
    private void printHostHistory(Host host) {
        final boolean cpuUtilizationNotZero =
                host.getStateHistory()
                        .stream()
                        .map(HostStateHistoryEntry::percentUsage)
                        .anyMatch(cpuUtilization -> cpuUtilization > 0);

        if(cpuUtilizationNotZero) {
            new HostHistoryTableBuilder(host).setTitle(host.toString()).build();
        } else System.out.printf("\t%s CPU was zero all the time%n", host);
    }


}
