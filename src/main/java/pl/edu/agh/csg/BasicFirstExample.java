package pl.edu.agh.csg;


import cloudsimMixedPeEnv.*;
import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicy;
import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicyBreadthFirst;
import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicyNull;
import cloudsimMixedPeEnv.hardware_assisted.GridVgpuSchedulerFairShare;
import cloudsimMixedPeEnv.hardware_assisted.GridVgpuTags;
import cloudsimMixedPeEnv.hardware_assisted.GridVideoCardTags;
import cloudsimMixedPeEnv.provisioners.GpuBwProvisionerShared;
import cloudsimMixedPeEnv.provisioners.GpuGddramProvisionerSimple;
import cloudsimMixedPeEnv.provisioners.VideoCardBwProvisioner;
import cloudsimMixedPeEnv.provisioners.VideoCardBwProvisionerShared;
import cloudsimMixedPeEnv.selection.PgpuSelectionPolicy;
import cloudsimMixedPeEnv.selection.PgpuSelectionPolicyNull;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyBestFit;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRoundRobin;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import cloudsimMixedPeEnv.MixedDatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import cloudsimMixedPeEnv.CloudletsTableBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;

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
public class BasicFirstExample {
    private static final int HOSTS = 1;
    private static final int HOST_PES = 8;
    private static final int GPUHOSTS = 3;


    private static final int VMS = 2;
    private static final int GPUVMS = 5;
    private static final int VM_PES = 4;

    private static final int CLOUDLETS = 10;
    private static final int GPUCLOUDLETS =6;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10000;

    private final CloudSim simulation;
    private DatacenterBroker broker0;

    private List<Vm> vmList;
    private List<GpuVm> gpuvmList;
    private List<CloudletSimple> cloudletList;
    private List<GpuCloudlet> gpucloudletList;
    private Datacenter datacenter0;
    private final ContinuousDistribution random;

    public int getClassID (Cloudlet cl){
        if(cl.getClass().toString() == "org.cloudbus.cloudsim.cloudlets.CloudletSimple"){
            return 0;
        }else if(cl.getClass().toString() == "cloudsimMixedPeEnv.GpuCloudlet"){
            return 1;
        }else
        return -1;
    }
    public static void main(String[] args) {
        new BasicFirstExample();
    }

    private BasicFirstExample() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        simulation = new CloudSim();
        random = new UniformDistr();
        datacenter0 = createDatacenter();

        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new MixedDatacenterBroker(simulation);

        vmList = createVms();
        gpuvmList = createGpuVms();

        cloudletList = createCloudlets(vmList);
        gpucloudletList = createGpuCloudlet(1,(int)broker0.getId(), gpuvmList);
        vmList.addAll(gpuvmList);
        broker0.submitVmList(vmList);
        cloudletList.addAll(gpucloudletList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        new org.cloudsimplus.builders.tables.CloudletsTableBuilder(finishedCloudlets).build();

    }


    private Datacenter createDatacenter() {
        final List<HostSimple> hostList = new ArrayList<>(HOSTS);

       for (int j = 0; j < GPUHOSTS; j++){
            GpuHost gpuhost = createGpuHost();
            hostList.add(gpuhost);
        }
        for(int i = 0; i < HOSTS; i++) {
            HostSimple host = (HostSimple) createHost();
            hostList.add(host);
        }
        VmAllocationPolicySimple randomvmAllocationPolicy = new VmAllocationPolicySimple();

        //Replaces the default method that allocates Hosts to VMs by our own implementation
        randomvmAllocationPolicy.setFindHostForVmFunction(this::findRandomSuitableHostForVm);
        final VmAllocationPolicySimple bestfitvmAllocationPolicy = new VmAllocationPolicySimple(this::bestFitHostSelectionPolicy);
        final VmAllocationPolicySimple firstfitvmAllocationPolicy = new VmAllocationPolicySimple(this::bestFitHostSelectionPolicy);


        //Uses a VmAllocationPolicySimple by default to allocate VMs
        return new DatacenterSimple(simulation, hostList, randomvmAllocationPolicy);
    }

    private Optional<Host> findRandomSuitableHostForVm(final VmAllocationPolicy vmAllocationPolicy, final Vm vm) {
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
    private Optional<Host> bestFitHostSelectionPolicy(VmAllocationPolicy allocationPolicy, Vm vm) {
        return allocationPolicy
                .getHostList()
                .stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingInt(Host::getFreePesNumber));
    }
    private Optional<Host> firstFitHostSelectionPolicy(VmAllocationPolicy allocationPolicy, Vm vm) {
        return allocationPolicy
                .getHostList()
                .stream()
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingLong(Host::getId));
    }

    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(HOST_PES);

        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(1000));
        }

        final long ram = 2048; //in Megabytes
        final long bw = 10000; //in Megabits/s
        final long storage = 1000000; //in Megabytes

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(ram, bw, storage, peList);
    }


    private GpuHost createGpuHost (){
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
        List<Pe> peList = new ArrayList<Pe>();

        // PE's MIPS power
        double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

        for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_PES; peId++) {
            // Create PEs and add these into a list.
            peList.add(0, new PeSimple(mips));
        }

        // Create Host with its id and list of PEs and add them to the list of machines
        // host memory (MB)
        long ram = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_RAM;
        // host storage
        long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
        // host BW
        long bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW;
        // Set VM Scheduler
        VmScheduler vmScheduler = new VmSchedulerTimeShared();

        // Video Card Selection Policy
        VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyBreadthFirst(videoCards);
        return new GpuHost(ram,bw, storage, peList,vmScheduler, videoCardAllocationPolicy);
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final Vm vm = new VmSimple(1000, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10000);
            list.add(vm);
        }

        return list;
    }

    private List<GpuVm> createGpuVms() {
        final List<GpuVm> gpuvmlist = new ArrayList<>(GPUVMS);
        for (int i = vmList.size(); i < GPUVMS + vmList.size(); i++) {

            // VM description
            long mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;
            // image size (GB)
            int size = 10;
            // vm memory (GB)
            long ram = 2;
            long bw = 100;
            // number of cpus
            long pesNumber = 4;
            // VMM namehttps://www.youtube.com/watch?v=Q7lVTjHt5M4
            String vmm = "vSphere";
            GpuCloudletSchedulerTimeShared GCSTS = new GpuCloudletSchedulerTimeShared();

            // Create a VM
            GpuVm vm = new GpuVm(mips, pesNumber, GCSTS);
            vm.setRam(ram);
            vm.setBw(bw);
            vm.setSize(size);
            // Create GpuTask Scheduler
            GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
            // Create a Vgpu
            Vgpu vgpu = GridVgpuTags.getK180Q(simulation,i, gpuTaskScheduler);
            vm.setVgpu(vgpu);
            vm.setId(i);
            gpuvmlist.add(vm);

        }
        return gpuvmlist;
    }
    /**
     * Creates a list of Cloudlets.
     */
    private List<CloudletSimple> createCloudlets(List<Vm> vmList){
        final List<CloudletSimple> list = new ArrayList<>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);

            for (int i = 0; i < CLOUDLETS; i++) {
                final CloudletSimple cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
                cloudlet.setSizes(1024);

                list.add(cloudlet);
            }


        return list;
    }
    private List<GpuCloudlet> createGpuCloudlet(int gpuTaskId, int brokerId, List<GpuVm> gpuvmList) {
        final List<GpuCloudlet> list = new ArrayList<>(GPUCLOUDLETS);

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

            GpuTask gpuTask = new GpuTask(simulation, gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
                    requestedGddramSize, 0, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);

            GpuCloudlet gpuCloudlet = new GpuCloudlet(length, pesNumber,fileSize, outputSize,cpuUtilizationModel,ramUtilizationModel,bwUtilizationModel, gpuTask);

            gpuCloudlet.setUserId(brokerId);
            list.add(gpuCloudlet);
            }

        return list;
    }



  /*  private void createAndSubmitCloudlets(Vm vm, double submissionDelay) {
        int cloudletId = cloudletList.size();
        List<Cloudlet> list = new ArrayList<>(NUMBER_OF_CLOUDLETS);
        for(int i = 0; i < NUMBER_OF_CLOUDLETS; i++){
            Cloudlet cloudlet = createCloudlet(cloudletId++, vm, broker);
            list.add(cloudlet);
        }

        broker.submitCloudletList(list, submissionDelay);
        cloudletList.addAll(list);
    }*/

}