package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.AbstractMachine;
import org.cloudbus.cloudsim.core.CustomerEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.resources.Processor;
import org.cloudbus.cloudsim.resources.Resource;
import org.cloudbus.cloudsim.resources.ResourceManageable;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.vms.*;
import org.cloudsimplus.autoscaling.HorizontalVmScaling;
import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.listeners.VmDatacenterEventInfo;
import org.cloudsimplus.listeners.VmHostEventInfo;

import java.util.List;

/**
 * 
 * {@link GpuVm} extends {@link Vm} to represent a VM with GPU requirements.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuVm extends VmSimple implements Vm {
	private int userId;
	/**
	 * Describes vm's type. A type can be associated with a configuration, therefore
	 * it helps identifying the vm
	 */
	private String type;

	/**
	 * Denotes the time in which the VM enters the system.
	 */
	private double arrivalTime;

	/**
	 * The Vgpu associated with the Vm
	 */
	private Vgpu vgpu;

	/**
	 * @param *vgpu the vgpu associated with this VM. Pass null in case of no vgpu.
	 * @param *type specifies the type of the vm
	 * @see Vm
	 */
	public GpuVm( final long mipsCapacity, final long numberOfPes, final CloudletScheduler cloudletScheduler) {
		super(mipsCapacity, numberOfPes, cloudletScheduler);

		setType(type);
		setArrivalTime(0.0);
	}

	public GpuVm(int userId,final long mipsCapacity, final long numberOfPes, final CloudletScheduler cloudletScheduler) {
		super(mipsCapacity, numberOfPes, cloudletScheduler);

		setType(type);
		setUserId(userId);
		setArrivalTime(0.0);
	}



	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	protected void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the vgpu
	 */
	public Vgpu getVgpu() {
		return vgpu;
	}

	/**
	 * @param vgpu the vgpu to set
	 */
	public void setVgpu(Vgpu vgpu) {
		this.vgpu = vgpu;
		if (vgpu.getVm() == null) {
			vgpu.setGpuVm(this);
		}
	}

	public boolean hasVgpu() {
		return getVgpu() != null;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	protected void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * Gets the ID of the owner of the VM.
	 *
	 * @return VM's owner ID
	 * @pre $none
	 * @post $none
	 */
	public int getUserId() {
		return userId;
	}

}
