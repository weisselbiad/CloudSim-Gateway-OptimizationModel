package gpu.power;

import gpu.GpuDatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTag;
import org.cloudbus.cloudsim.core.events.PredicateType;

/**
 * {@link PowerGpuDatacenterBroker} extends {@link GpuDatacenterBroker} to
 * handle extra power-events that occur in the simulation.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PowerGpuDatacenterBroker extends GpuDatacenterBroker {
	public CloudSim simulation;

	/**
	 */
	public PowerGpuDatacenterBroker(CloudSim simulation, String name) throws Exception {
		super(simulation, name);
	}

	@Override
	protected void finishExecution() {
		for (Integer datacenterId : getDatacenterIdsList()) {
			simulation.cancelAll(getDatacenterList().get(datacenterId.intValue()),
					new PredicateType(CloudSimTag.GPU_VM_DATACENTER_POWER_EVENT));
		}
		super.finishExecution();
	}

}
