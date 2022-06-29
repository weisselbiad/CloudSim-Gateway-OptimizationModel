package gpu;

import gpu.core.CloudSimTags;
import gpu.core.CloudletList;
import gpu.core.Log;
import gpu.core.VmList;
import org.apache.commons.lang3.NotImplementedException;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTag;
import org.cloudbus.cloudsim.core.events.PredicateType;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * for GpuCloudlets. Each {@link GpuCloudlet} must have a {@link GpuVm}
 * associated with it. If a {@link GpuVm} fails to find a {@link GpuHost} in all
 * {@link GpuDatacenter}s, it will be rejected.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuDatacenterBroker extends DatacenterBrokerAbstract {

	/** A structure to maintain VM-GpuCloudlet mapping */
	private HashMap<String, List<GpuCloudlet>> vmGpuCloudletMap;

	/** The number of submitted gpuCloudlets in each vm. */
	private HashMap<String, Integer> vmGpuCloudletsSubmitted;
	public CloudSim simulation;
	/**
	 */
	public GpuDatacenterBroker(final CloudSim simulation, final String name) throws Exception {
		super(simulation);
		if(!name.isEmpty()) {
			setName(name);
		}
		setGpuVmCloudletMap(new HashMap<String, List<GpuCloudlet>>());
		setVmGpuCloudletsSubmitted(new HashMap<String, Integer>());
		this.simulation = simulation;
	}

	@Override
	protected void finishExecution() {
		for (Integer datacenterId : getDatacenterIdsList()) {
			simulation.cancelAll(getDatacenterList().get(datacenterId.intValue()), new PredicateType(CloudSimTag.VGPU_DATACENTER_EVENT));
		}
		super.finishExecution();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the
		// next one
		int requestedVms = 0;
		for (GpuVm vm : (List<GpuVm>) (List<?>) getgpuVmList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId()) && !getgpuVmsCreatedList().contains(vm)) {
				getVmsToDatacentersMap().put((int)vm.getId(), datacenterId);
				send(getDatacenterList().get(datacenterId), vm.getArrivalTime(), CloudSimTag.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}
		getDatacenterRequestedIdsList().add(datacenterId);
		send(getDatacenterList().get(datacenterId), simulation.getMinTimeBetweenEvents(), CloudSimTag.VGPU_DATACENTER_EVENT);
		setgpuVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	@Override
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put((int)characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter(getDatacenterIdsList().get(0));
		}

	}

	@Override
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		Vm vm = VmList.getById(getgpuVmList(), vmId);
		String vmUid = vm.getUid();

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getgpuVmsCreatedList().add(vm);
			setVmsAcks(getVmsAcks() + 1);

			Log.printConcatLine(simulation.clock(), ": ", getName(), ": VM #", vmId, " has been created in Datacenter #",
					datacenterId, ", Host #", vm.getHost().getId());
			System.out.println("{'clock': " + simulation.clock() + ", 'type': 'vm allocation',  'vm': " + vm.getId()
					+ ", 'host': " + vm.getHost().getId() + "}");
			Vgpu vgpu = ((GpuVm) vm).getVgpu();
			if (vgpu != null) {
				Pgpu pgpu = vgpu.getVideoCard().getVgpuScheduler().getPgpuForVgpu(vgpu);
				System.out.println("{'clock': " + simulation.clock() + ", 'type': 'vgpu allocation', 'vgpu': "
						+ vgpu.getId() + ", 'pgpu': " + pgpu.getId() + ", 'vm': " + vm.getId() + "}");
			}
			// VM has been created successfully, submit its cloudlets now.
			List<GpuCloudlet> vmCloudlets = getVmGpuCloudletMap().get(vmUid);
			for (GpuCloudlet cloudlet : vmCloudlets) {
				submitGpuCloudlet(cloudlet);
			}
			getVmGpuCloudletsSubmitted().put(vmUid, vmCloudlets.size());
			// Remove submitted cloudlets from queue
			getgpuCloudletList().removeAll(vmCloudlets);
			getVmGpuCloudletMap().get(vmUid).removeAll(vmCloudlets);
			getVmGpuCloudletMap().remove(vmUid);
		} else {
			Log.printConcatLine(simulation.clock(), ": ", getName(), ": Creation of VM #", vmId,
					" failed in Datacenter #", datacenterId);
			// Create the VM in another datacenter.
			int nextDatacenterId = getDatacenterIdsList()
					.get((getDatacenterIdsList().indexOf(datacenterId) + 1) % getDatacenterIdsList().size());
			if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
				getDatacenterRequestedIdsList().add(nextDatacenterId);
				send(getDatacenterList().get(nextDatacenterId), simulation.getMinTimeBetweenEvents(), CloudSimTag.VGPU_DATACENTER_EVENT);
			}
			// Check for looping datacenters
			if (getDatacenterIdsList().indexOf(nextDatacenterId) != 0) {
				getVmsToDatacentersMap().replace(vmId, nextDatacenterId);
				send(getDatacenterList().get(nextDatacenterId), simulation.getMinTimeBetweenEvents(), CloudSimTag.VM_CREATE_ACK, vm);
			} else {
				// Reject the VM
				System.out.println(
						"{'clock': " + simulation.clock() + ", 'type': 'vm rejection',  'vm': " + vm.getId() + "}");
				List<GpuCloudlet> vmCloudlets = getVmGpuCloudletMap().get(vmUid);
				getgpuCloudletList().removeAll(vmCloudlets);
				getVmGpuCloudletMap().get(vmUid).removeAll(vmCloudlets);
				getVmGpuCloudletMap().remove(vmUid);
			}
		}
	}

	protected void processVmDestroy(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			Log.printLine(simulation.clock() + ": VM #" + vmId + " destroyed in Datacenter #" + datacenterId);
			System.out.println("{'clock': " + simulation.clock() + ", 'type': 'vm deallocation',  'vm': " + vmId + "}");
			setVmsDestroyed(getVmsDestroyed() + 1);
			getVmGpuCloudletsSubmitted().remove(getgpuVmList().get(vmId).getUid());
		} else {
			Log.printLine(simulation.clock() + ": Failed to destroy VM #" + vmId + " in Datacenter #" + datacenterId);
		}
	}


	protected void processCloudletReturn(SimEvent ev) {
		GpuCloudlet cloudlet = (GpuCloudlet) ev.getData();
		getgpuCloudletReceivedList().add(cloudlet);
		Log.printConcatLine(simulation.clock(), ": ", getName(), ": Cloudlet ", cloudlet.getId(), " received");
		GpuVm cloudletVm = (GpuVm) VmList.getById(getgpuVmList(), (int)cloudlet.getVm().getId());
		getVmGpuCloudletsSubmitted().replace(cloudletVm.getUid(),
				getVmGpuCloudletsSubmitted().get(cloudletVm.getUid()) - 1);
		cloudletsSubmitted--;
		if (getVmGpuCloudletsSubmitted().get(cloudletVm.getUid()) == 0) {
			sendNow(getDatacenter(cloudlet.getVm()), CloudSimTag.VM_DESTROY_ACK, cloudletVm);
			getgpuVmsCreatedList().remove(cloudletVm);
		}
		// all cloudlets executed
		if (getgpuCloudletList().isEmpty() && cloudletsSubmitted == 0) {
			Log.printConcatLine(simulation.clock(), ": ", getName(), ": All Jobs executed. Finishing...");
			clearDatacenters();
			finishExecution();
		}
	}

	@Override
	protected Datacenter defaultDatacenterMapper(Datacenter lastDatacenter, Vm vm) {
		return null;
	}

	@Override
	protected Vm defaultVmMapper(Cloudlet cloudlet) {
		return null;
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// VM Destroy answer
		case VM_DESTROY_ACK:
			processVmDestroy(ev);
			break;
		default:
			super.processOtherEvent(ev);
			break;
		}
	}

	protected void submitGpuCloudlet(GpuCloudlet gpuCloudlet) {
		int datacenterId = getVmsToDatacentersMap().get(gpuCloudlet.getVm().getId());
		sendNow(getDatacenterList().get(datacenterId), CloudSimTag.CLOUDLET_SUBMIT, gpuCloudlet);
		getgpuCloudletSubmittedList().add(gpuCloudlet);
		cloudletsSubmitted++;
	}

	@Override
	public void bindgpuCloudletToVm(int cloudletId, int vmId) {
		CloudletList.getById(getgpuCloudletList(), cloudletId).setVm(getVmFromCreatedList(vmId));
	}

	@Override
	public void submitgpuCloudletList(List<? extends GpuCloudlet> list) {
		getgpuCloudletList().addAll(list);
		if (getgpuVmList().isEmpty()) {
			throw new IllegalArgumentException("no vm submitted.");
		}
		for (GpuCloudlet cloudlet : getgpuCloudletList()) {
			if (cloudlet.getVm().getId() < 0) {
				throw new IllegalArgumentException("cloudlet (#" + cloudlet.getId() + ") has no VM.");
			}
			Vm vm = VmList.getById(getgpuVmList(), (int)cloudlet.getVm().getId());
			if (vm == null) {
				throw new IllegalArgumentException("no such vm (Id #" + cloudlet.getVm().getId() + ") exists for cloudlet (#"
						+ cloudlet.getId() + ")");
			}
			getVmGpuCloudletMap().get(vm.getUid()).add((GpuCloudlet) cloudlet);
		}
	}

	@Override
	public void submitgpuVmList(List<? extends Vm> list) {
		super.submitgpuVmList(list);
		for (Vm vm : vmList) {
			if (!getVmGpuCloudletMap().containsKey(vm.getUid())) {
				getVmGpuCloudletMap().put(vm.getUid(), new ArrayList<>());
			}
		}
	}

	/**
	 * @return the vmGpuCloudletsSubmitted
	 */
	protected HashMap<String, Integer> getVmGpuCloudletsSubmitted() {
		return vmGpuCloudletsSubmitted;
	}

	/**
	 * @param vmGpuCloudletsSubmitted the vmGpuCloudletsSubmitted to set
	 */
	protected void setVmGpuCloudletsSubmitted(HashMap<String, Integer> vmGpuCloudletsSubmitted) {
		this.vmGpuCloudletsSubmitted = vmGpuCloudletsSubmitted;
	}

	/**
	 * @return the vmGpuCloudletMap
	 */
	protected HashMap<String, List<GpuCloudlet>> getVmGpuCloudletMap() {
		return vmGpuCloudletMap;
	}

	/**
	 * @param vmGpuCloudletMap the vmGpuCloudletMap to set
	 */
	protected void setGpuVmCloudletMap(HashMap<String, List<GpuCloudlet>> vmGpuCloudletMap) {
		this.vmGpuCloudletMap = vmGpuCloudletMap;
	}

	@Override
	public List<? extends Cloudlet> getCloudletSubmittedList() {
		return getgpuCloudletSubmittedList();
	}

	@Override
	public List<GpuCloudlet> getgpuCloudletSubmittedList() {
		return gpucloudletSubmittedList;
	}

	@Override
	public void bindgpuCloudletToVm() {
		throw new NotImplementedException("not implemented");

	}
}
