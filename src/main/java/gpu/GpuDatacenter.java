package gpu;

import gpu.core.CloudSimTags;
import gpu.core.GpuCloudSimTags;
import gpu.core.Log;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.resources.DatacenterStorage;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.cloudbus.cloudsim.core.CloudSimTag.CLOUDLET_RETURN;

/**
 * {@link GpuDatacenter} extends {@link Datacenter} to support
 * {@link GpuCloudlet}s as well as the memory transfer between CPU and GPU.
 *
 * @author Ahmad Siavashi
 *
 */
public class GpuDatacenter extends CloudSimEntity implements Datacenter {
	private List<DatacenterStorage> storageList;
	private DatacenterCharacteristics characteristics;
	private double gpuTaskLastProcessTime;

	private Map<GpuTask, ResGpuCloudlet> gpuTaskResGpuCloudletMap;
	public Simulation simulation;
	/**
	 * See {@link Datacenter#}
	 */
	public GpuDatacenter(String name, Simulation simulation, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
						 List<DatacenterStorage> storageList, double schedulingInterval) throws Exception {
		super(simulation);
		setName(name);
		addHostList(getHostList());
		setSchedulingInterval(schedulingInterval);
		setCharacteristics(characteristics);
		setVmAllocationPolicy(vmAllocationPolicy);
		setStorageList(storageList);
		setGpuTaskLastProcessTime(0.0);
		setGpuTaskResGpuCloudletMap(new HashMap<>());
		this.simulation =simulation;
	}


	protected void processOtherEvent(SimEvent ev) {
		switch (ev.getTag()) {
			case GPU_MEMORY_TRANSFER:
				processGpuMemoryTransfer(ev);
				break;
			case GPU_TASK_SUBMIT:
				processGpuTaskSubmit(ev);
				break;
			case GPU_CLOUDLET_RETURN:
				processGpuCloudletReturn(ev);
				break;
			case VGPU_DATACENTER_EVENT:
				updateGpuTaskProcessing();
				checkGpuTaskCompletion();
				break;
			default:
				processOtherEvent1(ev);
				break;
		}
	}

	protected GpuVm getGpuTaskVm(GpuTask gt) {
		//int userId = gt.getCloudlet().getUserId();
		int vmId = (int) gt.getCloudlet().getVm().getId();

		GpuHost host = (GpuHost) getVmAllocationPolicy().getDatacenter().getHost(vmId);
		GpuVm vm = (GpuVm) host.getVmList().get(vmId);

		return vm;
	}

	protected void notifyGpuTaskCompletion(GpuTask gt) {
		GpuVm vm = getGpuTaskVm(gt);
		GpuCloudletScheduler scheduler = (GpuCloudletScheduler) vm.getCloudletScheduler();
		scheduler.notifyGpuTaskCompletion(gt);
	}

	protected void processGpuCloudletReturn(SimEvent ev) {
		GpuCloudlet cloudlet = (GpuCloudlet) ev.getData();
		sendNow(ev.getSource(), CLOUDLET_RETURN, cloudlet);
		notifyGpuTaskCompletion(cloudlet.getGpuTask());
	}

	protected void processGpuMemoryTransfer(SimEvent ev) {
		GpuTask gt = (GpuTask) ev.getData();

		double bandwidth = Double.valueOf(BusTags.PCI_E_3_X16_BW);

		if (gt.getStatus() == GpuTask.CREATED) {
			double delay = gt.getTaskInputSize() / bandwidth;
			send(ev.getSource(), delay, CloudSimTag.GPU_TASK_SUBMIT, gt);
		} else if (gt.getStatus() == GpuTask.SUCCESS) {
			double delay = gt.getTaskOutputSize() / bandwidth;
			send(ev.getSource(), delay, CloudSimTag.GPU_CLOUDLET_RETURN, gt.getCloudlet());
		}
	}

	protected void updateGpuTaskProcessing() {
		// if some time passed since last processing
		// R: for term is to allow loop at simulation start. Otherwise, one initial
		// simulation step is skipped and schedulers are not properly initialized
		if (simulation.clock() < 0.111
				|| simulation.clock() > geGpuTasktLastProcessTime() + simulation.getMinTimeBetweenEvents()) {
			List<? extends Host> list = getVmAllocationPolicy().getHostList();
			double smallerTime = Double.MAX_VALUE;
			// for each host...
			for (int i = 0; i < list.size(); i++) {
				GpuHost host = (GpuHost) list.get(i);
				// inform VMs to update processing
				double time = host.updateVgpusProcessing(simulation.clock());
				// what time do we expect that the next task will finish?
				if (time < smallerTime) {
					smallerTime = time;
				}
			}
			// guarantees a minimal interval before scheduling the event
			if (smallerTime < simulation.clock() + simulation.getMinTimeBetweenEvents() + 0.01) {
				smallerTime = simulation.clock() + simulation.getMinTimeBetweenEvents() + 0.01;
			}
			if (smallerTime != Double.MAX_VALUE) {
				schedule( (smallerTime - simulation.clock()), CloudSimTag.VGPU_DATACENTER_EVENT);
			}
			setGpuTaskLastProcessTime(simulation.clock());
		}
	}

	protected void checkGpuTaskCompletion() {
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			GpuHost host = (GpuHost) list.get(i);
			for (Vm vm : host.getVmList()) {
				GpuVm gpuVm = (GpuVm) vm;
				Vgpu vgpu = gpuVm.getVgpu();
				if (vgpu != null) {
					while (vgpu.getGpuTaskScheduler().hasFinishedTasks()) {
						ResGpuTask rgt = vgpu.getGpuTaskScheduler().getNextFinishedTask();
						try {
							sendNow( (SimEntity) getSimulation(), CloudSimTag.GPU_MEMORY_TRANSFER, rgt.getGpuTask());
						} catch (Exception e) {
							e.printStackTrace();
							simulation.abort();
						}
					}
				}
			}
		}
	}

	protected void checkCloudletCompletion() {
		firstcheckCloudletCompletion();
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				GpuCloudletScheduler scheduler = (GpuCloudletScheduler) vm.getCloudletScheduler();
				while (scheduler.hasGpuTask()) {
					GpuTask gt = scheduler.getNextGpuTask();
					sendNow((SimEntity) getSimulation(), CloudSimTag.GPU_MEMORY_TRANSFER, gt);
				}
			}
		}
	}

	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		Vm vm = (Vm) ev.getData();
		Log.printLine(simulation.clock() + ": Trying to Create VM #" + vm.getId() + " in " + getName());

		boolean result = getVmAllocationPolicy().allocateHostForVm(vm);

		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();

			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
		}

		if (result) {
			getVmList().add(vm);
			GpuVm gpuVm = (GpuVm) vm;
			Vgpu vgpu = gpuVm.getVgpu();

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}

			vm.updateVmProcessing(CloudSim.clock(),
					getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));

			if (vgpu != null) {
				if (vgpu.isBeingInstantiated()) {
					vgpu.setBeingInstantiated(false);
				}

				VideoCard videoCard = vgpu.getVideoCard();
				vgpu.updateGpuTaskProcessing(CloudSim.clock(),
						videoCard.getVgpuScheduler().getAllocatedMipsForVgpu(vgpu));
			}
		}

	}

	@Override
	protected void processVmDestroy(SimEvent ev, boolean ack) {
		GpuVm vm = (GpuVm) ev.getData();
		if (vm.hasVgpu()) {
			((GpuVmAllocationPolicy) getVmAllocationPolicy()).deallocateGpuForVgpu(vm.getVgpu());
		}
		super.processVmDestroy(ev, ack);
	}

	protected void processGpuTaskSubmit(SimEvent ev) {
		updateGpuTaskProcessing();

		try {
			// gets the task object
			GpuTask gt = (GpuTask) ev.getData();

			// TODO: checks whether this task has finished or not

			// process this task to this CloudResource
			gt.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(),
					getCharacteristics().getCostPerBw());

			GpuVm vm = getGpuTaskVm(gt);
			Vgpu vgpu = vm.getVgpu();

			GpuTaskScheduler scheduler = vgpu.getGpuTaskScheduler();

			double estimatedFinishTime = scheduler.taskSubmit(gt);

			// if this task is in the exec queue
			if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
				send(getId(), estimatedFinishTime, GpuCloudSimTags.VGPU_DATACENTER_EVENT);
			}

		} catch (ClassCastException c) {
			Log.printLine(getName() + ".processGpuTaskSubmit(): " + "ClassCastException error.");
			c.printStackTrace();
		} catch (Exception e) {
			Log.printLine(getName() + ".processGpuTaskSubmit(): " + "Exception error.");
			e.printStackTrace();
			System.exit(-1);
		}

		checkGpuTaskCompletion();
	}

	protected double geGpuTasktLastProcessTime() {
		return gpuTaskLastProcessTime;
	}

	protected void setGpuTaskLastProcessTime(double lastGpuTaskProcessTime) {
		this.gpuTaskLastProcessTime = lastGpuTaskProcessTime;
	}

	public Map<GpuTask, ResGpuCloudlet> getGpuTaskResGpuCloudletMap() {
		return gpuTaskResGpuCloudletMap;
	}

	protected void setGpuTaskResGpuCloudletMap(Map<GpuTask, ResGpuCloudlet> gpuTaskResGpuCloudletMap) {
		this.gpuTaskResGpuCloudletMap = gpuTaskResGpuCloudletMap;
	}


		/** The characteristics. */
		private DatacenterCharacteristics characteristics;

		/** The regional Cloud Information Service (CIS) name.
		 * @see org.cloudbus.cloudsim.core.CloudInformationService
		 */
		private String regionalCisName;

		/** The vm provisioner. */
		private VmAllocationPolicy vmAllocationPolicy;

		/** The last time some cloudlet was processed in the datacenter. */
		private double lastProcessTime;

		/** The storage list. */
		private List<DatacenterStorage> storageList;

		/** The vm list. */
		private List<? extends Vm> vmList;

		/** The scheduling delay to process each datacenter received event. */
		private double schedulingInterval;


		/**
		 * Overrides this method when making a new and different type of resource. <br>
		 * <b>NOTE:</b> You do not need to override {@link #body()} method, if you use this method.
		 *
		 * @pre $none
		 * @post $none
		 *
		 * @todo This method doesn't appear to be used
		 */
		protected void registerOtherEntity() {
			// empty. This should be override by a child class
		}

		@Override
		public void processEvent(SimEvent ev) {
			int srcId = -1;

			switch (ev.getTag()) {
				// Resource characteristics inquiry
				case CloudSimTags.RESOURCE_CHARACTERISTICS:
					srcId = ((Integer) ev.getData()).intValue();
					sendNow(srcId, ev.getTag(), getCharacteristics());
					break;

				// Resource dynamic info inquiry
				case CloudSimTags.RESOURCE_DYNAMICS:
					srcId = ((Integer) ev.getData()).intValue();
					sendNow(srcId, ev.getTag(), 0);
					break;

				case CloudSimTags.RESOURCE_NUM_PE:
					srcId = ((Integer) ev.getData()).intValue();
					int numPE = getCharacteristics().getNumberOfPes();
					sendNow(srcId, ev.getTag(), numPE);
					break;

				case CloudSimTags.RESOURCE_NUM_FREE_PE:
					srcId = ((Integer) ev.getData()).intValue();
					int freePesNumber = getCharacteristics().getNumberOfFreePes();
					sendNow(srcId, ev.getTag(), freePesNumber);
					break;

				// New Cloudlet arrives
				case CloudSimTags.CLOUDLET_SUBMIT:
					processCloudletSubmit(ev, false);
					break;

				// New Cloudlet arrives, but the sender asks for an ack
				case CloudSimTags.CLOUDLET_SUBMIT_ACK:
					processCloudletSubmit(ev, true);
					break;

				// Cancels a previously submitted Cloudlet
				case CloudSimTags.CLOUDLET_CANCEL:
					processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
					break;

				// Pauses a previously submitted Cloudlet
				case CloudSimTags.CLOUDLET_PAUSE:
					processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
					break;

				// Pauses a previously submitted Cloudlet, but the sender
				// asks for an acknowledgement
				case CloudSimTags.CLOUDLET_PAUSE_ACK:
					processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
					break;

				// Resumes a previously submitted Cloudlet
				case CloudSimTags.CLOUDLET_RESUME:
					processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
					break;

				// Resumes a previously submitted Cloudlet, but the sender
				// asks for an acknowledgement
				case CloudSimTags.CLOUDLET_RESUME_ACK:
					processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
					break;

				// Moves a previously submitted Cloudlet to a different resource
				case CloudSimTags.CLOUDLET_MOVE:
					processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
					break;

				// Moves a previously submitted Cloudlet to a different resource
				case CloudSimTags.CLOUDLET_MOVE_ACK:
					processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
					break;

				// Checks the status of a Cloudlet
				case CloudSimTags.CLOUDLET_STATUS:
					processCloudletStatus(ev);
					break;

				// Ping packet
				case CloudSimTags.INFOPKT_SUBMIT:
					processPingRequest(ev);
					break;

				case CloudSimTags.VM_CREATE:
					processVmCreate(ev, false);
					break;

				case CloudSimTags.VM_CREATE_ACK:
					processVmCreate(ev, true);
					break;

				case CloudSimTags.VM_DESTROY:
					processVmDestroy(ev, false);
					break;

				case CloudSimTags.VM_DESTROY_ACK:
					processVmDestroy(ev, true);
					break;

				case CloudSimTags.VM_MIGRATE:
					processVmMigrate(ev, false);
					break;

				case CloudSimTags.VM_MIGRATE_ACK:
					processVmMigrate(ev, true);
					break;

				case CloudSimTags.VM_DATA_ADD:
					processDataAdd(ev, false);
					break;

				case CloudSimTags.VM_DATA_ADD_ACK:
					processDataAdd(ev, true);
					break;

				case CloudSimTags.VM_DATA_DEL:
					processDataDelete(ev, false);
					break;

				case CloudSimTags.VM_DATA_DEL_ACK:
					processDataDelete(ev, true);
					break;

				case CloudSimTags.VM_DATACENTER_EVENT:
					updateCloudletProcessing();
					checkCloudletCompletion();
					break;

				// other unknown tags are processed by this method
				default:
					processOtherEvent1(ev);
					break;
			}
		}

		/**
		 * Process a file deletion request.
		 *
		 * @param ev information about the event just happened
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 */
		protected void processDataDelete(SimEvent ev, boolean ack) {
			if (ev == null) {
				return;
			}

			Object[] data = (Object[]) ev.getData();
			if (data == null) {
				return;
			}

			String filename = (String) data[0];
			int req_source = ((Integer) data[1]).intValue();
			int tag = -1;

			// check if this file can be deleted (do not delete is right now)
			int msg = deleteFileFromStorage(filename);
			if (msg == DataCloudTags.FILE_DELETE_SUCCESSFUL) {
				tag = DataCloudTags.CTLG_DELETE_MASTER;
			} else { // if an error occured, notify user
				tag = DataCloudTags.FILE_DELETE_MASTER_RESULT;
			}

			if (ack) {
				// send back to sender
				Object pack[] = new Object[2];
				pack[0] = filename;
				pack[1] = Integer.valueOf(msg);

				sendNow(req_source, tag, pack);
			}
		}

		/**
		 * Process a file inclusion request.
		 *
		 * @param ev information about the event just happened
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 */
		protected void processDataAdd(SimEvent ev, boolean ack) {
			if (ev == null) {
				return;
			}

			Object[] pack = (Object[]) ev.getData();
			if (pack == null) {
				return;
			}

			File file = (File) pack[0]; // get the file
			file.setMasterCopy(true); // set the file into a master copy
			int sentFrom = ((Integer) pack[1]).intValue(); // get sender ID

			/******
			 * // DEBUG Log.printLine(super.get_name() + ".addMasterFile(): " + file.getName() +
			 * " from " + CloudSim.getEntityName(sentFrom));
			 *******/

			Object[] data = new Object[3];
			data[0] = file.getName();

			int msg = addFile(file); // add the file

			if (ack) {
				data[1] = Integer.valueOf(-1); // no sender id
				data[2] = Integer.valueOf(msg); // the result of adding a master file
				sendNow(sentFrom, DataCloudTags.FILE_ADD_MASTER_RESULT, data);
			}
		}

		/**
		 * Processes a ping request.
		 *
		 * @param ev information about the event just happened
		 *
		 * @pre ev != null
		 * @post $none
		 */
		protected void processPingRequest(SimEvent ev) {
			InfoPacket pkt = (InfoPacket) ev.getData();
			pkt.setTag(CloudSimTags.INFOPKT_RETURN);
			pkt.setDestId(pkt.getSrcId());

			// sends back to the sender
			sendNow(pkt.getSrcId(), CloudSimTags.INFOPKT_RETURN, pkt);
		}

		/**
		 * Process the event for an User/Broker who wants to know the status of a Cloudlet. This
		 * Datacenter will then send the status back to the User/Broker.
		 *
		 * @param ev information about the event just happened
		 *
		 * @pre ev != null
		 * @post $none
		 */
		protected void processCloudletStatus(SimEvent ev) {
			int cloudletId = 0;
			int userId = 0;
			int vmId = 0;
			int status = -1;

			try {
				// if a sender using cloudletXXX() methods
				int data[] = (int[]) ev.getData();
				cloudletId = data[0];
				userId = data[1];
				vmId = data[2];

				status = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId,userId).getCloudletScheduler()
						.getCloudletStatus(cloudletId);
			}

			// if a sender using normal send() methods
			catch (ClassCastException c) {
				try {
					Cloudlet cl = (Cloudlet) ev.getData();
					cloudletId = cl.getCloudletId();
					userId = cl.getUserId();

					status = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId,userId)
							.getCloudletScheduler().getCloudletStatus(cloudletId);
				} catch (Exception e) {
					Log.printConcatLine(getName(), ": Error in processing CloudSimTags.CLOUDLET_STATUS");
					Log.printLine(e.getMessage());
					return;
				}
			} catch (Exception e) {
				Log.printConcatLine(getName(), ": Error in processing CloudSimTags.CLOUDLET_STATUS");
				Log.printLine(e.getMessage());
				return;
			}

			int[] array = new int[3];
			array[0] = getId();
			array[1] = cloudletId;
			array[2] = status;

			int tag = CloudSimTags.CLOUDLET_STATUS;
			sendNow(userId, tag, array);
		}

		/**
		 * Process non-default received events that aren't processed by
		 * the {@link #processEvent(org.cloudbus.cloudsim.core.SimEvent)} method.
		 * This method should be overridden by subclasses in other to process
		 * new defined events.
		 *
		 * @param ev information about the event just happened
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void processOtherEvent1(SimEvent ev) {
			if (ev == null) {
				Log.printConcatLine(getName(), ".processOtherEvent(): Error - an event is null.");
			}
		}

		/**
		 * Process the event for an User/Broker who wants to create a VM in this Datacenter. This
		 * Datacenter will then send the status back to the User/Broker.
		 *
		 * @param ev information about the event just happened
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 *
		 * @pre ev != null
		 * @post $none
		 */
		protected void processVmCreate(SimEvent ev, boolean ack) {
			Vm vm = (Vm) ev.getData();

			boolean result = getVmAllocationPolicy().allocateHostForVm(vm);

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = vm.getId();

				if (result) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
			}

			if (result) {
				getVmList().add(vm);

				if (vm.isBeingInstantiated()) {
					vm.setBeingInstantiated(false);
				}

				vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler()
						.getAllocatedMipsForVm(vm));
			}

		}

		/**
		 * Process the event for an User/Broker who wants to destroy a VM previously created in this
		 * Datacenter. This Datacenter may send, upon request, the status back to the
		 * User/Broker.
		 *
		 * @param ev information about the event just happened
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 *
		 * @pre ev != null
		 * @post $none
		 */
		protected void processVmDestroy(SimEvent ev, boolean ack) {
			Vm vm = (Vm) ev.getData();
			getVmAllocationPolicy().deallocateHostForVm(vm);

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = vm.getId();
				data[2] = CloudSimTags.TRUE;

				sendNow(vm.getUserId(), CloudSimTags.VM_DESTROY_ACK, data);
			}

			getVmList().remove(vm);
		}

		/**
		 * Process the event for an User/Broker who wants to migrate a VM. This Datacenter will
		 * then send the status back to the User/Broker.
		 *
		 * @param ev information about the event just happened
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 *
		 * @pre ev != null
		 * @post $none
		 */
		protected void processVmMigrate(SimEvent ev, boolean ack) {
			Object tmp = ev.getData();
			if (!(tmp instanceof Map<?, ?>)) {
				throw new ClassCastException("The data object must be Map<String, Object>");
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> migrate = (HashMap<String, Object>) tmp;

			Vm vm = (Vm) migrate.get("vm");
			Host host = (Host) migrate.get("host");

			getVmAllocationPolicy().deallocateHostForVm(vm);
			host.removeMigratingInVm(vm);
			boolean result = getVmAllocationPolicy().allocateHostForVm(vm, host);
			if (!result) {
				Log.printLine("[Datacenter.processVmMigrate] VM allocation to the destination host failed");
				System.exit(0);
			}

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = vm.getId();

				if (result) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				sendNow(ev.getSource(), CloudSimTags.VM_CREATE_ACK, data);
			}

			Log.formatLine(
					"%.2f: Migration of VM #%d to Host #%d is completed",
					CloudSim.clock(),
					vm.getId(),
					host.getId());
			vm.setInMigration(false);
		}

		/**
		 * Processes a Cloudlet based on the event type.
		 *
		 * @param ev information about the event just happened
		 * @param type event type
		 *
		 * @pre ev != null
		 * @pre type > 0
		 * @post $none
		 */
		protected void processCloudlet(SimEvent ev, int type) {
			int cloudletId = 0;
			int userId = 0;
			int vmId = 0;

			try { // if the sender using cloudletXXX() methods
				int data[] = (int[]) ev.getData();
				cloudletId = data[0];
				userId = data[1];
				vmId = data[2];
			}

			// if the sender using normal send() methods
			catch (ClassCastException c) {
				try {
					Cloudlet cl = (Cloudlet) ev.getData();
					cloudletId = cl.getCloudletId();
					userId = cl.getUserId();
					vmId = cl.getVmId();
				} catch (Exception e) {
					Log.printConcatLine(super.getName(), ": Error in processing Cloudlet");
					Log.printLine(e.getMessage());
					return;
				}
			} catch (Exception e) {
				Log.printConcatLine(super.getName(), ": Error in processing a Cloudlet.");
				Log.printLine(e.getMessage());
				return;
			}

			// begins executing ....
			switch (type) {
				case CloudSimTags.CLOUDLET_CANCEL:
					processCloudletCancel(cloudletId, userId, vmId);
					break;

				case CloudSimTags.CLOUDLET_PAUSE:
					processCloudletPause(cloudletId, userId, vmId, false);
					break;

				case CloudSimTags.CLOUDLET_PAUSE_ACK:
					processCloudletPause(cloudletId, userId, vmId, true);
					break;

				case CloudSimTags.CLOUDLET_RESUME:
					processCloudletResume(cloudletId, userId, vmId, false);
					break;

				case CloudSimTags.CLOUDLET_RESUME_ACK:
					processCloudletResume(cloudletId, userId, vmId, true);
					break;
				default:
					break;
			}

		}

		/**
		 * Process the event for an User/Broker who wants to move a Cloudlet.
		 *
		 * @param receivedData information about the migration
		 * @param type event type
		 *
		 * @pre receivedData != null
		 * @pre type > 0
		 * @post $none
		 */
		protected void processCloudletMove(int[] receivedData, int type) {
			updateCloudletProcessing();

			int[] array = receivedData;
			int cloudletId = array[0];
			int userId = array[1];
			int vmId = array[2];
			int vmDestId = array[3];
			int destId = array[4];

			// get the cloudlet
			Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId,userId)
					.getCloudletScheduler().cloudletCancel(cloudletId);

			boolean failed = false;
			if (cl == null) {// cloudlet doesn't exist
				failed = true;
			} else {
				// has the cloudlet already finished?
				if (cl.getCloudletStatusString() == "Success") {// if yes, send it back to user
					int[] data = new int[3];
					data[0] = getId();
					data[1] = cloudletId;
					data[2] = 0;
					sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
					sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
				}

				// prepare cloudlet for migration
				cl.setVmId(vmDestId);

				// the cloudlet will migrate from one vm to another does the destination VM exist?
				if (destId == getId()) {
					Vm vm = getVmAllocationPolicy().getHost(vmDestId, userId).getVm(vmDestId,userId);
					if (vm == null) {
						failed = true;
					} else {
						// time to transfer the files
						double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
						vm.getCloudletScheduler().cloudletSubmit(cl, fileTransferTime);
					}
				} else {// the cloudlet will migrate from one resource to another
					int tag = ((type == CloudSimTags.CLOUDLET_MOVE_ACK) ? CloudSimTags.CLOUDLET_SUBMIT_ACK
							: CloudSimTags.CLOUDLET_SUBMIT);
					sendNow(destId, tag, cl);
				}
			}

			if (type == CloudSimTags.CLOUDLET_MOVE_ACK) {// send ACK if requested
				int[] data = new int[3];
				data[0] = getId();
				data[1] = cloudletId;
				if (failed) {
					data[2] = 0;
				} else {
					data[2] = 1;
				}
				sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
			}
		}

		/**
		 * Processes a Cloudlet submission.
		 *
		 * @param ev information about the event just happened
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 *
		 * @pre ev != null
		 * @post $none
		 */
		protected void processCloudletSubmit(SimEvent ev, boolean ack) {
			updateCloudletProcessing();

			try {
				// gets the Cloudlet object
				Cloudlet cl = (Cloudlet) ev.getData();

				// checks whether this Cloudlet has finished or not
				if (cl.isFinished()) {
					String name = CloudSim.getEntityName(cl.getUserId());
					Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name,
							" is already completed/finished.");
					Log.printLine("Therefore, it is not being executed again");
					Log.printLine();

					// NOTE: If a Cloudlet has finished, then it won't be processed.
					// So, if ack is required, this method sends back a result.
					// If ack is not required, this method don't send back a result.
					// Hence, this might cause CloudSim to be hanged since waiting
					// for this Cloudlet back.
					if (ack) {
						int[] data = new int[3];
						data[0] = getId();
						data[1] = cl.getCloudletId();
						data[2] = CloudSimTags.FALSE;

						// unique tag = operation tag
						int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
						sendNow(cl.getUserId(), tag, data);
					}

					sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

					return;
				}

				// process this Cloudlet to this CloudResource
				cl.setResourceParameter(
						getId(), getCharacteristics().getCostPerSecond(),
						getCharacteristics().getCostPerBw());

				int userId = cl.getUserId();
				int vmId = cl.getVmId();

				// time to transfer the files
				double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

				Host host = getVmAllocationPolicy().getHost(vmId, userId);
				Vm vm = host.getVm(vmId, userId);
				CloudletScheduler scheduler = vm.getCloudletScheduler();
				double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);

				// if this cloudlet is in the exec queue
				if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
					estimatedFinishTime += fileTransferTime;
					send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
				}

				if (ack) {
					int[] data = new int[3];
					data[0] = getId();
					data[1] = cl.getCloudletId();
					data[2] = CloudSimTags.TRUE;

					// unique tag = operation tag
					int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
					sendNow(cl.getUserId(), tag, data);
				}
			} catch (ClassCastException c) {
				Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
				c.printStackTrace();
			} catch (Exception e) {
				Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
				e.printStackTrace();
			}

			checkCloudletCompletion();
		}

		/**
		 * Predict the total time to transfer a list of files.
		 *
		 * @param requiredFiles the files to be transferred
		 * @return the predicted time
		 */
		protected double predictFileTransferTime(List<String> requiredFiles) {
			double time = 0.0;

			Iterator<String> iter = requiredFiles.iterator();
			while (iter.hasNext()) {
				String fileName = iter.next();
				for (int i = 0; i < getStorageList().size(); i++) {
					Storage tempStorage = getStorageList().get(i);
					File tempFile = tempStorage.getFile(fileName);
					if (tempFile != null) {
						time += tempFile.getSize() / tempStorage.getMaxTransferRate();
						break;
					}
				}
			}
			return time;
		}

		/**
		 * Processes a Cloudlet resume request.
		 *
		 * @param cloudletId ID of the cloudlet to be resumed
		 * @param userId ID of the cloudlet's owner
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 * @param vmId the id of the VM where the cloudlet has to be resumed
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void processCloudletResume(int cloudletId, int userId, int vmId, boolean ack) {
			double eventTime = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId,userId)
					.getCloudletScheduler().cloudletResume(cloudletId);

			boolean status = false;
			if (eventTime > 0.0) { // if this cloudlet is in the exec queue
				status = true;
				if (eventTime > CloudSim.clock()) {
					schedule(getId(), eventTime, CloudSimTags.VM_DATACENTER_EVENT);
				}
			}

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = cloudletId;
				if (status) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				sendNow(userId, CloudSimTags.CLOUDLET_RESUME_ACK, data);
			}
		}

		/**
		 * Processes a Cloudlet pause request.
		 *
		 * @param cloudletId ID of the cloudlet to be paused
		 * @param userId ID of the cloudlet's owner
		 * @param ack indicates if the event's sender expects to receive
		 * an acknowledge message when the event finishes to be processed
		 * @param vmId the id of the VM where the cloudlet has to be paused
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void processCloudletPause(int cloudletId, int userId, int vmId, boolean ack) {
			boolean status = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId,userId)
					.getCloudletScheduler().cloudletPause(cloudletId);

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = cloudletId;
				if (status) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				sendNow(userId, CloudSimTags.CLOUDLET_PAUSE_ACK, data);
			}
		}

		/**
		 * Processes a Cloudlet cancel request.
		 *
		 * @param cloudletId ID of the cloudlet to be canceled
		 * @param userId ID of the cloudlet's owner
		 * @param vmId the id of the VM where the cloudlet has to be canceled
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void processCloudletCancel(int cloudletId, int userId, int vmId) {
			Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId,userId)
					.getCloudletScheduler().cloudletCancel(cloudletId);
			sendNow(userId, CloudSimTags.CLOUDLET_CANCEL, cl);
		}

		/**
		 * Updates processing of each cloudlet running in this Datacenter. It is necessary because
		 * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
		 * updating cloudlets inside them must be called from the outside.
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void updateCloudletProcessing() {
			// if some time passed since last processing
			// R: for term is to allow loop at simulation start. Otherwise, one initial
			// simulation step is skipped and schedulers are not properly initialized
			if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
				List<? extends Host> list = getVmAllocationPolicy().getHostList();
				double smallerTime = Double.MAX_VALUE;
				// for each host...
				for (int i = 0; i < list.size(); i++) {
					Host host = list.get(i);
					// inform VMs to update processing
					double time = host.updateVmsProcessing(CloudSim.clock());
					// what time do we expect that the next cloudlet will finish?
					if (time < smallerTime) {
						smallerTime = time;
					}
				}
				// gurantees a minimal interval before scheduling the event
				if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01) {
					smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01;
				}
				if (smallerTime != Double.MAX_VALUE) {
					schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);
				}
				setLastProcessTime(CloudSim.clock());
			}
		}

		/**
		 * Verifies if some cloudlet inside this Datacenter already finished.
		 * If yes, send it to the User/Broker
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void firstcheckCloudletCompletion() {
			List<? extends Host> list = getVmAllocationPolicy().getHostList();
			for (int i = 0; i < list.size(); i++) {
				Host host = list.get(i);
				for (Vm vm : host.getVmList()) {
					while (vm.getCloudletScheduler().isFinishedCloudlets()) {
						Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
						if (cl != null) {
							sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
						}
					}
				}
			}
		}

		/**
		 * Adds a file into the resource's storage before the experiment starts.
		 * If the file is a master file, then it will be registered to the RC
		 * when the experiment begins.
		 *
		 * @param file a DataCloud file
		 * @return a tag number denoting whether this operation is a success or not
		 */
		public int addFile(File file) {
			if (file == null) {
				return DataCloudTags.FILE_ADD_ERROR_EMPTY;
			}

			if (contains(file.getName())) {
				return DataCloudTags.FILE_ADD_ERROR_EXIST_READ_ONLY;
			}

			// check storage space first
			if (getStorageList().size() <= 0) {
				return DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;
			}

			Storage tempStorage = null;
			int msg = DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;

			for (int i = 0; i < getStorageList().size(); i++) {
				tempStorage = getStorageList().get(i);
				if (tempStorage.getAvailableSpace() >= file.getSize()) {
					tempStorage.addFile(file);
					msg = DataCloudTags.FILE_ADD_SUCCESSFUL;
					break;
				}
			}

			return msg;
		}

		/**
		 * Checks whether the datacenter has the given file.
		 *
		 * @param file a file to be searched
		 * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
		 */
		protected boolean contains(File file) {
			if (file == null) {
				return false;
			}
			return contains(file.getName());
		}

		/**
		 * Checks whether the datacenter has the given file.
		 *
		 * @param fileName a file name to be searched
		 * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
		 */
		protected boolean contains(String fileName) {
			if (fileName == null || fileName.length() == 0) {
				return false;
			}

			Iterator<Storage> it = getStorageList().iterator();
			Storage storage = null;
			boolean result = false;

			while (it.hasNext()) {
				storage = it.next();
				if (storage.contains(fileName)) {
					result = true;
					break;
				}
			}

			return result;
		}

		/**
		 * Deletes the file from the storage.
		 * Also, check whether it is possible to delete the file from the storage.
		 *
		 * @param fileName the name of the file to be deleted
		 * @return the tag denoting the status of the operation,
		 * either {@link DataCloudTags#FILE_DELETE_ERROR} or
		 *  {@link DataCloudTags#FILE_DELETE_SUCCESSFUL}
		 */
		private int deleteFileFromStorage(String fileName) {
			Storage tempStorage = null;
			File tempFile = null;
			int msg = DataCloudTags.FILE_DELETE_ERROR;

			for (int i = 0; i < getStorageList().size(); i++) {
				tempStorage = getStorageList().get(i);
				tempFile = tempStorage.getFile(fileName);
				tempStorage.deleteFile(fileName, tempFile);
				msg = DataCloudTags.FILE_DELETE_SUCCESSFUL;
			} // end for

			return msg;
		}

		@Override
		public void shutdownEntity() {
			Log.printConcatLine(getName(), " is shutting down...");
		}

		@Override
		public void startEntity() {
			Log.printConcatLine(getName(), " is starting...");
			// this resource should register to regional CIS.
			// However, if not specified, then register to system CIS (the
			// default CloudInformationService) entity.
			int gisID = CloudSim.getEntityId(regionalCisName);
			if (gisID == -1) {
				gisID = CloudSim.getCloudInfoServiceEntityId();
			}

			// send the registration to CIS
			sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
			// Below method is for a child class to override
			registerOtherEntity();
		}

		/**
		 * Gets the host list.
		 *
		 * @return the host list
		 */
		@SuppressWarnings("unchecked")
		public <T extends Host> List<T> getHostList() {
			return (List<T>) getCharacteristics().getHostList();
		}

		/**
		 * Gets the datacenter characteristics.
		 *
		 * @return the datacenter characteristics
		 */
		protected DatacenterCharacteristics getCharacteristics() {
			return characteristics;
		}

		/**
		 * Sets the datacenter characteristics.
		 *
		 * @param characteristics the new datacenter characteristics
		 */
		protected void setCharacteristics(DatacenterCharacteristics characteristics) {
			this.characteristics = characteristics;
		}

		/**
		 * Gets the regional Cloud Information Service (CIS) name.
		 *
		 * @return the regional CIS name
		 */
		protected String getRegionalCisName() {
			return regionalCisName;
		}

		/**
		 * Sets the regional cis name.
		 *
		 * @param regionalCisName the new regional cis name
		 */
		protected void setRegionalCisName(String regionalCisName) {
			this.regionalCisName = regionalCisName;
		}

		/**
		 * Gets the vm allocation policy.
		 *
		 * @return the vm allocation policy
		 */
		public VmAllocationPolicy getVmAllocationPolicy() {
			return vmAllocationPolicy;
		}

		/**
		 * Sets the vm allocation policy.
		 *
		 * @param vmAllocationPolicy the new vm allocation policy
		 */
		protected void setVmAllocationPolicy(VmAllocationPolicy vmAllocationPolicy) {
			this.vmAllocationPolicy = vmAllocationPolicy;
		}

		/**
		 * Gets the last time some cloudlet was processed in the datacenter.
		 *
		 * @return the last process time
		 */
		protected double getLastProcessTime() {
			return lastProcessTime;
		}

		/**
		 * Sets the last process time.
		 *
		 * @param lastProcessTime the new last process time
		 */
		protected void setLastProcessTime(double lastProcessTime) {
			this.lastProcessTime = lastProcessTime;
		}

		/**
		 * Gets the storage list.
		 *
		 * @return the storage list
		 */
		protected List<Storage> getStorageList() {
			return storageList;
		}

		/**
		 * Sets the storage list.
		 *
		 * @param storageList the new storage list
		 */
		protected void setStorageList(List<DatacenterStorage> storageList) {
			this.storageList = storageList;
		}

		/**
		 * Gets the vm list.
		 *
		 * @return the vm list
		 */
		@SuppressWarnings("unchecked")
		public <T extends Vm> List<T> getVmList() {
			return (List<T>) vmList;
		}

		/**
		 * Sets the vm list.
		 *
		 * @param vmList the new vm list
		 */
		protected <T extends Vm> void setVmList(List<T> vmList) {
			this.vmList = vmList;
		}

		/**
		 * Gets the scheduling interval.
		 *
		 * @return the scheduling interval
		 */
		protected double getSchedulingInterval() {
			return schedulingInterval;
		}

		/**
		 * Sets the scheduling interval.
		 *
		 * @param schedulingInterval the new scheduling interval
		 */
		protected void setSchedulingInterval(double schedulingInterval) {
			this.schedulingInterval = schedulingInterval;
		}

	}



}
