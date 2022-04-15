package cloudsimMixedPeEnv;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;


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
	 * Time Shared scheduler for GpuCloudlets. Assumes all PEs have same MIPS
	 * capacity.
	 */
	public GpuCloudletSchedulerTimeShared() {
		super();
		setGpuTaskList(new ArrayList<GpuTask>());
	}

	/*public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
		ResGpuCloudlet rcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
		rcl.setStatus(Cloudlet.Status.INEXEC);
		for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
			rcl.setJobId(i);
		}

		getCloudletExecList().add(rcl);

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = getAvailableMipsByPe() * fileTransferTime;
		long length = (long) (cloudlet.getLength() + extraSize);
		cloudlet.setLength(length);

		return cloudlet.getLength() / getAvailableMipsByPe();
	}*/

	@Override
	public void cloudletFinish(Object rcl) {
		ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
		if (!rgcl.hasGpuTask()) {
			super.cloudletFinish((ResGpuCloudlet)rcl);
		} else {
			GpuTask gt = rgcl.getGpuTask();
			getGpuTaskList().add(gt);
			try {
				rgcl.setStatus(GpuCloudlet.Status.PAUSED);
				getCloudletPausedList().add(rgcl);
			} catch (Exception e) {
				e.printStackTrace();
			//	CloudSim.abruptallyTerminate();
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
		for (CloudletExecution rcl : getCloudletPausedList()) {
			ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
			if (rgcl.getGpuTask() == gt) {
				rgcl.setStatus(Cloudlet.Status.SUCCESS);
				rgcl.finalizeCloudlet();
				return true;
			}
		}
		return false;
	}

}
