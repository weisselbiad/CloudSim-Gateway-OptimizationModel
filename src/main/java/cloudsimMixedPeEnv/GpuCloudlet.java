package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.resources.Resource;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.vms.Vm;

import java.text.DecimalFormat;
import java.util.List;

/**
 * In order to represent an application with both host and device execution
 * requirements, GpuCloudlet extends {@link Cloudlet} to include a
 * {@link GpuTask}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudlet extends CloudletSimple implements Cloudlet{

	/**
	 * A tag associated with the GpuCloudlet. A tag can be used to describe the
	 * application.
	 */
	private String tag;
	private  int cloudletId;
	public long finishedSoFar = 0;

	private int userId;
	private long cloudletLength;
	private int numberOfPes;
	private int status;
	private double execStartTime;
	private double finishTime;
	private  boolean record;
	private int reservationId = -1;
	private  List<Resource> resList;
	private int index;
	private int classType;
	private StringBuffer history;
	private String newline;
	private DecimalFormat num;
	private GpuVm vm;



	/**
	 * The GPU portion of the application.
	 */
	private GpuTask gpuTask;


	public GpuCloudlet(long cloudletLength, int pesNumber, long fileSize, long outputSize, UtilizationModel CpuutilizationModel , UtilizationModel RamutilizationModel, UtilizationModel BwutilizationModel, GpuTask gpuTask) {
		super(cloudletLength, pesNumber);
		setFileSize(fileSize);
		setOutputSize(outputSize);
		setGpuTask(gpuTask);
		setUtilizationModelCpu(CpuutilizationModel);
		setUtilizationModelRam(RamutilizationModel);
		setUtilizationModelBw(BwutilizationModel);
		//setVm(vm);
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

	public void setCloudletFinishedSoFar(final long length) {
		// if length is -ve then ignore
		if (length < 0.0 || index < 0) {
			return;
		}

		final Resource res = resList.get(index);
		long finishedSoFar1 = res.getAvailableResource();
		finishedSoFar1	= length;

		if (record) {
			write("Sets the length's finished so far to " + length);
		}
	}

	public void setUserId(final int id) {
		userId = id;
		if (record) {
			write("Assigns the Cloudlet to " + CloudSim.LOGGER.getName() + " (ID #" + id + ")");
		}
	}

	public int getUserId() {
		return userId;
	}

	public Cloudlet setVm(final GpuVm vm) {
		this.vm = vm;
		return this;
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

	protected void write(final String str) {
		if (!record) {
			return;
		}

		if (num == null || history == null) { // Creates the history or
			// transactions of this Cloudlet
			newline = System.getProperty("line.separator");
			num = new DecimalFormat("#0.00#"); // with 3 decimal spaces
			history = new StringBuffer(1000);
			history.append("Time below denotes the simulation time.");
			history.append(System.getProperty("line.separator"));
			history.append("Time (sec)       Description Cloudlet #" + cloudletId);
			history.append(System.getProperty("line.separator"));
			history.append("------------------------------------------");
			history.append(System.getProperty("line.separator"));
			history.append(num.format(CloudSim.NULL.clock()));
			history.append("   Creates Cloudlet ID #" + cloudletId);
			history.append(System.getProperty("line.separator"));
		}

		history.append(num.format(CloudSim.NULL.clock()));
		history.append("   " + str + newline);
	}


}
