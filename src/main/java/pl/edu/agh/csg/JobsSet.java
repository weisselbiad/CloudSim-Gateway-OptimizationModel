package pl.edu.agh.csg;

import cloudsimMixedPeEnv.GpuCloudlet;
import cloudsimMixedPeEnv.GpuTask;
import cloudsimMixedPeEnv.hardware_assisted.GridVideoCardTags;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class JobsSet {
    private static JobsSet instance;

    private  CloudSim simulation;
    public SimSettings settings = new SimSettings();
    Random rand = new Random();
    int NumofJobs;
    List<List<Cloudlet>> SeqList1 ;
    List<List<Cloudlet>> SeqList2;
    List<List<Cloudlet>> SeqList3;
    List<List<Cloudlet>> SeqList4;
    List<List<Cloudlet>> SeqList5;
    List<List<Cloudlet>> SeqList6;
    List<List<Cloudlet>> JobSeqList;

    private static final double CLOUDLET_CPU_USAGE_INCREMENT_PER_SECOND = 0.05;
    private int lastCloudletindex;
    private int lastindex;
    List<Integer> CloudletMips = Arrays.asList(5000, 40000, 30000,10000,50000);
    List<Integer> gpuCloudletMips = Arrays.asList(150000, 350000, 450000,600000,550000);
    List<Integer> CloudletSize = Arrays.asList(512,1024,1536);
    List<Integer> gpuCloudletSize = Arrays.asList(1024,2048,3096);
    Random randMips = new Random();
    Random gpurandMips = new Random();
    Random randSize = new Random();
    Random gpurandSize = new Random();

    private int cloudletPes = settings.getCloudletPes();


    private JobsSet(int NumofJobs){

        this.NumofJobs=NumofJobs;
         SeqList1 =  new ArrayList<>();
         SeqList2= new ArrayList<>();
         SeqList3= new ArrayList<>();
         SeqList4= new ArrayList<>();
         SeqList5= new ArrayList<>();
         SeqList6= new ArrayList<>();
        JobSeqList= new ArrayList<>();

        int jobs1 =rand.nextInt((NumofJobs - 5) + 1) + 5;
        for (int i = 0; i<jobs1; i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>();

                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));

            SeqList1.add(CloudletsTaskSet);
            JobSeqList.add(CloudletsTaskSet);
        }
        int jobs2= rand.nextInt(((NumofJobs -SeqList1.size())-1  ) + 1) + 1;
        for (int i = 0; i<jobs2; i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>();

                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));

            SeqList2.add(CloudletsTaskSet);
            JobSeqList.add(CloudletsTaskSet);
        }
        int jobs3 = rand.nextInt(((NumofJobs -(SeqList1.size()+SeqList2.size()))-1  ) + 1) + 1;
        for (int i = 0; i<jobs3; i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);

                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));

            SeqList3.add(CloudletsTaskSet);
            JobSeqList.add(CloudletsTaskSet);
        }
        int jobs4= rand.nextInt(((NumofJobs -(SeqList1.size()+SeqList2.size()+SeqList3.size()))-1  ) + 1) + 1;
        for (int i = 0; i<jobs4; i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);

                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));


            SeqList4.add(CloudletsTaskSet);
            JobSeqList.add(CloudletsTaskSet);
        }
        int jobs5= rand.nextInt(((NumofJobs -(SeqList1.size()+SeqList2.size()+SeqList3.size()+SeqList4.size()))-1) + 1) + 1;
        for (int i = 0; i<jobs5; i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);

                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));

            SeqList5.add(CloudletsTaskSet);
            JobSeqList.add(CloudletsTaskSet);
        }
        int jobs6=NumofJobs-(SeqList5.size()+SeqList4.size()+SeqList3.size()+SeqList2.size()+SeqList1.size());
        for (int i = 0; i<jobs6; i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);

                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));

            SeqList6.add(CloudletsTaskSet);
            JobSeqList.add(CloudletsTaskSet);
        }

    }

    public static JobsSet getInstance(int numofJobs)
    {
        if (instance == null){

            instance = new JobsSet(numofJobs);}

        return instance;
    }


  /*  public void generateCloudlets(){

        for (int i = 0; i<SeqList1.size(); i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);
            for (int j = 0; j < 4; i++) {
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
            }
            SeqList1.add(CloudletsTaskSet);
        }this.SeqList1 =SeqList1;
        for (int i = 0; i<SeqList2.size(); i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);
            for (int j = 0; j < 4; i++) {
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
            }
            SeqList2.add(CloudletsTaskSet);
        }this.SeqList2 =SeqList2;
        for (int i = 0; i<SeqList3.size(); i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);
            for (int j = 0; j < 4; i++) {
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
            }
            SeqList3.add(CloudletsTaskSet);
        }this.SeqList3 =SeqList3;
        for (int i = 0; i<SeqList4.size(); i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);
            for (int j = 0; j < 4; i++) {
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));

            }
            SeqList4.add(CloudletsTaskSet);
        }this.SeqList4 =SeqList4;
        for (int i = 0; i<SeqList5.size(); i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);
            for (int j = 0; j < 4; i++) {
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));

            }
            SeqList5.add(CloudletsTaskSet);
        }this.SeqList5 =SeqList5;
        for (int i = 0; i<SeqList6.size(); i++) {
            List<Cloudlet> CloudletsTaskSet = new ArrayList<>(4);
            for (int j = 0; j < 4; i++) {
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createGpuCloudlet(rand.nextInt((10 - 0) + 1) + 0));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));
                CloudletsTaskSet.add( createCloudlet(cloudletPes, CloudletSize.get(randSize.nextInt(CloudletSize.size()))));

            }
            SeqList6.add(CloudletsTaskSet);
        }this.SeqList6 =SeqList6;

    }*/

    private Cloudlet createCloudlet(int pes,long clSize ){
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(UtilizationModel.Unit.ABSOLUTE,0.2);
        //final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.2);
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
        return cloudlet;
    }

    private GpuCloudlet createGpuCloudlet(int gpuTaskId){
        // Cloudlet properties
        long fileSize = 1024;
        long outputSize = 1024;
        int pesNumber = 2;

        UtilizationModelDynamic cpuUtilizationModel =
                createUtilizationModel(
                        0.5,
                        1.5,
                        true);
        UtilizationModel ramUtilizationModel = new UtilizationModelDynamic(UtilizationModel.Unit.ABSOLUTE, 50);
        UtilizationModel bwUtilizationModel = new UtilizationModelDynamic(UtilizationModel.Unit.ABSOLUTE, 50);

        // GpuTask properties
        long taskLength = (long) (GridVideoCardTags.NVIDIA_K1_CARD_PE_MIPS * 150);
        //GpuTaskMips.get((int) randMips.nextLong(GpuTaskMips.size()));
        long taskInputSize = 128;
        long taskOutputSize = 128;
        long requestedGddramSize = 4 * 1024;
        int numberOfBlocks = 2;

        UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
        UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
        UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();
        GpuTask gpuTask = new GpuTask(simulation, gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
                requestedGddramSize, 0, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
        int randomlength = gpuCloudletMips.get(gpurandMips.nextInt(gpuCloudletMips.size()));
        GpuCloudlet gpuCloudlet = new GpuCloudlet(randomlength, pesNumber,fileSize, outputSize,cpuUtilizationModel,ramUtilizationModel,bwUtilizationModel, gpuTask);

        gpuCloudlet.setSizes(gpuCloudletSize.get(gpurandSize.nextInt(gpuCloudletSize.size())));
        gpuCloudlet.setId(lastCloudletindex);
        lastCloudletindex = ++lastCloudletindex;
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
    public void setSimulation(CloudSim simulation){
        this.simulation= simulation;
    }

    public List<List<Cloudlet>> getSeqList1(){
        return SeqList1;
    }
    public List<List<Cloudlet>> getSeqList2(){
        return SeqList2;
    }
    public List<List<Cloudlet>> getSeqList3(){
        return SeqList3;
    }
    public List<List<Cloudlet>> getSeqList4(){
        return SeqList4;
    }
    public List<List<Cloudlet>> getSeqList5(){
        return SeqList5;
    }
    public List<List<Cloudlet>> getSeqList6(){
        return SeqList6;
    }

    public List<List<Cloudlet>> getJobSeqList(){
        return JobSeqList;
    }


}


