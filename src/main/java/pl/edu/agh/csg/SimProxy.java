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

public class  SimProxy {

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
    private List<Cloudlet> GPUfirtCloudleList;
    private List<GpuCloudlet> gpucloudletList;
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
    public SimProxy(String identifier,
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
        Cloudlet cl = new CloudletSimple(200,1);
        cl.setId(2222222);
        //cl.setVm(this.vmList.get(0));
        this.broker.bindCloudletToVm(cl,this.vmList.get(0));
        this.gpuvmList = createGpuVmList( toArray( (JsonArray) this.GpuvmTuple));

        this.CPUfirstvmList = createVmList(ForvmTuple);
    //    this.CPUfirstvmList.forEach(vm -> vm.addOnMigrationStartListener(this::startMigration));
        this.GPUfisrtgpuvmList = createGpuVmList( ForvmTuple);

        this.Cloudletlist = createCloudList(cloudletCnt);
        this.gpucloudletList = createGpuCloudletList(1,(int)this.broker.getId(),gpuclouletCnt);
        this.CPUfirtCloudleList =createAndbindCPUfirstCloudlets(CPUfirstvmList);

        /***
         * Submition of the Lists to the Broker
          */
        this.vmList.addAll(this.gpuvmList);
        this.vmList.addAll(this.CPUfirstvmList);
        this.broker.submitVmList(this.vmList);

        this.Cloudletlist.addAll(this.gpucloudletList);
        this.Cloudletlist.addAll(this.CPUfirtCloudleList);
        this.Cloudletlist.add(cl);
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
        /*  simulation.startSync();
        while(simulation.isRunning() ){
            simulation.runFor(10);
            simulation.pause();
            if(vmDestructionRequested){
                simulation.pause();
                if (vmcopy==temp && vmcopy!=null){
            tryDestroyVmAndResubmitCloudlets(vmcopy);
                temp=Vm.NULL;}
        }simulation.resume();}
    /*     this.simulation.startSync();
        while(simulation.isRunning()){
            //simulation.runFor(INTERVAL);
            while(!CPUfirstvmList.isEmpty()){
                this.simulation.pause();
            while (CPUfirstvmList.stream().findFirst().filter(vm ->vm.getCloudletScheduler().getCloudletList().stream().allMatch(cloudlet -> cloudlet.isFinished())).isPresent()) {

                this.sourceVm = CPUfirstvmList.stream().findFirst().filter(vm ->vm.getCloudletScheduler().getCloudletList().stream().allMatch(cloudlet -> cloudlet.isFinished())).get();

                CPUfirstvmList.remove(CPUfirstvmList.stream().findFirst().filter(vm ->vm.getCloudletScheduler().getCloudletList().stream().allMatch(cloudlet -> cloudlet.isFinished())).get());
                //this.sourceVm = convertToGpuVm(this.sourceVm);

                this.sourceVmList.add(this.sourceVm);

        }
            for (Vm vm: this.sourceVmList) {
                 vm.getCloudletScheduler().getCloudletList().get(vm.getCloudletScheduler().getCloudletFinishedList().size()).addOnFinishListener(this::submitNewVmAndCloudletsToBroker);
            }
            //this.sourceVmList.forEach(vm -> vm.addOnMigrationStartListener(this::startMigration));
            //this.simulation.addOnClockTickListener(this::clockTickListenerForGpu);


        //simulation.resume();
        }this.simulation.resume();}*/
/* broker.getVmExecList().stream()
                    .filter(vm -> vm.getId() == CPUfirstvmList.get(0).getId())
                    .findFirst().get()           */
        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        finishedCloudlets.sort(
                Comparator.comparingLong((Cloudlet c) -> c.getVm().getHost().getId())
                        .thenComparingLong(c -> c.getVm().getId()));
        //final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        new org.cloudsimplus.builders.tables.CloudletsTableBuilder(finishedCloudlets).build();
        System.out.printf("%nHosts CPU usage History (when the allocated MIPS is lower than the requested, it is due to VM migration overhead)%n");

        this.datacenter.getHostList().forEach(this::printHostHistory);
        System.out.printf("Number of VM migrations: %d%n", migrationsNumber);
        System.out.println(getClass().getSimpleName() + " finished!");
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

            gpuCloudlet.setUserId(brokerId);
            list.add(gpuCloudlet);
        }

        return list;
    }



    public int[][] toArray(JsonArray obj){
        int[][] intArray = gson.fromJson(obj, int[][].class);
        return intArray;
    }

    /**
     * Methode for Logger info
     * @param message
     */

//    private void info(String message) { logger.info(getIdentifier() + " " + message);    }

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


    /**
     * Event listener which is called every time the simulation clock advances.
     * When the simulation clock reaches 10 seconds, it migrates an arbitrary VM to
     * an arbitrary Host.
     *
     * @param info information about the event happened.
     * @see CloudSim#addOnClockTickListener(EventListener)
     */
    private void clockTickListener(CloudletVmEventInfo info) {
            Vm gpuvm=convertAndsubmitGpuVmsAndCloudlets(info.getVm());
            Host targetHost =  //datacenter.getVmAllocationPolicy().findHostForVm(vm).get();
                    findRandomSuitableHostForVm(this.datacenter.getVmAllocationPolicy(),gpuvm);
            System.out.printf("%n# Requesting the migration of %s to %s%n%n",gpuvm, targetHost);
            this.datacenter.requestVmMigration(gpuvm, targetHost);


            }

    private void clockTickListenerForGpu(EventInfo info) {
        if(!migrationRequested ){
            for(Vm vm: this.sourceVmList){
                Host targetHost =  firstfitSuitableGpuHostForGpuVm(allocationPolicy,vm);
                        //firstfitSuitableGpuHostForGpuVm(datacenter.getVmAllocationPolicy(),vm);
                System.out.printf("%n# Requesting the migration of %s to %s%n%n",vm, targetHost);
                this.datacenter.requestVmMigration(vm, targetHost);
                this.migrationRequested = true;
            }}
    }

    /**
     * A listener method that is called when a VM migration starts.
     * @param info information about the happened event
     *
     * @see Vm#addOnMigrationFinishListener(EventListener)
     */

    private void startMigration(final VmHostEventInfo info) {
         final Vm vm = info.getVm();
        final Host targetHost = info.getHost();
        System.out.printf(
                "# %.2f: %s started migrating to %s (you can perform any operation you want here)%n",
                info.getTime(), vm, targetHost);

        migrationsNumber++;
    }
    private Host findRandomSuitableHostForVm(final VmAllocationPolicy vmAllocationPolicy, final Vm vm) {
        final List<Host> hostList = vmAllocationPolicy.getHostList();
        /* Despite the loop is bound to the number of Hosts inside the List,
         *  the index "i" is not used to get a Host at that position,
         *  but just to define that the maximum number of tries to find a
         *  suitable Host will be the number of available Hosts.*/
        for (int i = 0; i < hostList.size(); i++){
            final int randomIndex = (int)(random.sample() * hostList.size());
            final Host host = hostList.get(randomIndex);
            if(host.isSuitableForVm(vm)){
                return host;
            }
        }
        return Host.NULL;
    }

    private Host firstfitSuitableGpuHostForGpuVm(final VmAllocationPolicy vmAllocationPolicy, final Vm vm) {
        final List<Host> hostList = datacenter.getVmAllocationPolicy().getHostList();
        /* Despite the loop is bound to the number of Hosts inside the List,
         *  the index "i" is not used to get a Host at that position,
         *  but just to define that the maximum number of tries to find a
         *  suitable Host will be the number of available Hosts.*/
        for (int i = 0; i < hostList.size(); i++){
            final int randomIndex = (int)(random.sample() * hostList.size());
            final Host host = hostList.get(i);
            if(host.isSuitableForVm(vm)){
                return host;
            }
        }
        return Host.NULL;
    }
    private Host bestFitHostSelectionPolicy(VmAllocationPolicy allocationPolicy, Vm vm) {
        Host gpuhost =allocationPolicy
                .getHostList()
                .stream()
                .filter(host -> host.isSuitableForVm(vm))
                .findAny().orElse(Host.NULL);
        if(gpuhost == Host.NULL){
            Host newgpuhost = createGpuHost(3);
            this.datacenter.addHost(newgpuhost);
            return newgpuhost;
        }else       return gpuhost;
    }
    private List<Cloudlet> createAndbindCPUfirstCloudlets(List<Vm> Cpuvmlist){
        List<Cloudlet> CPUfirstCloudlets = new ArrayList<>();
        for(Vm vm : Cpuvmlist){
            vm.addOnHostDeallocationListener(this::submitNewVmAndCloudletsToBroker);

            List<Cloudlet> interCloudletList = createCloudList(6);
            for(Cloudlet cl: interCloudletList){
                this.broker.bindCloudletToVm(cl,vm);
                CPUfirstCloudlets.add(cl);
            }
            var testcl = CPUfirstCloudlets.get(CPUfirstCloudlets.size()-1);
           // testcl.addOnFinishListener(this::submitNewVmAndCloudletsToBroker);
        }
        CPUfirstCloudlets.get(CPUfirstCloudlets.size()-1).setId(19999);
        Cpuvmlist.get(Cpuvmlist.size()-1).setId(91111);
        return CPUfirstCloudlets;
    }

    /**
     * Convert the state of a Vm to GpuVm along the simulation time.
     * bind random GpuCloudlets to this GpuVm and prepare it to migration
     ** @param "Vm"
     */
    private GpuVm convertToGpuVm(Vm vm){
        temp=vm.getId()+10000;
        vmcopy= vm;

        //this.broker.destroyVm(vm);
        GpuVm gpuvm = new GpuVm((int)vmcopy.getBroker().getId(),(long)vmcopy.getTotalMipsCapacity(),vmcopy.getNumberOfPes(),new CloudletSchedulerTimeShared());
        gpuvm.setRam(vmcopy.getRam().getCapacity()).setBw(vmcopy.getBw().getCapacity()).setSize(vm.getStorage().getCapacity()).enableUtilizationStats();
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
    private GpuVm convertAndsubmitGpuVmsAndCloudlets(Vm vm){
        List<GpuCloudlet> clList = new ArrayList<>();
        GpuVm gpuvm=convertToGpuVm(vm);
        clList =createGpuCloudletList(1,(int)this.broker.getId(),gpuclouletCnt/8);
        // createGpuCloudletList(1,(int)this.broker.getId(),(int)(random.sample() * gpuvm.getCloudletScheduler().getCloudletList().size()-1)).forEach(cl -> this.broker.bindCloudletToVm(cl,gpuvm));
        for(Cloudlet cl: clList){
            this.broker.bindCloudletToVm(cl,gpuvm);
        }
        //clList.forEach(cl -> this.broker.bindCloudletToVm(cl,gpuvm));
        this.broker.submitVm(gpuvm);
        //gpuvm.addOnMigrationStartListener(this::startMigration);
        this.broker.submitCloudletList(clList);
        return gpuvm;
    }
    /**
     * Checks if the Cloudlet is finishing his execution.
     * If so, request the simulation interruption.
     * @param eventInfo object containing data about the happened event
     */
    private void submitNewVmAndCloudletsToBroker(VmHostEventInfo eventInfo) {
        System.out.printf(
        "%n\t# Vm %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getVm().getId(), eventInfo.getVm().getId());
        Vm vmtemp= eventInfo.getVm();
 //       this.broker.destroyVm(eventInfo.getVm());
            //convertAndsubmitGpuVmsAndCloudlets(eventInfo.getCloudlet().getVm());
        createAndSubmitVmsAndCloudlets(vmtemp);
        vmDestructionRequested=true;
      //  temp =eventInfo.getVm();
    }
    private void createAndSubmitVmsAndCloudlets(Vm vm) {
        List<Cloudlet> newCloudletList = new ArrayList<>();

            GpuVm gpuvm =convertToGpuVm(vm) ;
            gpuvm.setId(temp);
            newCloudletList = createCloudList(3);
            int i = 555555555;
            for (Cloudlet cl: newCloudletList){
                this.broker.bindCloudletToVm(cl, gpuvm);
                cl.setId(i);
                i++;
            }
        this.broker.submitVm(gpuvm);
        this.broker.submitCloudletList(newCloudletList);
        System.out.printf("%n\t# Create and Submit new GpuVm %d to the broker %d %n",gpuvm.getId(),this.broker.getId());
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

/*    private void onVmsCreatedListener(final DatacenterBrokerEventInfo info) {
        System.out.printf("# All %d VMs submitted to the broker have been created.%n", broker.getVmCreatedList().size());
        allocationPolicy.setOverUtilizationThreshold(HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);
        broker.removeOnVmsCreatedListener(info.getListener());
        vmList.forEach(vm -> showVmAllocatedMips(vm, vm.getHost(), info.getTime()));

        System.out.println();
        this.createdSimpleHostList.forEach(host -> showHostAllocatedMips(info.getTime(), host));
        System.out.println();
    }
    private void showVmAllocatedMips(final Vm vm, final Host targetHost, final double time) {
        final String msg = String.format("# %.2f: %s in %s: total allocated", time, vm, targetHost);
        final MipsShare allocatedMips = targetHost.getVmScheduler().getAllocatedMips(vm);
        final String msg2 = allocatedMips.totalMips() == VM_MIPS * 0.9 ? " - reduction due to migration overhead" : "";
        System.out.printf("%s %.0f MIPs (divided by %d PEs)%s\n", msg, allocatedMips.totalMips(), allocatedMips.pes(), msg2);
    }
    private void showHostAllocatedMips(final double time, final Host host) {
        System.out.printf(
                "%.2f: %s allocated %.2f MIPS from %.2f total capacity%n",
                time, host, host.getTotalAllocatedMips(), host.getTotalMipsCapacity());
    }*/


    /**
     * Checks if the simulation clock reached the time defined to request
     * a VM destruction. If so, destroys a VM and resubmit its unfinished
     * Cloudlets to the broker, so that it can decide which VM
     * to run such Cloudlets.
     */
    private void tryDestroyVmAndResubmitCloudlets(Vm vm) {

            final List<Cloudlet> affected = this.broker.destroyVm(vm);
            System.out.printf("%.2f: Re-submitting %d Cloudlets that weren't finished in the destroyed %s:%n", simulation.clock(), affected.size(), vm);
            affected.forEach(cl -> System.out.printf("\tCloudlet %d%n", cl.getId()));
            System.out.println();
            this.broker.submitCloudletList(affected);

    }

}
