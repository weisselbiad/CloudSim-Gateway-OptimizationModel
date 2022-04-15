package cloudsimMixedPeEnv;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import cloudsimMixedPeEnv.ResCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * {@link GpuCloudletSchedulerSpaceShared} extends
 * {@link CloudletSchedulerSpaceShared} to schedule {@link GpuCloudlet}s.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudletSchedulerSpaceShared extends CloudletSchedulerSpaceShared implements GpuCloudletScheduler {

	private List<GpuTask> gpuTaskList;

	/**
	 * Space Shared scheduler for GpuCloudlets. Assumes all PEs have same MIPS
	 * capacity.
	 */
	public GpuCloudletSchedulerSpaceShared() {
		super();
		setGpuTaskList(new ArrayList<GpuTask>());
	}

	/*public double cloudletSubmit( Cloudlet cloudlet, double fileTransferTime) {
		super(cloudlet,  fileTransferTime);
		// it can go to the exec list
		long usedPes = getUsedPes();
		if (getFreePes() >= cloudlet.getNumberOfPes()) {
			ResGpuCloudlet rgcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
			rgcl.setStatus(Cloudlet.Status.INEXEC);
			for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
				rgcl.setJobId((long)i);
			}
			getCloudletExecList().add(rgcl);
			usedPes += cloudlet.getNumberOfPes();
		} else {// no enough free PEs: go to the waiting queue
			ResGpuCloudlet rgcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
			rgcl.setStatus(Cloudlet.Status.QUEUED);
			getCloudletWaitingList().add(rgcl);
			return 0.0;
		}

		// calculate the expected time for cloudlet completion
		double capacity = 0.0;
		long cpus = 0;
		long currentPes = getCurrentMipsShare().pes();
		double mips = getCurrentMipsShare().mips();
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}


		currentPes = cpus;
		capacity /= cpus;

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = capacity * fileTransferTime;
		long length = cloudlet.getLength();
		length += extraSize;
		cloudlet.setLength(length);
		return cloudlet.getLength() / capacity;
	}
*/
	@Override


	public void cloudletFinish(CloudletExecution rcl) {
		long usedPes = getUsedPes();
		ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
		if (!rgcl.hasGpuTask()) {
			super.cloudletFinish(rcl);
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
			usedPes -= rgcl.getNumberOfPes();
		}
	}

	protected List<GpuTask> getGpuTaskList() {
		return gpuTaskList;
	}

	protected void setGpuTaskList(List<GpuTask> gpuTaskList) {
		this.gpuTaskList = gpuTaskList;
	}

	@Override
	public void cloudletFinish(Object rcl) {

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
				rgcl.setStatus(GpuCloudlet.Status.SUCCESS);
				rgcl.finalizeCloudlet();
				return true;
			}
		}
		return false;
	}

}
