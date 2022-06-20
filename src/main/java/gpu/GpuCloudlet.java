package gpu;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletAbstract;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;

import java.util.List;
import java.util.Objects;

/**
 * To represent an application with both host and device execution
 * requirements, GpuCloudlet extends {@link Cloudlet} to include a
 * {@link GpuTask}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudlet extends CloudletAbstract implements Cloudlet {
	private int userId;

	/**
	 * A tag associated with the GpuCloudlet. A tag can be used to describe the
	 * application.
	 */
	private String tag;

	/**
	 * The GPU part of the application.
	 */
	private GpuTask gpuTask;

	/**
	 * Create a GpuCloudlet. {@link Cloudlet} represents the host portion of the
	 * application while {@link GpuTask} represents the device portion.
	 * 
	 * @param cloudletLength      length of the host portion
	 * @param pesNumber           number of threads
	 * @param cloudletFileSize    size of the application
	 * @param cloudletOutputSize  size of the application when executed
	 * @param utilizationModelCpu CPU utilization model of host portion
	 * @param utilizationModelRam RAM utilization model of host portion
	 * @param utilizationModelBw  BW utilization model of host portion
	 * @param gpuTask             the device portion of the application
	 */
	public GpuCloudlet(long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, GpuTask gpuTask) {
		super(cloudletLength, pesNumber);
		userId = -1;
		setFileSize(cloudletFileSize);
		setOutputSize(cloudletOutputSize);
		setUtilizationModelCpu(utilizationModelCpu);
		setUtilizationModelRam(utilizationModelRam);
		setUtilizationModelBw(utilizationModelBw);
		setGpuTask(gpuTask);
	}

	public GpuCloudlet(long id, long cloudletLength, int pesNumber, long cloudletFileSize,
					   long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
					   UtilizationModel utilizationModelBw, GpuTask gpuTask) {
		super(id, cloudletLength, pesNumber);
		userId = -1;
		setFileSize(cloudletFileSize);
		setOutputSize(cloudletOutputSize);
		setUtilizationModelCpu(utilizationModelCpu);
		setUtilizationModelRam(utilizationModelRam);
		setUtilizationModelBw(utilizationModelBw);
		setGpuTask(gpuTask);
	}


	/**
	 * @return the device portion
	 */
	public GpuTask getGpuTask() {
		return gpuTask;
	}

	/**
	 * @param gpuTask the device portion
	 */
	protected void setGpuTask(GpuTask gpuTask) {
		this.gpuTask = gpuTask;
		if (gpuTask != null && gpuTask.getCloudlet() == null) {
			gpuTask.setCloudlet(this);
		}
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public int compareTo(final Cloudlet other) {
		if(this.equals(Objects.requireNonNull(other))) {
			return 0;
		}

		return Double.compare(getLength(), other.getLength()) +
				Long.compare(this.getId(), other.getId()) +
				this.getBroker().compareTo(other.getBroker());
	}

	public void setUserId(final int id) {
		userId = id;

	}

	/**
	 * Gets the user or owner ID of this Cloudlet.
	 *
	 * @return the user ID or <tt>-1</tt> if the user ID has not been set before
	 * @pre $none
	 * @post $result >= -1
	 */
	public int getUserId() {
		return userId;
	}
}
