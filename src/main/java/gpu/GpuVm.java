package gpu;

import org.cloudbus.cloudsim.resources.ResourceManageable;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.List;

/**
 * 
 * {@link GpuVm} extends {@link Vm} to represent a VM with GPU requirements.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuVm extends VmSimple implements Vm {

	/**
	 * Describes vm's type. A type can be associated with a configuration, therefore
	 * it helps identifying the vm
	 */
	private String type;

	/**
	 * Denotes the time in which the VM enters the system.
	 */
	private double arrivalTime;

	/** The Vgpu associated with the Vm */
	private Vgpu vgpu;
	private long currentAllocatedBw;
	private int currentAllocatedRam;
	/**
	 * @see Vm
	 *
	 *            the vgpu associated with this VM. Pass null in case of no vgpu.
	 * @param type
	 *            specifies the type of the vm
	 */
	public GpuVm(int id, double mips, int numberOfPes, int ram, long bw, long size, String type,
			CloudletScheduler cloudletScheduler) {
		super( mips, numberOfPes);
		setId(id);
		setRam(ram);
		setBw(bw);
		setSize(size);
		setCloudletScheduler(cloudletScheduler);
		setType(type);
		setArrivalTime(0.0);
		setCurrentAllocatedBw(0);
		setCurrentAllocatedRam(0);
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
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
	 * @param vgpu
	 *            the vgpu to set
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

	public double updategpuVmProcessing(double currentTime, List<Double> mipsShare) {
		if (mipsShare != null) {
			return getCloudletScheduler().updategpuVmProcessing(currentTime, mipsShare);
		}
		return 0.0;
	}
	public void setCurrentAllocatedBw(long currentAllocatedBw) {
		this.currentAllocatedBw = currentAllocatedBw;
	}
	public long getCurrentAllocatedBw() {
		return currentAllocatedBw;
	}

	public int getCurrentAllocatedRam() {
		return currentAllocatedRam;
	}

	/**
	 * Sets the current allocated ram.
	 *
	 * @param currentAllocatedRam the new current allocated ram
	 */
	public void setCurrentAllocatedRam(int currentAllocatedRam) {
		this.currentAllocatedRam = currentAllocatedRam;
	}




}
