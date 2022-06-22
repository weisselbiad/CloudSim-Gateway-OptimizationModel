package gpu;

import gpu.core.CloudSimTags;
import gpu.core.DataCloudTag;
import gpu.core.Log;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.power.models.PowerModelDatacenter;
import org.cloudbus.cloudsim.power.models.PowerModelDatacenterSimple;
import org.cloudbus.cloudsim.resources.DatacenterStorage;
import org.cloudbus.cloudsim.resources.File;
import org.cloudbus.cloudsim.resources.SanStorage;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.util.DataCloudTags;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.DatacenterVmMigrationEventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.listeners.HostEventInfo;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static org.cloudbus.cloudsim.core.CloudSimTag.CLOUDLET_RETURN;
import static org.cloudbus.cloudsim.util.BytesConversion.bitesToBytes;

/**
 * {@link GpuDatacenter} extends {@link Datacenter} to support
 * {@link GpuCloudlet}s as well as the memory transfer between CPU and GPU.
 *
 * @author Ahmad Siavashi
 *
 */
public class GpuDatacenter extends CloudSimEntity implements Datacenter {
	private List<SanStorage> storageList;
	private DatacenterCharacteristics characteristics;
	private double gpuTaskLastProcessTime;
	private long activeHostsNumber;
	private DatacenterStorage datacenterStorage;
	private SanStorage sanStorage;
	private PowerModelDatacenter powerModel = PowerModelDatacenter.NULL;
	private double bandwidthPercentForMigration;
	private final List<EventListener<HostEventInfo>> onHostAvailableListeners;
	private final List<EventListener<DatacenterVmMigrationEventInfo>> onVmMigrationFinishListeners;
	private double hostSearchRetryDelay;
	private double timeZone;
	private boolean migrationsEnabled;
	private List<? extends Host> hostList;
	private Map<GpuTask, ResGpuCloudlet> gpuTaskResGpuCloudletMap;

	public Simulation simulation;
	/**
	 * See {@link Datacenter#}
	 */
	public GpuDatacenter(String name, Simulation simulation, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
						 List<SanStorage> storageList, double schedulingInterval) throws Exception {
		super(simulation);
		setName(name);

		addHostList(getHostList());
		setSchedulingInterval(schedulingInterval);
		setCharacteristics(characteristics);
		setVmAllocationPolicy(vmAllocationPolicy);
		setStorageList(storageList);
		setGpuTaskLastProcessTime(0.0);
		setGpuTaskResGpuCloudletMap(new HashMap<>());
		setPowerModel(new PowerModelDatacenterSimple(this));

		this.simulation =simulation;
		this.onHostAvailableListeners = new ArrayList<>();
		this.onVmMigrationFinishListeners = new ArrayList<>();
		this.characteristics = new DatacenterCharacteristicsSimple(this);
		this.bandwidthPercentForMigration = DEF_BW_PERCENT_FOR_MIGRATION;
		this.migrationsEnabled = true;
		this.hostSearchRetryDelay = -1;
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

	protected void checkCloudletCompletion(SimEvent ev) {
		firstcheckCloudletCompletion(ev);
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				GpuCloudletScheduler scheduler = (GpuCloudletScheduler) vm.getCloudletScheduler();
				while (scheduler.hasGpuTask()) {
					GpuTask gt = scheduler.getNextGpuTask();
					sendNow(ev.getSource(), CloudSimTag.GPU_MEMORY_TRANSFER, gt);
				}
			}
		}
	}

	public void processVmCreate(SimEvent ev, boolean ack) {
		Vm vm = (Vm) ev.getData();
		Log.printLine(simulation.clock() + ": Trying to Create VM #" + vm.getId() + " in " + getName());

		HostSuitability result = getVmAllocationPolicy().allocateHostForVm(vm);

		if (ack) {
			int[] data = new int[3];
			data[0] = (int)getId();
			data[1] = (int)vm.getId();

			if (result.fully()) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(ev.getSource(), simulation.getMinTimeBetweenEvents(), CloudSimTag.VM_CREATE_ACK, data);
		}

		if (result.fully()) {
			getVmList().add(vm);
			GpuVm gpuVm = (GpuVm) vm;
			Vgpu vgpu = gpuVm.getVgpu();

			if (vm.isWorking()) {
				vm.setFailed(true);
			}

			vm.updateProcessing(simulation.clock(),
					getVmAllocationPolicy().getDatacenter().getHost((int)vm.getHost().getId()).getVmScheduler().getAllocatedMips(vm));

			if (vgpu != null) {
				if (vgpu.isBeingInstantiated()) {
					vgpu.setBeingInstantiated(false);
				}

				VideoCard videoCard = vgpu.getVideoCard();
				vgpu.updateGpuTaskProcessing(simulation.clock(),
						videoCard.getVgpuScheduler().getAllocatedMipsForVgpu(vgpu));
			}
		}

	}


	protected void processVmDestroy(SimEvent ev, boolean ack) {
		GpuVm vm = (GpuVm) ev.getData();
		if (vm.hasVgpu()) {
			((GpuVmAllocationPolicy) getVmAllocationPolicy()).deallocateGpuForVgpu(vm.getVgpu());
		}
		firstprocessVmDestroy(ev, ack);
	}

	protected void processGpuTaskSubmit(SimEvent ev) {
		updateGpuTaskProcessing();

		try {
			// gets the task object
			GpuTask gt = (GpuTask) ev.getData();

			// TODO: checks whether this task has finished or not

			// process this task to this CloudResource
			gt.setResourceParameter((int)getId(), getCharacteristics().getCostPerSecond(),
					getCharacteristics().getCostPerBw());

			GpuVm vm = getGpuTaskVm(gt);
			Vgpu vgpu = vm.getVgpu();

			GpuTaskScheduler scheduler = vgpu.getGpuTaskScheduler();

			double estimatedFinishTime = scheduler.taskSubmit(gt);

			// if this task is in the exec queue
			if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
				send(ev.getSource(), estimatedFinishTime, CloudSimTag.VGPU_DATACENTER_EVENT);
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


		/** The regional Cloud Information Service (CIS) name.
		 * @see org.cloudbus.cloudsim.core.CloudInformationService
		 */
		private String regionalCisName;

		/** The vm provisioner. */
		private VmAllocationPolicy vmAllocationPolicy;

		/** The last time some cloudlet was processed in the datacenter. */
		private double lastProcessTime;


		/** The vm list. */
		private List<? extends Vm> vmList;

		/** The scheduling delay to process each datacenter received event. */
		private double schedulingInterval;


		/**
		 * Overrides this method when making a new and different type of resource. <br>
		 * <b>NOTE:</b> You do not need to override {@link } method, if you use this method.
		 *
		 * @pre $none
		 * @post $none
		 *
		 * @todo This method doesn't appear to be used
		 */
		protected void registerOtherEntity() {
			// empty. This should be override by a child class
		}


		public void processEvent(SimEvent ev) {
			int srcId = -1;

			switch (ev.getTag()) {
				// Resource characteristics inquiry
				case RESOURCE_CHARACTERISTICS:
					srcId = ((Integer) ev.getData()).intValue();
					sendNow(ev.getSource(), ev.getTag(), getCharacteristics());
					break;

				// Resource dynamic info inquiry
				case RESOURCE_DYNAMICS:
					srcId = ((Integer) ev.getData()).intValue();
					sendNow(ev.getSource(), ev.getTag(), 0);
					break;

				case RESOURCE_NUM_PE:
					srcId = ((Integer) ev.getData()).intValue();
					int numPE = getCharacteristics().getNumberOfPes();
					sendNow(ev.getSource(), ev.getTag(), numPE);
					break;

				case RESOURCE_NUM_FREE_PE:
					srcId = ((Integer) ev.getData()).intValue();
					int freePesNumber = (int) getCharacteristics().getDatacenter().getHostList().stream().mapToLong(Host::getFreePesNumber).sum();
					sendNow(ev.getSource(), ev.getTag(), freePesNumber);
					break;

				// New Cloudlet arrives
				case CLOUDLET_SUBMIT:
					processCloudletSubmit(ev, false);
					break;

				// New Cloudlet arrives, but the sender asks for an ack
				case CLOUDLET_SUBMIT_ACK:
					processCloudletSubmit(ev, true);
					break;

				// Cancels a previously submitted Cloudlet
				case CLOUDLET_CANCEL:
					processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
					break;

				// Pauses a previously submitted Cloudlet
				case CLOUDLET_PAUSE:
					processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
					break;

				// Pauses a previously submitted Cloudlet, but the sender
				// asks for an acknowledgement
				case CLOUDLET_PAUSE_ACK:
					processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
					break;

				// Resumes a previously submitted Cloudlet
				case CLOUDLET_RESUME:
					processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
					break;

				// Resumes a previously submitted Cloudlet, but the sender
				// asks for an acknowledgement
				case CLOUDLET_RESUME_ACK:
					processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
					break;

				// Moves a previously submitted Cloudlet to a different resource
				case CLOUDLET_MOVE:
					processCloudletMove( ev,(int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
					break;

				// Moves a previously submitted Cloudlet to a different resource
				case CLOUDLET_MOVE_ACK:
					processCloudletMove( ev,(int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
					break;

				// Checks the status of a Cloudlet
				case CLOUDLET_STATUS:
					processCloudletStatus(ev);
					break;

				// Ping packet

				case VM_CREATE:
					firstprocessVmCreate(ev, false);
					break;

				case VM_CREATE_ACK:
					firstprocessVmCreate(ev, true);
					break;

				case VM_DESTROY:
					processVmDestroy(ev, false);
					break;

				case VM_DESTROY_ACK:
					processVmDestroy(ev, true);
					break;

				case VM_MIGRATE:
					processVmMigrate(ev, false);
					break;

				case VM_MIGRATE_ACK:
					processVmMigrate(ev, true);
					break;



				case VM_DATACENTER_EVENT:
					updateCloudletProcessing();
					checkCloudletCompletion(ev);
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
			CloudSimTag tag;

			// check if this file can be deleted (do not delete is right now)
			int msg = deleteFileFromStorage(filename);
			if (msg == DataCloudTags.FILE_DELETE_SUCCESSFUL) {
				tag = CloudSimTag.CTLG_DELETE_MASTER;
			} else { // if an error occured, notify user
				tag = CloudSimTag.FILE_DELETE_MASTER_RESULT;
			}

			if (ack) {
				// send back to sender
				Object pack[] = new Object[2];
				pack[0] = filename;
				pack[1] = Integer.valueOf(msg);

				sendNow(ev.getSource(), tag, pack);
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
				sendNow(ev.getSource(), CloudSimTag.FILE_ADD_MASTER_RESULT, data);
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
	/*	protected void processPingRequest(SimEvent ev) {
			InfoPacket pkt = (InfoPacket) ev.getData();
			pkt.setTag(CloudSimTags.INFOPKT_RETURN);
			pkt.setDestId(pkt.getSrcId());

			// sends back to the sender
			sendNow(pkt.getSrcId(), CloudSimTags.INFOPKT_RETURN, pkt);
		}*/

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
			Cloudlet.Status status ;

			try {
				// if a sender using cloudletXXX() methods
				int data[] = (int[]) ev.getData();
				cloudletId = data[0];
				userId = data[1];
				vmId = data[2];

				status =getVmList().get(vmId).getCloudletScheduler().getCloudletList().get(cloudletId).getStatus();

			}

			// if a sender using normal send() methods
			catch (ClassCastException c) {
				try {
					GpuCloudlet cl = (GpuCloudlet) ev.getData();
					cloudletId = (int)cl.getId();
					userId = cl.getUserId();

					status = getVmList().get(vmId).getCloudletScheduler().getCloudletList().get(cloudletId).getStatus();
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

			Object[] array = new Object[3];
			array[0] = (int)getId();
			array[1] = cloudletId;
			array[2] = status;

			CloudSimTag tag = CloudSimTag.CLOUDLET_STATUS;
			sendNow(ev.getSource(), tag, array);
		}

		/**
		 * Process non-default received events that aren't processed by
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
		protected void firstprocessVmCreate(SimEvent ev, boolean ack) {
			Vm vm = (Vm) ev.getData();

			HostSuitability result = getVmAllocationPolicy().allocateHostForVm(vm);

			if (ack) {
				int[] data = new int[3];
				data[0] = (int)getId();
				data[1] = (int)vm.getId();

				if (result.fully()) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				send(ev.getSource(), simulation.getMinTimeBetweenEvents(), CloudSimTag.VM_CREATE_ACK, data);
			}

			if (result.fully()) {
				getVmList().add(vm);

				if (vm.isWorking()) {
					vm.setFailed(true);
				}

				vm.updateProcessing(simulation.clock(), getVmAllocationPolicy().getDatacenter().getHost((int)vm.getHost().getId()).getVmScheduler()
						.getAllocatedMips(vm));
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
		protected void firstprocessVmDestroy(SimEvent ev, boolean ack) {
			Vm vm = (Vm) ev.getData();
			getVmAllocationPolicy().deallocateHostForVm(vm);

			if (ack) {
				int[] data = new int[3];
				data[0] = (int) getId();
				data[1] = (int) vm.getId();
				data[2] = CloudSimTags.TRUE;

				sendNow(ev.getSource(), CloudSimTag.VM_DESTROY_ACK, data);
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
			HostSuitability result = getVmAllocationPolicy().allocateHostForVm(vm, host);
			if (!result.fully()) {
				Log.printLine("[Datacenter.processVmMigrate] VM allocation to the destination host failed");
				System.exit(0);
			}

			if (ack) {
				int[] data = new int[3];
				data[0] = (int)getId();
				data[1] = (int)vm.getId();

				if (result.fully()) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				sendNow(ev.getSource(), CloudSimTag.VM_CREATE_ACK, data);
			}

			Log.formatLine(
					"%.2f: Migration of VM #%d to Host #%d is completed",
					simulation.clock(),
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
					GpuCloudlet cl = (GpuCloudlet) ev.getData();
					cloudletId = (int)cl.getId();
					userId = cl.getUserId();
					vmId = (int)cl.getVm().getId();
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
					processCloudletCancel(ev, cloudletId, userId, vmId);
					break;

				case CloudSimTags.CLOUDLET_PAUSE:
					processCloudletPause(ev,cloudletId, userId, vmId, false);
					break;

				case CloudSimTags.CLOUDLET_PAUSE_ACK:
					processCloudletPause(ev,cloudletId, userId, vmId, true);
					break;

				case CloudSimTags.CLOUDLET_RESUME:
					processCloudletResume(ev,cloudletId, userId, vmId, false);
					break;

				case CloudSimTags.CLOUDLET_RESUME_ACK:
					processCloudletResume(ev,cloudletId, userId, vmId, true);
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
		protected void processCloudletMove(SimEvent ev,int[] receivedData, int type) {
			updateCloudletProcessing();

			int[] array = receivedData;
			int cloudletId = array[0];
			int userId = array[1];
			int vmId = array[2];
			int vmDestId = array[3];
			int destId = array[4];

			// get the cloudlet
			GpuCloudlet cl = (GpuCloudlet) getVmAllocationPolicy().getDatacenter().getHost((int)getVmList().get(vmId).getHost().getId()).getVmList().get(vmId).getCloudletScheduler().getCloudletList().get(cloudletId);

			boolean failed = false;
			if (cl == null) {// cloudlet doesn't exist
				failed = true;
			} else {
				// has the cloudlet already finished?
				if (cl.getStatus() == Cloudlet.Status.SUCCESS) {// if yes, send it back to user
					int[] data = new int[3];
					data[0] = (int)getId();
					data[1] = cloudletId;
					data[2] = 0;
					sendNow(ev.getSource(), CloudSimTag.CLOUDLET_SUBMIT_ACK, data);
					sendNow(ev.getSource(), CloudSimTag.CLOUDLET_RETURN, cl);
				}

				// prepare cloudlet for migration
				cl.getVm().setId(vmDestId);

				// the cloudlet will migrate from one vm to another does the destination VM exist?
				if (destId == getId()) {
					Vm vm = getVmList().get(vmDestId);
					if (vm == null) {
						failed = true;
					} else {
						// time to transfer the files
						double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
						vm.getCloudletScheduler().cloudletSubmit(cl, fileTransferTime);
					}
				} else {// the cloudlet will migrate from one resource to another
					CloudSimTag tag = ((type == CloudSimTags.CLOUDLET_MOVE_ACK) ? CloudSimTag.CLOUDLET_SUBMIT_ACK
							: CloudSimTag.CLOUDLET_SUBMIT);
					sendNow(ev.getDestination(), tag, cl);
				}
			}

			if (type == CloudSimTags.CLOUDLET_MOVE_ACK) {// send ACK if requested
				int[] data = new int[3];
				data[0] = (int)getId();
				data[1] = cloudletId;
				if (failed) {
					data[2] = 0;
				} else {
					data[2] = 1;
				}
				sendNow(ev.getSource(), CloudSimTag.CLOUDLET_SUBMIT_ACK, data);
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
				GpuCloudlet cl = (GpuCloudlet) ev.getData();

				// checks whether this Cloudlet has finished or not
				if (cl.isFinished()) {
					String name = simulation.toString();
					Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getId(), " owned by ", name,
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
						data[0] = (int)getId();
						data[1] = (int) cl.getId();
						data[2] = CloudSimTags.FALSE;

						// unique tag = operation tag
						CloudSimTag tag = CloudSimTag.CLOUDLET_SUBMIT_ACK;
						sendNow(ev.getSource(), tag, data);
					}

					sendNow(ev.getSource(), CloudSimTag.CLOUDLET_RETURN, cl);

					return;
				}

				// process this Cloudlet to this CloudResource
				cl.setId(getId());
				/*
				cl.setResourceParameter(
						getId(), getCharacteristics().getCostPerSecond(),
						getCharacteristics().getCostPerBw());*/

				int userId = cl.getUserId();
				int vmId = (int)cl.getVm().getId();

				// time to transfer the files
				double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

				Host host = getVmAllocationPolicy().getDatacenter().getHostById(getVmList().get(vmId).getHost().getId());
				Vm vm = host.getVmList().get(vmId);
				CloudletScheduler scheduler = vm.getCloudletScheduler();
				double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);

				// if this cloudlet is in the exec queue
				if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
					estimatedFinishTime += fileTransferTime;
					send(ev.getSource(), estimatedFinishTime, CloudSimTag.VM_DATACENTER_EVENT);
				}

				if (ack) {
					int[] data = new int[3];
					data[0] =(int) getId();
					data[1] = (int)cl.getId();
					data[2] = CloudSimTags.TRUE;

					// unique tag = operation tag
					CloudSimTag tag = CloudSimTag.CLOUDLET_SUBMIT_ACK;
					sendNow(ev.getSource(), tag, data);
				}
			} catch (ClassCastException c) {
				Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
				c.printStackTrace();
			} catch (Exception e) {
				Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
				e.printStackTrace();
			}

			checkCloudletCompletion(ev);
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
					SanStorage tempStorage = getStorageList().get(i);
					Optional<File> tempFile = tempStorage.getFile(fileName);
					if (tempFile != null) {
						time += tempFile.get().getSize() / tempStorage.getMaxTransferRate();
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
		protected void processCloudletResume(SimEvent ev, int cloudletId, int userId, int vmId, boolean ack) {
			double eventTime = getVmAllocationPolicy().getDatacenter().getHostById(getVmList().get(vmId).getHost().getId()).getVmList().get(vmId)
					.getCloudletScheduler().cloudletResume(getVmList().get(vmId).getCloudletScheduler().getCloudletList().get(cloudletId));

			boolean status = false;
			if (eventTime > 0.0) { // if this cloudlet is in the exec queue
				status = true;
				if (eventTime > simulation.clock()) {
					schedule( eventTime, CloudSimTag.VM_DATACENTER_EVENT);
				}
			}

			if (ack) {
				int[] data = new int[3];
				data[0] = (int)getId();
				data[1] = cloudletId;
				if (status) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				sendNow(ev.getSource(), CloudSimTag.CLOUDLET_RESUME_ACK, data);
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
		protected void processCloudletPause(SimEvent ev ,int cloudletId, int userId, int vmId, boolean ack) {
			boolean status = getVmAllocationPolicy().getDatacenter().getHostById(getVmList().get(vmId).getHost().getId()).getVmList().get(vmId)
					.getCloudletScheduler().cloudletPause(getVmList().get(vmId).getCloudletScheduler().getCloudletList().get(cloudletId));

			if (ack) {
				int[] data = new int[3];
				data[0] = (int)getId();
				data[1] = cloudletId;
				if (status) {
					data[2] = CloudSimTags.TRUE;
				} else {
					data[2] = CloudSimTags.FALSE;
				}
				sendNow(ev.getSource(), CloudSimTag.CLOUDLET_PAUSE_ACK, data);
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
		protected void processCloudletCancel(SimEvent ev, int cloudletId, int userId, int vmId) {
			Cloudlet cl = getVmAllocationPolicy().getDatacenter().getHostById(getVmList().get(vmId).getHost().getId()).getVmList().get(vmId)
					.getCloudletScheduler().cloudletCancel(getVmList().get(vmId).getCloudletScheduler().getCloudletList().get(cloudletId));
			sendNow(ev.getSource(), CloudSimTag.CLOUDLET_CANCEL, cl);
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
			if (simulation.clock() < 0.111 || simulation.clock() > getLastProcessTime() + simulation.getMinTimeBetweenEvents()) {
				List<? extends Host> list = getVmAllocationPolicy().getHostList();
				double smallerTime = Double.MAX_VALUE;
				// for each host...
				for (int i = 0; i < list.size(); i++) {
					Host host = list.get(i);
					// inform VMs to update processing
					double time = host.updateProcessing(simulation.clock());
					// what time do we expect that the next cloudlet will finish?
					if (time < smallerTime) {
						smallerTime = time;
					}
				}
				// gurantees a minimal interval before scheduling the event
				if (smallerTime < simulation.clock() + simulation.getMinTimeBetweenEvents() + 0.01) {
					smallerTime = simulation.clock() + simulation.getMinTimeBetweenEvents() + 0.01;
				}
				if (smallerTime != Double.MAX_VALUE) {
					schedule(simulation.getEntityList().get((int)getId()), (smallerTime - simulation.clock()), CloudSimTag.VM_DATACENTER_EVENT);
				}
				setLastProcessTime(simulation.clock());
			}
		}

		/**
		 * Verifies if some cloudlet inside this Datacenter already finished.
		 * If yes, send it to the User/Broker
		 *
		 * @pre $none
		 * @post $none
		 */
		protected void firstcheckCloudletCompletion(SimEvent ev) {
			List<? extends Host> list = getVmAllocationPolicy().getHostList();
			for (int i = 0; i < list.size(); i++) {
				Host host = list.get(i);
				for (Vm vm : host.getVmList()) {
					while (vm.getCloudletScheduler().isEmpty() ) {
						CloudletExecution cl = vm.getCloudletScheduler().getCloudletFinishedList().get(i);
						if (cl != null) {
							sendNow(ev.getSource(), CloudSimTag.CLOUDLET_RETURN, cl);
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

			SanStorage tempStorage = null;
			int msg = DataCloudTags.FILE_ADD_ERROR_STORAGE_FULL;

			for (int i = 0; i < getStorageList().size(); i++) {
				tempStorage = getStorageList().get(i);
				if (tempStorage.getAvailableResource() >= file.getSize()) {
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

			Iterator<SanStorage> it = getStorageList().iterator();
			SanStorage storage = null;
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
			SanStorage tempStorage = null;
			Optional<File> tempFile = null;
			int msg = DataCloudTags.FILE_DELETE_ERROR;

			for (int i = 0; i < getStorageList().size(); i++) {
				tempStorage = getStorageList().get(i);
				tempFile = tempStorage.getFile(fileName);
				tempStorage.deleteFile(fileName);
				msg = DataCloudTags.FILE_DELETE_SUCCESSFUL;
			} // end for

			return msg;
		}


		public void shutdownEntity() {
			Log.printConcatLine(getName(), " is shutting down...");
		}


		public void startEntity() {
			Log.printConcatLine(getName(), " is starting...");
			// this resource should register to regional CIS.
			// However, if not specified, then register to system CIS (the
			// default CloudInformationService) entity.
			SimEntity gisID = simulation.getEntityId(regionalCisName);
			if (gisID == SimEntity.NULL) {
				gisID = CloudSim.NULL.getCloudInfoService();
			}

			// send the registration to CIS
			sendNow(gisID, CloudSimTag.REGISTER_RESOURCE, getId());
			// Below method is for a child class to override
			registerOtherEntity();
		}

	@Override
	public void requestVmMigration(Vm sourceVm, Host targetHost) {
		//If Host.NULL is given, it must try to find a target host
		if(Host.NULL.equals(targetHost)){
			targetHost = vmAllocationPolicy.findHostForVm(sourceVm).orElse(Host.NULL);
		}

		//If a host couldn't be found yet
		if(Host.NULL.equals(targetHost)) {
			LOGGER.warn("{}: {}: No suitable host found for {} in {}", sourceVm.getSimulation().clockStr(), getClass().getSimpleName(), sourceVm, this);
			return;
		}

		final Host sourceHost = sourceVm.getHost();
		final double delay = timeToMigrateVm(sourceVm, targetHost);
		final String msg1 =
				Host.NULL.equals(sourceHost) ?
						String.format("%s to %s", sourceVm, targetHost) :
						String.format("%s from %s to %s", sourceVm, sourceHost, targetHost);

		final String currentTime = getSimulation().clockStr();
		final var fmt = "It's expected to finish in %.2f seconds, considering the %.0f%% of bandwidth allowed for migration and the VM RAM size.";
		final String msg2 = String.format(fmt, delay, getBandwidthPercentForMigration()*100);
		LOGGER.info("{}: {}: Migration of {} is started. {}", currentTime, getName(), msg1, msg2);

		if(targetHost.addMigratingInVm(sourceVm)) {
			sourceHost.addVmMigratingOut(sourceVm);
			send(this, delay, CloudSimTag.VM_MIGRATE, new TreeMap.SimpleEntry<>(sourceVm, targetHost));
		}

	}
	private double timeToMigrateVm(final Vm vm, final Host targetHost) {
		return vm.getRam().getCapacity() / bitesToBytes(targetHost.getBw().getCapacity() * getBandwidthPercentForMigration());
	}


	@Override
	public void requestVmMigration(Vm sourceVm) {
		requestVmMigration(sourceVm, Host.NULL);
	}

	/**
		 * Gets the host list.
		 *
		 * @return the host list
		 */
		@SuppressWarnings("unchecked")
		public <T extends Host> List<T> getHostList() {
			return (List<T>)Collections.unmodifiableList(hostList);
		}

	@Override
	public <T extends Host> List<T> getSimpleHostList() {
		return null;
	}

	@Override
	public <T extends Host> List<T> getGpuHostList() {
		return null;
	}

	@Override
	public Stream<? extends Host> getActiveHostStream() {
		return hostList.stream().filter(Host::isActive);
	}

	@Override
	public Host getHost(int index) {
		if (index >= 0 && index < getHostList().size()) {
		}return getHostList().get(index);}

	@Override
	public long getActiveHostsNumber() {
		return activeHostsNumber;
	}
	public void updateActiveHostsNumber(final Host host){
		activeHostsNumber += host.isActive() ? 1 : -1;
	}

	private void setHostList(final List<? extends Host> hostList) {
		this.hostList = requireNonNull(hostList);
		setupHosts();
	}

	private void setupHosts() {
		long lastHostId = getLastHostId();
		for (final Host host : hostList) {
			lastHostId = setupHost(host, lastHostId);
		}
	}
	private long getLastHostId() {
		return hostList.isEmpty() ? -1 : hostList.get(hostList.size()-1).getId();
	}

	protected long setupHost(final Host host, long nextId) {
		nextId = Math.max(nextId, -1);
		if(host.getId() < 0) {
			host.setId(++nextId);
		}

		host.setSimulation(getSimulation()).setDatacenter(this);
		host.setActive(((HostSimple)host).isActivateOnDatacenterStartup());
		return nextId;
	}
	@Override
	public long size() {
		return hostList.size();
	}

	@Override
	public Host getHostById(long id) {
		return hostList.stream().filter(host -> host.getId() == id).findFirst().map(host -> (Host)host).orElse(Host.NULL);
	}

	@Override
	public <T extends Host> Datacenter addHostList(List<T> hostList) {
		requireNonNull(hostList);
		hostList.forEach(this::addHost);
		return this;
	}

	@Override
	public <T extends Host> Datacenter addHost(T host) {
		if(vmAllocationPolicy == null || vmAllocationPolicy == VmAllocationPolicy.NULL){
			throw new IllegalStateException("A VmAllocationPolicy must be set before adding a new Host to the Datacenter.");
		}

		setupHost(host, getLastHostId());
		((List<T>)hostList).add(host);
		return this;
	}


	@Override
	public <T extends Host> Datacenter removeHost(final T host) {
		hostList.remove(host);
		return this;
	}

	/**
		 * Gets the datacenter characteristics.
		 *
		 * @return the datacenter characteristics
		 */
		public DatacenterCharacteristics getCharacteristics() {
			return characteristics;
		}

	@Override
	public DatacenterStorage getDatacenterStorage() {
		return this.datacenterStorage;
	}

	@Override
	public final void setDatacenterStorage(final DatacenterStorage datacenterStorage) {
		datacenterStorage.setDatacenter(this);
		this.datacenterStorage = datacenterStorage;
	}

	@Override
	public double getBandwidthPercentForMigration() {
		return bandwidthPercentForMigration;
	}
	@Override
	public void setBandwidthPercentForMigration(final double bandwidthPercentForMigration) {
		if(bandwidthPercentForMigration <= 0){
			throw new IllegalArgumentException("The bandwidth migration percentage must be greater than 0.");
		}

		if(bandwidthPercentForMigration > 1){
			throw new IllegalArgumentException("The bandwidth migration percentage must be lower or equal to 1.");
		}

		this.bandwidthPercentForMigration = bandwidthPercentForMigration;
	}

	@Override
	public Datacenter addOnHostAvailableListener(final EventListener<HostEventInfo> listener) {
		onHostAvailableListeners.add(requireNonNull(listener));
		return this;
	}

	@Override
	public Datacenter addOnVmMigrationFinishListener(final EventListener<DatacenterVmMigrationEventInfo> listener) {
		onVmMigrationFinishListeners.add(requireNonNull(listener));
		return this;
	}


	public boolean isMigrationsEnabled() {
		return migrationsEnabled && vmAllocationPolicy.isVmMigrationSupported();
	}

	@Override
	public final Datacenter enableMigrations() {
		if(!vmAllocationPolicy.isVmMigrationSupported()){
			LOGGER.warn(
					"{}: {}: It was requested to enable VM migrations but the {} doesn't support that.",
					getSimulation().clockStr(), getName(), vmAllocationPolicy.getClass().getSimpleName());
			return this;
		}

		this.migrationsEnabled = true;
		return this;
	}

	@Override
	public final Datacenter disableMigrations() {
		this.migrationsEnabled = false;
		return this;
	}

	@Override
	public double getHostSearchRetryDelay() {
		return hostSearchRetryDelay;
	}

	@Override
	public Datacenter setHostSearchRetryDelay(final double delay) {
		if(delay == 0){
			throw new IllegalArgumentException("hostSearchRetryDelay cannot be 0. Set a positive value to define an actual delay or a negative value to indicate a new Host search must be tried as soon as possible.");
		}

		this.hostSearchRetryDelay = delay;
		return this;
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
		protected List<SanStorage> getStorageList() {
			return storageList;
		}

		/**
		 * Sets the storage list.
		 *
		 * @param storageList the new storage list
		 */
		protected void setStorageList(List<SanStorage> storageList) {
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
		public double getSchedulingInterval() {
			return schedulingInterval;
		}

		/**
		 * Sets the scheduling interval.
		 *
		 * @param schedulingInterval the new scheduling interval
		 * @return
		 */
		public final Datacenter setSchedulingInterval(double schedulingInterval) {
			this.schedulingInterval = schedulingInterval;
			return this;
		}

	@Override
	protected void startInternal() {
		LOGGER.info("{}: {} is starting...", getSimulation().clockStr(), getName());
		hostList.stream()
				.filter(not(Host::isActive))
				.map(host -> (HostSimple)host)
				.forEach(host -> host.setActive(host.isActivateOnDatacenterStartup()));
		sendNow(getSimulation().getCloudInfoService(), CloudSimTag.DC_REGISTRATION_REQUEST, this);

	}

	@Override
	public double getTimeZone() {
		return timeZone;
	}

	@Override
	public final Datacenter setTimeZone(final double timeZone) {
		this.timeZone = validateTimeZone(timeZone);
		return this;
	}
	@Override
	public PowerModelDatacenter getPowerModel() {
		return powerModel;
	}

	@Override
	public final void setPowerModel(final PowerModelDatacenter powerModel) {
		requireNonNull(powerModel,
				"powerModel cannot be null. You could provide a " +
						PowerModelDatacenter.class.getSimpleName() + ".NULL instead");

		if(powerModel.getDatacenter() != null && powerModel.getDatacenter() != Datacenter.NULL && !this.equals(powerModel.getDatacenter())){
			throw new IllegalStateException("The given PowerModel is already assigned to another Datacenter. Each Datacenter must have its own PowerModel instance.");
		}

		this.powerModel = powerModel;
	}
}




