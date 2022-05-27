package cloudsimMixedPeEnv.core;

import cloudsimMixedPeEnv.GpuVm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTag;

/**
 * Contains Gpu-related events in the simulator.
 * private constructor.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudSimTags  {

	/**
	 * Denotes an event to submit a GpuTask for execution.
	 */
	public final static int GPU_TASK_SUBMIT = 49;

	/**
	 * Denotes an internal event in the GpuDatacenter. Updates the progress of
	 * executions.
	 */
	public final static int VGPU_DATACENTER_EVENT = 50;

	/**
	 * Denotes an event to evaluate the power consumption of a
	 * PowerGpuDatacenter}.
	 */
	public final static int GPU_VM_DATACENTER_POWER_EVENT = 51;

	/**
	 * Denotes an event to perform a {@link GpuVm} placement in a
	 */
	public final static int GPU_VM_DATACENTER_PLACEMENT = 52;

	/**
	 * Denotes an event to update GPU memory transfers.
	 */
	public final static int GPU_MEMORY_TRANSFER = 53;

	/**
	 * Denotes the return of a GpuCloudlet to the sender.
	 */
	public final static int GPU_CLOUDLET_RETURN = 54;


	public int compareTo(GpuCloudSimTag o) {
		return 0;
	}
}
