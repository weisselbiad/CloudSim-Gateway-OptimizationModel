package gpu;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;

import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GpuCloudletSchedulerSpaceShared} extends
 * {@link CloudletSchedulerSpaceShared} to schedule {@link GpuCloudlet}s.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudletSchedulerSpaceShared extends CloudletSchedulerSpaceShared implements GpuCloudletScheduler {

	private List<GpuTask> gpuTaskList;

	/** The number of PEs currently available for the VM using the scheduler,
	 * according to the mips share provided to it by
	 * {@link #(double, java.util.List)} method. */
	protected int currentCpus;

	/** The number of used PEs. */
	protected int usedPes;

	/**
	 * Space Shared scheduler for GpuCloudlets. Assumes all PEs have same MIPS
	 * capacity.
	 */
	public GpuCloudletSchedulerSpaceShared() {
		super();
		usedPes = 0;
		currentCpus = 0;
		setGpuTaskList(new ArrayList<GpuTask>());
	}

	@Override
	public double gpucloudletSubmit(GpuCloudlet cloudlet, double fileTransferTime) {
		// it can go to the exec list
		if ((currentCpus - usedPes) >= cloudlet.getNumberOfPes()) {
			ResGpuCloudlet rgcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
			rgcl.setCloudletStatus(GpuCloudlet.INEXEC);
			for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
				rgcl.setMachineAndPeId(0, i);
			}
			getgpuCloudletExecList().add(rgcl);
			usedPes += cloudlet.getNumberOfPes();
		} else {// no enough free PEs: go to the waiting queue
			ResGpuCloudlet rgcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
			rgcl.setCloudletStatus(GpuCloudlet.QUEUED);
			getgpuCloudletWaitingList().add(rgcl);
			return 0.0;
		}

		// calculate the expected time for cloudlet completion
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : (List<Double>)getCurrentMipsShare()) {
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}

		currentCpus = cpus;
		capacity /= cpus;

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = capacity * fileTransferTime;
		long length = cloudlet.getLength();
		length += extraSize;
		cloudlet.setLength(length);
		return cloudlet.getLength() / capacity;
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
