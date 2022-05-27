package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CustomerEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.resources.ResourceManageable;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventListener;

import java.util.List;

public abstract class ResGpuCloudlet extends CloudletExecution implements Cloudlet {

    private final GpuTask gpuTask;

	public ResGpuCloudlet(GpuCloudlet cloudlet) {
		super(cloudlet);
		this.gpuTask = cloudlet.getGpuTask();
	}

/*	public ResGpuCloudlet(GpuCloudlet cloudlet, long startTime, int duration, int reservID) {
		super(cloudlet, startTime, duration, reservID);
		this.gpuTask = cloudlet.getGpuTask();
	}*/
	
	public GpuCloudlet finishCloudlet() {
		setStatus(Cloudlet.Status.SUCCESS);
		finalizeCloudlet();
		return (GpuCloudlet) getCloudlet();
	}

	public GpuTask getGpuTask() {
		return gpuTask;
	}

	public boolean hasGpuTask() {
		if (getGpuTask() != null) {
			return true;
		}
		return false;
	}


}
// gzip -d