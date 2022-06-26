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
	private double finishTime;

	/**
	 * A tag associated with the GpuCloudlet. A tag can be used to describe the
	 * application.
	 */
	private String tag;

	/**
	 * The GPU part of the application.
	 */
	private GpuTask gpuTask;


	// //////////////////////////////////////////
	// Below are CONSTANTS attributes
	/**
	 * The Cloudlet has been created and added to the CloudletList object.
	 */
	public static final int CREATED = 0;

	/**
	 * The Cloudlet has been assigned to a CloudResource object to be executed
	 * as planned.
	 */
	public static final int READY = 1;

	/**
	 * The Cloudlet has moved to a Cloud node.
	 */
	public static final int QUEUED = 2;

	/**
	 * The Cloudlet is in execution in a Cloud node.
	 */
	public static final int INEXEC = 3;

	/**
	 * The Cloudlet has been executed successfully.
	 */
	public static final int SUCCESS = 4;

	/**
	 * The Cloudlet has failed.
	 */
	public static final int FAILED = 5;

	/**
	 * The Cloudlet has been canceled.
	 */
	public static final int CANCELED = 6;

	/**
	 * The Cloudlet has been paused. It can be resumed by changing the status
	 * into <tt>RESUMED</tt>.
	 */
	public static final int PAUSED = 7;

	/**
	 * The Cloudlet has been resumed from <tt>PAUSED</tt> state.
	 */
	public static final int RESUMED = 8;

	/**
	 * The cloudlet has failed due to a resource failure.
	 */
	public static final int FAILED_RESOURCE_UNAVAILABLE = 9;
	private int status;

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
		setgpuStatus(CREATED);
		finishTime = -1.0;    // meaning this Cloudlet hasn't finished yet

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
		setgpuStatus(CREATED);
		finishTime = -1.0;    // meaning this Cloudlet hasn't finished yet

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
	@Override
	public int getgpuStatus() {
		return status;
	}

	protected void setgpuStatus(int status) {
		this.status = status;
	}
	public static String getStatusString(final int status) {
		String statusString = null;
		switch (status) {
			case GpuCloudlet.CREATED:
				statusString = "Created";
				break;

			case GpuCloudlet.READY:
				statusString = "Ready";
				break;

			case GpuCloudlet.INEXEC:
				statusString = "InExec";
				break;

			case GpuCloudlet.SUCCESS:
				statusString = "Success";
				break;

			case GpuCloudlet.QUEUED:
				statusString = "Queued";
				break;

			case GpuCloudlet.FAILED:
				statusString = "Failed";
				break;

			case GpuCloudlet.CANCELED:
				statusString = "Canceled";
				break;

			case GpuCloudlet.PAUSED:
				statusString = "Paused";
				break;

			case GpuCloudlet.RESUMED:
				statusString = "Resumed";
				break;

			case GpuCloudlet.FAILED_RESOURCE_UNAVAILABLE:
				statusString = "Failed_resource_unavailable";
				break;

			default:
				break;
		}

		return statusString;
	}
	public void setCloudletStatus(final int newStatus) throws Exception {
		// if the new status is same as current one, then ignore the rest
		if (status == newStatus) {
			return;
		}

		// throws an exception if the new status is outside the range
		if (newStatus < GpuCloudlet.CREATED || newStatus > GpuCloudlet.FAILED_RESOURCE_UNAVAILABLE) {
			throw new Exception(
					"Cloudlet.setCloudletStatus() : Error - Invalid integer range for Cloudlet status.");
		}

		if (newStatus == GpuCloudlet.SUCCESS) {
			finishTime = getSimulation().clock();
		}



		setgpuStatus(newStatus);
	}
	public int getCloudletStatus() {
		return status;
	}
	public String getCloudletStatusString() {
		return GpuCloudlet.getStatusString(status);
	}

	public double getFinishTime() {
		return finishTime;
	}
	public double getActualCPUTime() {
		return getFinishTime() - getExecStartTime();
	}

}
