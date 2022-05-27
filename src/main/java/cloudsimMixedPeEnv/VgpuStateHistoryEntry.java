package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.vms.VmStateHistoryEntry;

/**
 * @author Ahmad Siavashi
 *
 */
public class VgpuStateHistoryEntry extends VmStateHistoryEntry {

	/**
	 * @see VmStateHistoryEntry#VmStateHistoryEntry
	 */
	public VgpuStateHistoryEntry(double time, double allocatedMips,
			double requestedMips, boolean isInMigration) {
		super(time, allocatedMips, requestedMips, isInMigration);
	}

}
