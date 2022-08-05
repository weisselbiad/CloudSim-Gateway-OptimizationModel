package pl.edu.agh.csg;

import cloudsimMixedPeEnv.GpuHostTags;

/**
 * This Class is basically used to initiate all settings of the components
 */

public class SimSettings {

    private int cloudletCnt = 4;
    private int gpucloudletCnt = 4;
    private int cloudletLength =10000;
    private int cloudletPes = 1;
    private long cloudletSize = 1024;

    private int hostCnt = 20;

    private int gpuhostCnt = 10;
    private long hostRam = 64 * 1024;
    private long hostBw = 50000*3;
    private long hostSize = 1000000*3;
    private int hostPes = 64;
    private long hostPeMips =20*GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;;

    private long gpumips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

    private int vmCnt= 3;
    private int gpuvmCnt = 2;
    private long vmRam= 2048;

    private long gpuvmram = 2048;
    private long vmBw = 100;

    private long gpuvmbw = 100;
    private long vmSize = 1000;

    private int gpuvmsize = 100;
    private long vmPes= 4;


  /*  public SimSettings() {
        // Host size is big enough to host a m5a.2xlarge VM

        hostPeMips = 10000;
        hostBw = 50000;
        hostRam = 65536;
        hostSize = 16000;
        hostPes = 14;
        VmCount = 10;
        datacenterHostsCnt = 300;
        cloudletsCnt = 200;
        basicVmRam = 8192;
        basicVmPeCount = 2;

        // we can have 3000 == the same as number of hosts, as every host can have 1 small, 1 medium and 1 large Vm
        maxVmsPerSize = 3000;
    }*/

    @Override
    public String toString() {
        return "SimulationSettings{" +

                "\n, cloudletsCnt"+ ":" + cloudletCnt +
                "\n, cloudletLength"+ ":" + cloudletLength +
                "\n, cloudletPes"+ ":"+ cloudletPes +
                "\n, cloudletSize"+ ":"+ cloudletSize +

                "\n, datacenterHostsCnt"+ ":" + hostCnt +
                "\n, hostPeMips"+ ":" + hostPeMips +
                "\n, hostBw"+ ":" + hostBw +
                "\n, hostRam"+ ":" + hostRam +
                "\n, hostSize"+ ":" + hostSize +
                "\n, hostPeCnt"+ ":" + hostPes +

                "\n, VmCount"+ ":" + vmCnt +
                "\n, VmRam"+ ":" + vmRam +
                "\n, VmPes"+ ":" + vmPes +
                "\n} VmBw"+ ":" + vmBw +
                "\n} VmSize"+ ":" + vmSize +""
                ;


    }

    public int getCloudletCnt() {
        return this.cloudletCnt;
    }

    public int getGpucloudletCnt(){return this.gpucloudletCnt;}
    public int getCloudletLength(){return this.cloudletLength;}
    public long getCloudletSize(){return this.cloudletSize;}
    public int getCloudletPes(){return this.cloudletPes;}

    public int getHostCnt() {return this.hostCnt;}
    public int getGpuhostCnt() {return this.gpuhostCnt;}
    public long getHostRam() {return this.hostRam;}
    public long getHostBw() {
        return this.hostBw;
    }
    public long getHostSize() {
        return this.hostSize;
    }
    public int getHostPes() { return this.hostPes; }
    public long getHostPeMips() {
        return this.hostPeMips;
    }
    public long getGpumips(){return this.gpumips;}

    public int getVmCnt() {return this.vmCnt;}
    public int getGpuvmCnt() {return this.gpuvmCnt;}
    public long getVmRam() {return this.vmRam;}
    public long getGpuvmram(){return this.gpuvmram;}
    public long getVmBw() {
        return this.vmBw;
    }
    public long getGpuvmbw(){return this.gpuvmbw;}
    public long getVmSize() {
        return this.vmSize;
    }
    public int getGpuvmsize(){return this.gpuvmsize;}
    public long getVmPes() {
        return this.vmPes;
    }

    public  long getDatacenterCores() {
        return getHostCnt() * hostPes;
    }

}
