package pl.edu.agh.csg;

import org.cloudbus.cloudsim.core.CloudSim;

public class SimSettings {
    private int cloudletCnt = 10;
    private int cloudletLength =10_000;
    private int cloudletPes = 2;
    private long cloudletSize = 1024;

    private int hostCnt = 20;
    private long hostRam = 2048;
    private long hostBw = 10000;
    private long hostSize = 1000000;
    private int hostPes = 8;
    private long hostPeMips = 1000;

    private int vmCnt= 10;
    private long vmRam= 512;
    private long vmBw = 1000;
    private long vmSize = 10000;
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

                "\n, hostPeMips=" + hostPeMips +
                "\n, hostBw=" + hostBw +
                "\n, hostRam=" + hostRam +
                "\n, hostSize=" + hostSize +
                "\n, hostPeCnt=" + hostPes +
                "\n, defaultInitialVmCount=" + vmCnt +
                "\n, datacenterHostsCnt=" + hostCnt +
                "\n, cloudletsCnt=" + cloudletCnt +
                "\n, basicVmRam=" + vmRam +
                "\n, basicVmPeCount=" + vmPes +
                "\n}";
    }

    public int getCloudletCnt() {
        return this.cloudletCnt;
    }
    public int getCloudletLength(){return this.cloudletLength;}
    public long getCloudletSize(){return this.cloudletSize;}
    public int getCloudletPes(){return this.cloudletPes;}

    public int getHostCnt() {return this.hostCnt;}
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

    public int getVmCnt() {return this.vmCnt;}
    public long getVmRam() {return this.vmRam;}
    public long getVmBw() {
        return this.vmBw;
    }
    public long getVmSize() {
        return this.vmSize;
    }
    public long getVmPes() {
        return this.vmPes;
    }

    public  long getDatacenterCores() {
        return getHostCnt() * hostPes;
    }





}
