package gpu;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GpuCloudletSchedulerTimeShared} extends
 * {@link CloudletSchedulerTimeShared} to schedule {@link GpuCloudlet}s.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudletSchedulerTimeShared extends CloudletSchedulerTimeShared implements GpuCloudletScheduler {

	private List<GpuTask> gpuTaskList;

	/**
	 * {@link CloudletSchedulerTimeShared} with GpuCloudlet support. Assumes all PEs have same MIPS
	 * capacity.
	 */
	public GpuCloudletSchedulerTimeShared() {
		super();
		setGpuTaskList(new ArrayList<GpuTask>());
	}

	@Override
	public double gpucloudletSubmit(GpuCloudlet cloudlet, double fileTransferTime) {
		ResGpuCloudlet rcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
		rcl.setCloudletStatus(GpuCloudlet.INEXEC);
		for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
			rcl.setMachineAndPeId(0, i);
		}

		getgpuCloudletExecList().add(rcl);

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = getAvailableMipsByPe() * fileTransferTime;
		long length = (long) (cloudlet.getLength() + extraSize);
		cloudlet.setLength(length);

		return cloudlet.getLength() / getAvailableMipsByPe();
	}

	@Override
	public void gpucloudletFinish(ResCloudlet rcl) {
		ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
		if (!rgcl.hasGpuTask()) {
			gpucloudletFinish(rcl);
		} else {
			GpuTask gt = rgcl.getGpuTask();
			getGpuTaskList().add(gt);
			try {
				rgcl.setCloudletStatus(GpuCloudlet.PAUSED);
				getgpuCloudletPausedList().add(rgcl);
			} catch (Exception e) {
				e.printStackTrace();
				((ResGpuCloudlet) rcl).simulation.abort();
			}
		}
	}

	protected List<GpuTask> getGpuTaskList() {
		return gpuTaskList;
	}

	protected void setGpuTaskList(List<GpuTask> gpuTaskList) {
		this.gpuTaskList = gpuTaskList;
	}

	@Override
	public boolean hasGpuTask() {
		return !getGpuTaskList().isEmpty();
	}

	@Override
	public GpuTask getNextGpuTask() {
		if (hasGpuTask()) {
			return getGpuTaskList().remove(0);
		}
		return null;
	}

	@Override
	public boolean notifyGpuTaskCompletion(GpuTask gt) {
		for (ResCloudlet rcl : getgpuCloudletPausedList()) {
			ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
			if (rgcl.getGpuTask() == gt) {
				rgcl.setCloudletStatus(GpuCloudlet.SUCCESS);
				rgcl.finalizeCloudlet();
				return true;
			}
		}
		return false;
	}

}
