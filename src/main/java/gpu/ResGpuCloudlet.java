package gpu;

import cloudsimMixedPeEnv.Consts;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.core.CloudSim;

public class ResGpuCloudlet extends CloudletExecution {


	/** The Cloudlet arrival time for the first time. */
	private double arrivalTime;

	/** The estimation of Cloudlet finished time. */
	private double finishedTime;

	/** The length of Cloudlet finished so far. */
	private long cloudletFinishedSoFar;

	/**
	 * Cloudlet execution start time. This attribute will only hold the latest time since a Cloudlet
	 * can be canceled, paused or resumed.
	 */
	private double startExecTime;

	/** The total time to complete this Cloudlet. */
	private double totalCompletionTime;

	// The below attributes are only to be used by the SpaceShared policy.

	/** The machine id this Cloudlet is assigned to. */
	private int machineId;

	/** The Pe id this Cloudlet is assigned to. */
	private int peId;

	/** The an array of machine IDs. */
	private int[] machineArrayId = null;

	/** The an array of Pe IDs. */
	private int[] peArrayId = null;

	/** The index of machine and Pe arrays. */
	private int index;

	// NOTE: Below attributes are related to AR stuff

	/** The Constant NOT_FOUND. */
	private static final int NOT_FOUND = -1;

	/** The reservation id. */
	private final int reservId;

	/** The num Pe needed to execute this Cloudlet. */
	private int pesNumber;
	private final GpuTask gpuTask;

	CloudSim simulation = (CloudSim) cloudlet.getSimulation();


	public ResGpuCloudlet(GpuCloudlet cloudlet) {
		super(cloudlet);

		this.cloudlet = cloudlet;
		this.gpuTask = cloudlet.getGpuTask();
		this.pesNumber= (int) cloudlet.getNumberOfPes();
		reservId = NOT_FOUND;
		init();
	}

	public ResGpuCloudlet(GpuCloudlet cloudlet, int reservID) {
		super(cloudlet);
		reservId = reservID;
		this.cloudlet = cloudlet;
		this.gpuTask = cloudlet.getGpuTask();
		this.pesNumber= (int) cloudlet.getNumberOfPes();

		init();
	}



	public GpuCloudlet finishCloudlet() {
		setStatus(GpuCloudlet.Status.SUCCESS);
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
	public long getStartTime() {
		return (long) cloudlet.getExecStartTime();
	}

	/**
	 * Gets the reservation duration time.
	 *
	 * @return reservation duration time
	 * @pre $none
	 * @post $none
	 */
	public int getDurationTime() {
		return (int)cloudlet.getFinishTime();
	}

	/**
	 * Gets the number of PEs required to execute this Cloudlet.
	 *
	 * @return number of Pe
	 * @pre $none
	 * @post $none
	 */
	public long getNumberOfPes() {
		return pesNumber;
	}

	/**
	 * Gets the reservation ID that owns this Cloudlet.
	 *
	 * @return a reservation ID
	 * @pre $none
	 * @post $none
	 */
	public int getReservationID() {
		return reservId;
	}

	/**
	 * Checks whether this Cloudlet is submitted by reserving or not.
	 *
	 * @return <tt>true</tt> if this Cloudlet has reserved before, <tt>false</tt> otherwise
	 * @pre $none
	 * @post $none
	 */
	public boolean hasReserved() {
		if (reservId == NOT_FOUND) {
			return false;
		}

		return true;
	}

	/**
	 * Initialises all local attributes.
	 *
	 * @pre $none
	 * @post $none
	 */
	private void init() {
		// get number of PEs required to run this Cloudlet
		pesNumber = (int) cloudlet.getNumberOfPes();

		// if more than 1 Pe, then create an array
		if (pesNumber > 1) {
			machineArrayId = new int[pesNumber];
			peArrayId = new int[pesNumber];
		}

		arrivalTime = simulation.clock();
		cloudlet.setSubmissionDelay(arrivalTime);

		// default values
		finishedTime = NOT_FOUND;  // Cannot finish in this hourly slot.
		machineId = NOT_FOUND;
		peId = NOT_FOUND;
		index = 0;
		totalCompletionTime = 0.0;
		startExecTime = 0.0;

		// In case a Cloudlet has been executed partially by some other grid
		// hostList.
		cloudletFinishedSoFar = cloudlet.getFinishedLengthSoFar() * Consts.MILLION;
	}

	/**
	 * Gets this Cloudlet entity Id.
	 *
	 * @return the Cloudlet entity Id
	 * @pre $none
	 * @post $none
	 */
	public long getCloudletId() {
		return cloudlet.getId();
	}

	/**
	 * Gets the Cloudlet's length.
	 *
	 * @return Cloudlet's length
	 * @pre $none
	 * @post $none
	 */
	public long getCloudletLength() {
		return cloudlet.getLength();
	}

	/**
	 * Gets the total Cloudlet's length (across all PEs).
	 *
	 * @return total Cloudlet's length
	 * @pre $none
	 * @post $none
	 */
	public long getCloudletTotalLength() {
		return cloudlet.getTotalLength();
	}

	/**
	 * Sets the Cloudlet status.
	 *
	 * @param status the Cloudlet status
	 * @return <tt>true</tt> if the new status has been set, <tt>false</tt> otherwise
	 * @pre status >= 0
	 * @post $none
	 */
	public boolean setCloudletStatus(Cloudlet.Status status) {

		// gets Cloudlet's previous status
		Cloudlet.Status prevStatus = cloudlet.getStatus();

		// if the status of a Cloudlet is the same as last time, then ignore
		if (prevStatus == status) {
			return false;
		}

		boolean success = true;
		try {
			double clock = simulation.clock();   // gets the current clock

			// sets Cloudlet's current status
			cloudlet.setStatus(status);

			// if a previous Cloudlet status is INEXEC
			if (prevStatus == Cloudlet.Status.INEXEC) {
				// and current status is either CANCELED, PAUSED or SUCCESS
				if (status == Cloudlet.Status.CANCELED || status == Cloudlet.Status.PAUSED || status == Cloudlet.Status.SUCCESS) {
					// then update the Cloudlet completion time
					totalCompletionTime += (clock - startExecTime);
					index = 0;
					return true;
				}
			}

			if (prevStatus == Cloudlet.Status.RESUMED && status == Cloudlet.Status.SUCCESS) {
				// then update the Cloudlet completion time
				totalCompletionTime += (clock - startExecTime);
				return true;
			}

			// if a Cloudlet is now in execution
			if (status == Cloudlet.Status.INEXEC || (prevStatus == Cloudlet.Status.PAUSED && status == Cloudlet.Status.RESUMED)) {
				startExecTime = clock;
				cloudlet.setExecStartTime(startExecTime);
			}

		} catch (Exception e) {
			success = false;
		}

		return success;
	}

	/**
	 * Gets the Cloudlet's execution start time.
	 *
	 * @return Cloudlet's execution start time
	 * @pre $none
	 * @post $none
	 */
	public double getExecStartTime() {
		return cloudlet.getExecStartTime();
	}

	/**
	 * Sets the machine and Pe (Processing Element) ID.
	 *
	 * @param machineId machine ID
	 * @param peId Pe ID
	 * @pre machineID >= 0
	 * @pre peID >= 0
	 * @post $none
	 *
	 * @todo the machineId param and attribute mean a VM or a PM id?
	 * Only the term machine is ambiguous.

	 * it is stated it is a VM.
	 */
	public void setMachineAndPeId(int machineId, int peId) {
		// if this job only requires 1 Pe
		this.machineId = machineId;
		this.peId = peId;

		// if this job requires many PEs
		if (peArrayId != null && pesNumber > 1) {
			machineArrayId[index] = machineId;
			peArrayId[index] = peId;
			index++;
		}
	}

	/**
	 * Gets machine ID.
	 *
	 * @return machine ID or <tt>-1</tt> if it is not specified before
	 * @pre $none
	 * @post $result >= -1
	 */
	public int getMachineId() {
		return machineId;
	}

	/**
	 * Gets Pe ID.
	 *
	 * @return Pe ID or <tt>-1</tt> if it is not specified before
	 * @pre $none
	 * @post $result >= -1
	 */
	public int getPeId() {
		return peId;
	}

	/**
	 * Gets a list of Pe IDs. <br>
	 * NOTE: To get the machine IDs corresponding to these Pe IDs, use {@link #getMachineIdList()}.
	 *
	 * @return an array containing Pe IDs.
	 * @pre $none
	 * @post $none
	 */
	public int[] getPeIdList() {
		return peArrayId;
	}

	/**
	 * Gets a list of Machine IDs. <br>
	 * NOTE: To get the Pe IDs corresponding to these machine IDs, use {@link #getPeIdList()}.
	 *
	 * @return an array containing Machine IDs.
	 * @pre $none
	 * @post $none
	 */
	public int[] getMachineIdList() {
		return machineArrayId;
	}

	/**
	 * Gets the remaining cloudlet length that has to be execute yet,
	 * considering the {@link #getCloudletTotalLength()}.
	 *
	 * @return cloudlet length
	 * @pre $none
	 * @post $result >= 0
	 */
	public long getRemainingCloudletLength() {
		long length = cloudlet.getTotalLength() * Consts.MILLION - cloudletFinishedSoFar;

		// Remaining Cloudlet length can't be negative number.
		if (length < 0) {
			return 0;
		}

		return (long) Math.floor(length / Consts.MILLION);
	}

	/**
	 * Finalizes all relevant information before <tt>exiting</tt> the CloudResource entity. This
	 * method sets the final data of:
	 * <ul>
	 * <li>wall clock time, i.e. the time of this Cloudlet resides in a CloudResource (from arrival
	 * time until departure time).
	 * <li>actual CPU time, i.e. the total execution time of this Cloudlet in a CloudResource.
	 * <li>Cloudlet's finished time so far
	 * </ul>
	 *
	 * @pre $none
	 * @post $none
	 */
	public void finalizeCloudlet() {
		// Sets the wall clock time and actual CPU time
		double wallClockTime = simulation.clock() - arrivalTime;
		cloudlet.setWallClockTime(wallClockTime, totalCompletionTime);

		long finished = 0;
		//if (cloudlet.getCloudletTotalLength() * Consts.MILLION < cloudletFinishedSoFar) {
		if (cloudlet.getStatus()== Cloudlet.Status.SUCCESS) {
			finished = cloudlet.getLength();
		} else {
			finished = cloudletFinishedSoFar / Consts.MILLION;
		}

		cloudlet. addFinishedLengthSoFar(finished);
	}

	/**
	 * Updates the length of cloudlet that has already been completed.
	 *
	 * @param miLength cloudlet length in Instructions (I)
	 * @pre miLength >= 0.0
	 * @post $none
	 */
	public void updateCloudletFinishedSoFar(long miLength) {
		cloudletFinishedSoFar += miLength;
	}

	/**
	 * Gets arrival time of a cloudlet.
	 *
	 * @return arrival time
	 * @pre $none
	 * @post $result >= 0.0
	 *
	 * @todo It is being used different words for the same term.
	 * Here it is used arrival time while at Resource inner classe of the Cloudlet class
	 * it is being used submissionTime. It needs to be checked if they are
	 * the same term or different ones in fact.
	 */
	public double getCloudletArrivalTime() {
		return arrivalTime;
	}

	/**
	 * Sets the finish time for this Cloudlet. If time is negative, then it is being ignored.
	 *
	 * @param time finish time
	 * @pre time >= 0.0
	 * @post $none
	 */
	public void setFinishTime(double time) {
		if (time < 0.0) {
			return;
		}

		finishedTime = time;
	}

	/**
	 * Gets the Cloudlet's finish time.
	 *
	 * @return finish time of a cloudlet or <tt>-1.0</tt> if it cannot finish in this hourly slot
	 * @pre $none
	 * @post $result >= -1.0
	 */
	public double getClouddletFinishTime() {
		return finishedTime;
	}


	/**
	 * Gets the Cloudlet status.
	 *
	 * @return Cloudlet status
	 * @pre $none
	 * @post $none
	 */
	public Cloudlet.Status getCloudletStatus() {
		return cloudlet.getStatus();
	}

	/**
	 * Get am Unique Identifier (UID) of the cloudlet.
	 *
	 * @return The UID
	 */
	//public String getUid() {		return getUserId() + "-" + getCloudletId();	}



}
