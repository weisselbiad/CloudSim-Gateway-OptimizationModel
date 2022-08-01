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
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigration;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationWorstFitStaticThreshold;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.power.models.PowerModelHostSpec;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
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
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.slametrics.SlaContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class  SimProxy3 {

    public static final int SMALL = 1;
    public static final int MEDIUM = 2;
    public static final int LARGE = 3;
    Gson gson = new GsonBuilder().create();

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
    private int lastVmIndex;
    private int lastHostIndex;
    private int lastCloudletindex;
    private int lastvgpuid;
    private int migrationsNumber;
    private int SLAviolationnumber;

    public SimSettings settings = new SimSettings();


    private SlaContract contract;

    /**
     * Class declaration of the Cloud Components and Simulation
     */

    private Object VmType ;
    private List<Vm> vmList = new ArrayList<>() ;
    private List<Vm> CPUfirstvmList ;

    private List<GpuVm> gpuvmList;
    private  List<Cloudlet> Cloudletlist;
    private List<Cloudlet> CPUfirstCloudletList ;

    private List<GpuCloudlet> gpucloudletList;
    private Datacenter datacenter;
    private MixedDatacenterBroker broker;

    List<Integer> CloudletMips = Arrays.asList(5000, 40000, 30000,10000,50000);
    List<Long> GpuTaskMips = Arrays.asList((long) (GridVideoCardTags.NVIDIA_K1_CARD_PE_MIPS ),(long) (GridVideoCardTags.NVIDIA_K2_CARD_PE_MIPS ),(long) (GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K80_CARD,1000) ),(long) (GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_M60_CARD,850) ));
    Random randMips = new Random();
    private static final double CLOUDLET_CPU_USAGE_INCREMENT_PER_SECOND = 0.05;
    /**
     * The file containing the Customer's SLA Contract in JSON format.
     */
    private static final String CUSTOMER_SLA_CONTRACT = "CustomerSLA.json";
    private final CloudSim simulation;

    /**
     * Initializing variables from the Setting Class
     */

    private Object Result;

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
    private final ContinuousDistribution random;

    public SimProxy3(String identifier,
                    Object Result){

        this.contract = SlaContract.getInstance(CUSTOMER_SLA_CONTRACT);
        random = new UniformDistr();
        /**
         * Simulation identifier in case of instancing more than one simulation
         */

        this.identifier = identifier;
        this.Result = Result;

        /**
         * Initializing the Simulation, as parameter a double should be passed which is the minimum
         * time between events
         */

        this.simulation = new CloudSim(0.1);

        /**
         * Creating the Datacenter and calling the Broker
         */
        int[][] hostTuple = { { 1, 1}, { 2, 1},  { 3, 1} };
        int[][] GpuhostTuple = { { 1, 1}, { 2, 1},  { 3, 1} };
        this.datacenter =  createDatacenter (hostTuple,  GpuhostTuple)  ;
        this.broker = new MixedDatacenterBroker(this.simulation);

        /**
         * Creating a List of Virtual machines and Cloudlets
         */

        System.out.println("Result from Proxy: "+this.Result);
/*
        this.vmList = createVmList( toArray( (JsonArray) this.vmTuple));
        this.gpuvmList = createGpuVmList( toArray( (JsonArray) this.GpuvmTuple));
        int[][] arr = { { 1, 2}, { 2, 2 },  { 3, 2 } };
        this.CPUfirstvmList = createVmList(arr);
        int[][] arr2 = { { 1, 1}, { 2, 1 },  { 3, 1 } };
        Vm testCpufirstVm= createVm(2);
        this.CPUfirstCloudletList = createCPUfirstCloudList(5);
        HashMap<Vm, List<Cloudlet>> hm1 = new HashMap<>();

        this.Cloudletlist = createCloudList(cloudletCnt);
        this.gpucloudletList = createGpuCloudletList(gpuclouletCnt);

        /***
         * Submition of the Lists to the Broker
         */
      /*  this.vmList.addAll(this.gpuvmList);
        this.broker.submitVmList(this.vmList);

        this.Cloudletlist.addAll(this.gpucloudletList);
        this.broker.submitCloudletList(this.Cloudletlist);*/
        int[][] arr2 = { { 1, 1}, { 2, 1 },  { 3, 1 } };
        createVmList(toArray( (JsonArray) this.Result));
      //  this.broker.submitVmList(createGpuVmList( toArray( (JsonArray) this.GpuvmTuple)));

        info("Creating simulation: " + identifier);

    }

    /**
     * Methode to run the Simulation used start it through sockets of Py4j
     * by need is a Table builder included to print the results
     */

    public void runSim(){

        this.simulation.start();

        final List<Cloudlet> finishedCloudlets = this.broker.getCloudletFinishedList();
        new org.cloudsimplus.builders.tables.CloudletsTableBuilder(finishedCloudlets).build();

    }

    /**
     * Creation of the Datacenter using a List of Hosts, which will be passed as a parameter
     * Uses a VmAllocationPolicySimple by default to allocate VMs
     *  @return Object Datacentersimple
     * @param hostTuple
     */

    private Datacenter createDatacenter(int[][] hostTuple, int[][] gpuhostTuple) {

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
        final VmAllocationPolicyMigration allocationPolicy
                = new VmAllocationPolicyMigrationWorstFitStaticThreshold(
                new VmSelectionPolicyMinimumUtilization(),
                contract.getCpuUtilizationMetric().getMaxDimension().getValue());
        allocationPolicy.setUnderUtilizationThreshold(contract.getCpuUtilizationMetric().getMinDimension().getValue());
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

        // Create a host
        int hostId = 0;

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
        lastHostIndex = ++lastHostIndex ;
        return gpuhost;
    }


    private Vm createAndbindsequentialVm(int factor, int clSequence, int vmallocationid){

        Vm finalVm = Vm.NULL;
        Cloudlet cl = createCloudlet(cloudletPes,cloudletSize);
        GpuCloudlet gcl = createGpuCloudlet(1, (int)this.broker.getId());
        switch(clSequence)
        {
            case 1 :{ cl.setpathid(1);
                Vm vm =createVm(factor);
                vm.setvmallocationid(vmallocationid);
                System.out.println("created cloudlet :" + cl.getId());
                System.out.println("created Vm :" + vm.getId());
                cl.addOnFinishListener(this::Listener1);
                vm.setvmallocationid(vmallocationid);
                this.broker.bindCloudletToVm(cl,vm);
                this.broker.submitCloudlet(cl);
                this.broker.submitVm(vm);
                finalVm = vm ; }
                    break;
            case 2 :{ cl.setpathid(2);
                Vm vm =createVm(factor);
                vm.setvmallocationid(vmallocationid);
                System.out.println("created cloudlet :" + cl.getId());
                System.out.println("created Vm :" + vm.getId());
                cl.addOnFinishListener(this::Listener2);
                vm.setvmallocationid(vmallocationid);
                this.broker.bindCloudletToVm(cl,vm);
                this.broker.submitCloudlet(cl);
                this.broker.submitVm(vm);
                finalVm = vm;}
            break;
            case 3 :{ gcl.setpathid(3);
                GpuVm gvm = createGpuVm(factor);
                gvm.setvmallocationid(vmallocationid);
                System.out.println("created Gpucloudlet :" + cl.getId());
                System.out.println("created GpuVm :" + gvm.getId());
                gcl.addOnFinishListener(this::Listener3);
                gvm.setvmallocationid(vmallocationid);
                this.broker.bindCloudletToVm(gcl,gvm);
                this.broker.submitCloudlet(gcl);
                this.broker.submitVm(gvm);
                finalVm  = gvm;}
            break;
            case 4: { gcl.setpathid(4);
                GpuVm gvm = createGpuVm(factor);
                gvm.setvmallocationid(vmallocationid);
                System.out.println("created Gpucloudlet :" + cl.getId());
                System.out.println("created GpuVm :" + gvm.getId());
                gcl.addOnFinishListener(this::Listener4);
                gvm.setvmallocationid(vmallocationid);
                this.broker.bindCloudletToVm(gcl,gvm);
                this.broker.submitCloudlet(gcl);
                this.broker.submitVm(gvm);
                finalVm  = gvm;  }
            break;
            case 5 :{ Vm vm =createVm(factor);
                vm.setvmallocationid(vmallocationid);
                finalVm= createAndbindCPUfirstVms(vm);
                vmList.add(finalVm);}
            break;
            case 6:{  GpuVm gvm = createGpuVm(factor);
                gvm.setvmallocationid(vmallocationid);
                finalVm= createAndbindGPUfirstVms(gvm);
                vmList.add(finalVm);}
            break;
            default:{
                System.out.println("Illegal Cloudlets Sequence ");
                break;
        } }
        return finalVm;
    }

    /**
     * Creating a List of virtual machines with a randomly passed hardware configuration
     * @return List of Virtual machines
     * @param vmTuple
     */

    private void createVmList(int[][] vmTuple) {
        this.Result = vmTuple;
        final List<Vm> list = new ArrayList<>();
        for (int i = 0; i < vmTuple.length; i++) {
            int factor = getSizeFactor(vmTuple[i][0]);

               vmList.add(createAndbindsequentialVm(factor,vmTuple[i][1],vmTuple[i][2]));
        }
   }

    private Vm createVm(int factor){
        final Vm vm = new VmSimple(hostPeMips*factor,vmPes*factor);
        vm.setRam(vmRam*factor).setBw(vmBw*factor).setSize(vmSize*factor).enableUtilizationStats();
        vm.setId(lastVmIndex);
        lastVmIndex = ++lastVmIndex ;
        return vm;
    }
/*    private List<GpuVm> createGpuVmList(int[][] gpuvmTuple) {
        this.GpuvmTuple = gpuvmTuple;
        final List<GpuVm> gpuvmlist = new ArrayList<>();
        for (int i = 0; i < gpuvmTuple.length; i++) {
            int factor = getSizeFactor(gpuvmTuple[i][0]);

            for (int j = 0; j < gpuvmTuple[i][1] ; j++) {

                GpuVm gvm = (GpuVm) createAndbindsequentialVm(factor,6);
                gpuvmlist.add(gvm);
            }}
        return gpuvmlist;
    }*/

    private GpuVm createGpuVm(int factor){
        String vmm = "vSphere";
        //GpuCloudletSchedulerTimeShared GCSTS = new GpuCloudletSchedulerTimeShared();

        // Create a VM
        GpuVm vm = new GpuVm( gpuMips*factor, vmPes*factor, new CloudletSchedulerSpaceShared());
        vm.setRam(gpuvmRam*factor);
        vm.setBw(gpuvmBw*factor);
        vm.setSize(gpuvmSize*factor);
        // Create GpuTask Scheduler
        GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
        // Create a Vgpu
        Vgpu vgpu = GridVgpuTags.getK120Q(simulation,lastvgpuid, gpuTaskScheduler);
        lastvgpuid++;
        vm.setVgpu(vgpu);
        vm.setId(lastVmIndex);
        lastVmIndex = ++lastVmIndex ;
        return vm;
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

    private List<Cloudlet> createCloudList(int cnt) {
        final List<Cloudlet> list = new ArrayList<>(cloudletCnt);

        for (int i = 0; i < cnt; i++) {
             Cloudlet cloudlet = createCloudlet(cloudletPes,cloudletSize);

            list.add(cloudlet);
        }
        return list;
    }
    private Cloudlet createCloudlet(int pes,long clSize ){
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(UtilizationModel.Unit.ABSOLUTE,0.2);
        int randomlength = CloudletMips.get(randMips.nextInt(CloudletMips.size()));
        UtilizationModelDynamic cpuUtilizationModel =
                createUtilizationModel(
                        0.2,1,
                        true);
         Cloudlet cloudlet =
                new CloudletSimple(randomlength,pes)
                        .setFileSize(1024)
                        .setOutputSize(1024)
                        .setUtilizationModelCpu(cpuUtilizationModel)
                        .setUtilizationModelRam(utilizationModel)
                        .setUtilizationModelBw(utilizationModel)
                        .setSizes(clSize);
        cloudlet.setId(lastCloudletindex);
        lastCloudletindex = ++lastCloudletindex;
        System.out.println("created cloudlet :" + cloudlet.getId());
        return cloudlet;
    }

    private List<GpuCloudlet> createGpuCloudletList(int cnt) {
        final List<GpuCloudlet> list = new ArrayList<>(gpuclouletCnt);

        for (int j = 0; j < gpuclouletCnt; j++) {

            GpuCloudlet gpuCloudlet= createGpuCloudlet(1,(int)this.broker.getId());

            list.add(gpuCloudlet);
        }

        return list;
    }
    private GpuCloudlet createGpuCloudlet(int gpuTaskId, int brokerId){
        // Cloudlet properties
        long length = (long) (400 * GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS);
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 2;
        UtilizationModelDynamic cpuUtilizationModel =
                createUtilizationModel(
                        0.5,
                        1.5,
                        true);
        UtilizationModel ramUtilizationModel = new UtilizationModelDynamic(UtilizationModel.Unit.ABSOLUTE, 50);
        UtilizationModel bwUtilizationModel = new UtilizationModelDynamic(UtilizationModel.Unit.ABSOLUTE, 50);

        // GpuTask properties
        long taskLength = GpuTaskMips.get((int) randMips.nextLong(GpuTaskMips.size()));
        long taskInputSize = 128;
        long taskOutputSize = 128;
        long requestedGddramSize = 4 * 1024;
        int numberOfBlocks = 2;

        UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
        UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
        UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();
        GpuTask gpuTask = new GpuTask(simulation, gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
                requestedGddramSize, 0, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
        int randomlength = CloudletMips.get(randMips.nextInt(CloudletMips.size()));
        GpuCloudlet gpuCloudlet = new GpuCloudlet(randomlength, pesNumber,fileSize, outputSize,cpuUtilizationModel,ramUtilizationModel,bwUtilizationModel, gpuTask);
        gpuCloudlet.setUserId(brokerId);
        gpuCloudlet.setId(lastCloudletindex);
        lastCloudletindex = ++lastCloudletindex;
        System.out.println("created Gpucloudlet :" + gpuCloudlet.getId());
        return gpuCloudlet;

    }

    /**
     * Creates a {@link UtilizationModel} for a Cloudlet
     * defines if CPU usage will be static or dynamic.
     *
     * @param initialCpuUsagePercent the percentage of CPU the Cloudlet will use initially
     * @param maxCloudletCpuUsagePercent the maximum percentage of CPU the Cloudlet will use
     * @param progressiveCpuUsage true if the CPU usage must increment along the time, false if it's static.
     * @return the  {@link UtilizationModel} for a Cloudlet's CPU usage
     */
    private UtilizationModelDynamic createUtilizationModel(
            double initialCpuUsagePercent,
            double maxCloudletCpuUsagePercent,
            final boolean progressiveCpuUsage)
    {
        initialCpuUsagePercent = Math.min(initialCpuUsagePercent, 1);
        maxCloudletCpuUsagePercent = Math.min(maxCloudletCpuUsagePercent, 1);
        final UtilizationModelDynamic um = new UtilizationModelDynamic(initialCpuUsagePercent);

        if (progressiveCpuUsage) {
            um.setUtilizationUpdateFunction(this::getCpuUtilizationIncrement);
        }

        um.setMaxResourceUtilization(maxCloudletCpuUsagePercent);
        return um;
    }

    /**
     * Increments the CPU resource utilization, that is defined in percentage
     * values.
     *
     * @return the new resource utilization after the increment
     */
    private double getCpuUtilizationIncrement(final UtilizationModelDynamic um) {
        return um.getUtilization() + um.getTimeSpan() * CLOUDLET_CPU_USAGE_INCREMENT_PER_SECOND;
    }

    private Vm createAndbindCPUfirstVms(Vm Cpuvm){
        List<Cloudlet> CPUfirstCloudlets = new ArrayList<>();

            List<Cloudlet> interCloudletList = createCloudList(2);
            for(Cloudlet cl: interCloudletList){
                cl.setId(lastCloudletindex);
                lastCloudletindex = ++lastCloudletindex;

                this.broker.bindCloudletToVm(cl,Cpuvm);
                this.broker.submitCloudlet(cl);
                CPUfirstCloudlets.add(cl);
            }

            this.broker.submitVm(Cpuvm);
            var testcl = CPUfirstCloudlets.get(CPUfirstCloudlets.size()-1);
            testcl.addOnFinishListener(this::Listener5);

        return Cpuvm;
    }

    private GpuVm createAndbindGPUfirstVms(GpuVm Gpuvm){
        List<GpuCloudlet> GPUfirstCloudlets = new ArrayList<>();

            List<GpuCloudlet> interCloudletList = createGpuCloudletList(2);
            for(GpuCloudlet cl: interCloudletList){
                cl.setId(lastCloudletindex);
                lastCloudletindex = ++lastCloudletindex;

                this.broker.bindCloudletToVm(cl,Gpuvm);
                this.broker.submitCloudlet(cl);
                GPUfirstCloudlets.add(cl);

            }
            this.broker.submitVm(Gpuvm);
            var testcl = GPUfirstCloudlets.get(GPUfirstCloudlets.size()-1);
            testcl.addOnFinishListener(this::Listener6);

        GPUfirstCloudlets.get(GPUfirstCloudlets.size()-1).setId(22229);
        return Gpuvm;
    }

    /**
     * Convert the state of a Vm to GpuVm along the simulation time.
     * bind random GpuCloudlets to this GpuVm and prepare it to migration
     ** @param "Vm"
     */
    private GpuVm convertToGpuVm(Vm vm){
        Vm vmcopy;
        vmcopy= vm;

        //this.broker.destroyVm(vm);
        GpuVm gpuvm =  new GpuVm((long) vmcopy.getMips(), vmPes, new CloudletSchedulerTimeShared());
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
        gpuvm.setvmallocationid(vmcopy.getvmallocationid());
        vmList.add(gpuvm);
        return gpuvm;
    }
    private Vm convertToSimpleVm(Vm vm){
        Vm vmcopy;
        vmcopy= vm;
        //this.broker.destroyVm(vm);
        Vm Svm =  new VmSimple( vmcopy.getMips(), vmPes, new CloudletSchedulerTimeShared());
        Svm.setRam(vmcopy.getRam().getCapacity()).setBw(vmcopy.getBw().getCapacity()).setSize(vmcopy.getStorage().getCapacity()).enableUtilizationStats();

        Svm.setId(vmcopy.getId());
        Svm.setStartTime(vmcopy.getStartTime());
        for (VmStateHistoryEntry state:vmcopy.getStateHistory()){
            Svm.addStateHistoryEntry(state);}
        Svm.setTimeZone(vmcopy.getTimeZone()).setArrivedTime(vmcopy.getArrivedTime());
        Svm.setDescription(vmcopy.getDescription());
        Svm.setvmallocationid(vmcopy.getvmallocationid());
        vmList.add(Svm);
        return Svm;
    }


    private void Listener1(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t#  cloudlet %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
        int Cpathid = eventInfo.getCloudlet().getpathid();
        Vm vm= eventInfo.getVm();
        CheckSla(vm);
        //this.broker.destroyVm(eventInfo.getVm());
        if (Cpathid ==1){
            Cloudlet cl =createGpuCloudlet(1,(int)this.broker.getId());
            cl.addOnFinishListener(this::Listener1);
            cl.setpathid(5);
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
            } else if (Cpathid ==5) {
            Cloudlet cl =createCloudlet(cloudletPes,cloudletSize);
            cl.addOnFinishListener(this::Listener1);
            cl.setpathid(7);
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
            } else if (Cpathid == 7) {
            Cloudlet cl = createGpuCloudlet(1, (int)this.broker.getId());
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
            } else System.out.printf(
                "%n\t#  cloudlet %d in VM %d Path Id not recognized. (Listener 1)%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
                    }

    private void Listener2(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t#  cloudlet %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
        int Cpathid = eventInfo.getCloudlet().getpathid();
        Vm vm= eventInfo.getVm();
        CheckSla(vm);
        //this.broker.destroyVm(eventInfo.getVm());
        if (Cpathid ==2){
            Cloudlet cl1 =createGpuCloudlet(1,(int)this.broker.getId());
            Cloudlet cl2 =createGpuCloudlet(1,(int)this.broker.getId());
            cl2.addOnFinishListener(this::Listener2);
            cl2.setpathid(9);
            Vm gvm = convertToGpuVm(vm);
            this.broker.bindCloudletToVm(cl1,gvm);
            this.broker.bindCloudletToVm(cl2,gvm);
            this.broker.submitVm(gvm);
            this.broker.submitCloudlet(cl1);
            this.broker.submitCloudlet(cl2);
            migrationsNumber++;
        }else if (Cpathid == 9) {
            Cloudlet cl = createCloudlet(cloudletPes,cloudletSize);
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
        }else System.out.printf(
                "%n\t#  cloudlet %d in VM %d Path Id not recognized. (Listener 3)%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
    }
    private void Listener3(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t#  cloudlet %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
        int Cpathid = eventInfo.getCloudlet().getpathid();
        Vm vm= eventInfo.getVm();
        CheckSla(vm);
        //this.broker.destroyVm(eventInfo.getVm());
        if (Cpathid ==3){
            Cloudlet cl =createCloudlet(cloudletPes,cloudletSize);
            cl.addOnFinishListener(this::Listener3);
            cl.setpathid(6);
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
        }  else if (Cpathid ==6) {
            Cloudlet cl =createGpuCloudlet(1,(int)this.broker.getId());
            cl.addOnFinishListener(this::Listener3);
            cl.setpathid(8);
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
        }else if (Cpathid == 8) {
            Cloudlet cl = createCloudlet(cloudletPes,cloudletSize);
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
        }else System.out.printf(
                "%n\t#  cloudlet %d in VM %d Path Id not recognized. (Listener 3)%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
    }
    private void Listener4(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t#  cloudlet %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
        int Cpathid = eventInfo.getCloudlet().getpathid();
        Vm vm= eventInfo.getVm();
        CheckSla(vm);
        //this.broker.destroyVm(eventInfo.getVm());
        if (Cpathid ==4){
            Cloudlet cl1 =createCloudlet(cloudletPes,cloudletSize);
            Cloudlet cl2 =createCloudlet(cloudletPes,cloudletSize);
            cl2.addOnFinishListener(this::Listener4);
            cl2.setpathid(10);
            Vm Svm = convertToSimpleVm(vm);
            this.broker.bindCloudletToVm(cl1,Svm);
            this.broker.bindCloudletToVm(cl2,Svm);
            this.broker.submitVm(Svm);
            this.broker.submitCloudlet(cl1);
            this.broker.submitCloudlet(cl2);
            System.out.println("Listener 4 --->> Vm "+Svm.getId()+" Cloudlet path Id : "+cl2.getpathid());
            migrationsNumber++;
        }else if (Cpathid == 10) {
            Cloudlet cl = createGpuCloudlet(1,(int)this.broker.getId());
            bindAndSubmitCtoVm(cl,vm);
            migrationsNumber++;
        }else System.out.printf(
                "%n\t#  cloudlet %d in VM %d Path Id not recognized. (Listener 4)%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());

    }

    /**
     * Checks if the Cloudlet is finishing his execution.
     * If so, request the simulation interruption.
     * @param eventInfo object containing data about the happened event
     */
    private void Listener5(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t# last cloudlet %d finished. Submitting GpuVM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId());
        Vm vmtemp= eventInfo.getVm();
        CheckSla(vmtemp);
        this.broker.destroyVm(eventInfo.getVm());

        List<GpuCloudlet> CloudletList = new ArrayList<>();
        List<GpuCloudlet> newCloudletList = new ArrayList<>();
        GpuVm gpuvm =convertToGpuVm(vmtemp) ;
        setVmallocation(gpuvm.getvmallocationid());
        newCloudletList = createGpuCloudletList(2);
        int i = 555555;
        for (GpuCloudlet cl: newCloudletList){
            cl.setId(lastCloudletindex+i);
            lastCloudletindex = ++lastCloudletindex;
            this.broker.bindCloudletToVm(cl, gpuvm);
            CloudletList.add(cl);
            i++;
        }
        this.broker.submitVm(gpuvm);
        this.broker.submitCloudletList(CloudletList);
        System.out.printf("%n\t# Submit GpuVm %d and last Gpucloudlet of new cloudlest list %d to the broker %n",gpuvm.getId(),newCloudletList.get(newCloudletList.size()-1).getId());
        migrationsNumber++;
    }

    private void Listener6(CloudletVmEventInfo eventInfo) {
        //VmHostEventInfo
        //CloudletVmEventInfo
        System.out.printf(
                "%n\t# last cloudlet %d finished. Submitting VM %d to the broker for Migration%n",
                eventInfo.getCloudlet().getId()/*getVm().getCloudletScheduler().getCloudletFinishedList().get(5).getCloudletId()*/, eventInfo.getVm().getId());
        Vm vmtemp= eventInfo.getVm();
        CheckSla(vmtemp);
        this.broker.destroyVm(eventInfo.getVm());
        List<Cloudlet> CloudletList = new ArrayList<>();
        List<Cloudlet> newCloudletList = new ArrayList<>();
        Vm newvm =convertToSimpleVm(vmtemp) ;
        setVmallocation(newvm.getvmallocationid());
        newCloudletList = createCloudList(2);
        int i = 7777777;
        for (Cloudlet cl: newCloudletList){
            cl.setId(lastCloudletindex+i);
            lastCloudletindex = ++lastCloudletindex;
            this.broker.bindCloudletToVm(cl, newvm);
            CloudletList.add(cl);
            i++;
        }
        this.broker.submitVm(newvm);
        this.broker.submitCloudletList(CloudletList);
        System.out.printf("%n\t# Submit Vm %d and last cloudlet of new cloudlet list %d to the broker %n",newvm.getId(),newCloudletList.get(newCloudletList.size()-1).getId());
        migrationsNumber++;
    }

    private Vm bindAndSubmitCtoVm(Cloudlet cl, Vm vm){
        Vm newvm = Vm.NULL;

        if (cl.getClass().equals(CloudletSimple.class)){
            newvm=convertToSimpleVm(vm);
        } else if (cl.getClass().equals(GpuCloudlet.class)){
            newvm =convertToGpuVm(vm);
        }else System.out.println("Illegale Cloudlet Class Type");
        setVmallocation(vm.getvmallocationid());
        this.broker.bindCloudletToVm(cl,newvm);
        this.broker.submitVm(newvm);
        this.broker.submitCloudlet(cl);
        return newvm;
    }

    private List<Cloudlet> createCPUfirstCloudList(int listsize){
        final List<Cloudlet> list = new ArrayList<>();
        list.add(createCloudlet(cloudletPes,cloudletSize));
        for (int i=0; i< listsize; i++ ){
            int randbin =(int)Math.round(Math.random());
            if(randbin==0){list.add(createCloudlet(cloudletPes,cloudletSize));}
            else {list.add(createGpuCloudlet(1,(int)this.broker.getId()));}
        }return list;
    }
    private List<Cloudlet> createGPUfirstCloudList(int listsize){
        final List<Cloudlet> list = new ArrayList<>();
        list.add(createGpuCloudlet(1,(int)this.broker.getId()));
        for (int i=0; i< listsize; i++ ){
            int randbin =(int)Math.round(Math.random());
            if(randbin==0){list.add(createCloudlet(cloudletPes,cloudletSize));}
            else {list.add(createGpuCloudlet(1,(int)this.broker.getId()));}
        }return list;
    }

    public int[][] toArray(JsonArray obj){
        int[][] intArray = gson.fromJson(obj, int[][].class);
        return intArray;
    }

    public boolean CheckSla(Vm vm){
        boolean TaskCompletionTime = false,CpuUtilization = false, WaitTime = false,Price = false;
        for (Cloudlet cl : vm.getCloudletScheduler().getCloudletList()){
            if(cl.getLifeTime()< contract.getTaskCompletionTimeMetric().getMaxDimension().getValue()){
                TaskCompletionTime =false;
            }else
                System.out.println("Cloudlet "+cl.getId()+" in Vm "+vm.getId()+" has violated the SLA in term of TaskCompletionTime");
                TaskCompletionTime =true;
            SLAviolationnumber++;
        }
        for (Cloudlet cl : vm.getCloudletScheduler().getCloudletList()){
            if(cl.getUtilizationOfCpu()< contract.getCpuUtilizationMetric().getMaxDimension().getValue()){
                CpuUtilization =false;
            }else
                System.out.println("Cloudlet "+cl.getId()+" in Vm "+vm.getId()+" has violated the SLA in term of CpuUtilization");
            CpuUtilization =true;
            SLAviolationnumber++;
        }
        for (Cloudlet cl : vm.getCloudletScheduler().getCloudletList()){
            if(cl.getWaitingTime()< contract.getWaitTimeMetric().getMaxDimension().getValue()){
                WaitTime =false;
            }else
                System.out.println("Cloudlet "+cl.getId()+" in Vm "+vm.getId()+" has violated the SLA in term of WaitTime");
            WaitTime =true;
            SLAviolationnumber++;
        }
            if( new VmCost(vm).getTotalCost()< contract.getPriceMetric().getMaxDimension().getValue()){
                Price =false;
            }else
                System.out.println(" Vm "+vm.getId()+" has violated the SLA in term of Price");
                Price =true;
            SLAviolationnumber++;
        if(TaskCompletionTime && CpuUtilization && WaitTime && Price){return true;}else
                    return false;
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
    public MixedDatacenterBroker getBroker() { return this.broker; }
    public Datacenter getDatacenter() { return this.datacenter; }
    public List<Cloudlet> getCloudletlist() { return this.Cloudletlist; }
    public List<Integer> getVmCost(){
        final List<Integer> listcost = new ArrayList<Integer>();
        int ProcessingCost=0,MemoryCost=0,StorageCost=0,BwCost=0;
        int TotalCost=0,TotalExTime=0;

        for (int i = 0; i < this.broker.getVmCreatedList().size(); i++) {
            final VmCost cost = new VmCost(this.broker.getVmCreatedList().get(i));
            ProcessingCost += cost.getProcessingCost();
            MemoryCost += cost.getMemoryCost();
            StorageCost += cost.getStorageCost();
            BwCost += cost.getBwCost();
            TotalCost += cost.getTotalCost();
            int vmExtime= this.broker.getVmCreatedList().get(i).getTotalExecutionTime() > 0 ? 1 : 0;
            TotalExTime+=vmExtime;
        }
        this.broker.getVmCreatedList().forEach(vm -> System.out.println("Created vm Id : "+vm.getId()));
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

    public int getNumofSLAviolation(){

        return this.SLAviolationnumber;
    }
    private Optional<Host> findRandomSuitableHostForVm( VmAllocationPolicy vmAllocationPolicy,  Vm vm) {
        final List<Host> hostList = vmAllocationPolicy.getHostList();
        /* Despite the loop is bound to the number of Hosts inside the List,
         *  the index "i" is not used to get a Host at that position,
         *  but just to define that the maximum number of tries to find a
         *  suitable Host will be the number of available Hosts.*/
        for (int i = 0; i < hostList.size(); i++){
            final int randomIndex = (int)(random.sample() * hostList.size());
            final Host host = hostList.get(randomIndex);
            if(host.isSuitableForVm(vm)){
                return Optional.of(host);
            }
        }

        return Optional.empty();
    }
    private Optional<Host> FirstFitFindHostForVm( VmAllocationPolicy vmAllocationPolicy, Vm vm) {
        final List<Host> hostList = vmAllocationPolicy.getHostList();
        /* The for loop just defines the maximum number of Hosts to try.
         * When a suitable Host is found, the method returns immediately. */
        final int maxTries = hostList.size();
        for (int i = 0; i < maxTries; i++) {
            final Host host = hostList.get(lastHostIndex);
            if (host.isSuitableForVm(vm)) {
                return Optional.of(host);
            }

            /* If it gets here, the previous Host doesn't have capacity to place the VM.
             * Then, moves to the next Host.*/
            incLastHostIndex(vmAllocationPolicy);
        }

        return Optional.empty();
    }
    private void incLastHostIndex( VmAllocationPolicy vmAllocationPolicy) {
        lastHostIndex = ++lastHostIndex % vmAllocationPolicy.getHostList().size();
    }

    private Optional<Host> bestFitHostSelectionPolicy(VmAllocationPolicy allocationPolicy, Vm vm) {
        return allocationPolicy
                .getHostList()
                .stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingInt(Host::getFreePesNumber));
    }

    private void setVmallocation(int id){
        switch (id){
            case 1:{datacenter.getVmAllocationPolicy().setFindHostForVmFunction(this::findRandomSuitableHostForVm);}
            case 2:{datacenter.getVmAllocationPolicy().setFindHostForVmFunction(this::FirstFitFindHostForVm);}
            case 3:{datacenter.getVmAllocationPolicy().setFindHostForVmFunction(this::bestFitHostSelectionPolicy);}
        }
    }
}
