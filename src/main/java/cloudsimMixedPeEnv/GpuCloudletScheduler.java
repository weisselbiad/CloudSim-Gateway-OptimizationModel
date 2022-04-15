package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;

public interface GpuCloudletScheduler {
  //  double cloudletSubmit(CloudSim cloudsim, Cloudlet cloudlet, double fileTransferTime);

    void cloudletFinish(Object rcl);

    public boolean hasGpuTask();
	public GpuTask getNextGpuTask();
	public boolean notifyGpuTaskCompletion(GpuTask gt);
}
