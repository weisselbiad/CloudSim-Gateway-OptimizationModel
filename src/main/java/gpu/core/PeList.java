package gpu.core;


import java.util.List;

import gpu.GpuPe;
import gpu.GpuVm;
import gpu.core.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;


/**
 * PeList is a collection of operations on lists of PEs.
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class PeList {

    /**
     * Gets a {@link Pe} with a given id.
     *
     * @param peList the PE list where to get a given PE
     * @param id the id of the PE to be get
     * @return the PE with the given id or null if not found
     * @pre id >= 0
     * @post $none
     */
    public static <T extends GpuPe> GpuPe getById(List<T> peList, int id) {
                /*@todo such kind of search would be made using a HashMap
                (to avoid always iterating over the list),
                where the key is the id of the object and the value the object
                itself. The same occurs for lists of hosts and VMs.*/
        for (GpuPe pe : peList) {
            if (pe.getId() == id) {
                return pe;
            }
        }
        return null;
    }

    /**
     * Gets MIPS Rating of a PE with a given ID.
     *
     * @param peList the PE list where to get a given PE
     * @param id the id of the PE to be get
     * @return the MIPS rating of the PE or -1 if the PE was not found
     * @pre id >= 0
     * @post $none
     */
    public static <T extends GpuPe> int getMips(List<T> peList, int id) {
        GpuPe pe = getById(peList, id);
        if (pe != null) {
            return (int)pe.getMips();
        }
        return -1;
    }

    /**
     * Gets total MIPS Rating for all PEs.
     *
     * @param peList the pe list
     * @return the total MIPS Rating
     * @pre $none
     * @post $none
     */
    public static <T extends GpuPe> int getTotalMips(List<T> peList) {
        int totalMips = 0;
        for (GpuPe pe : peList) {
            totalMips += pe.getMips();
        }
        return totalMips;
    }

    /**
     * Gets the max utilization percentage among all PEs.
     *
     * @param peList the pe list
     * @return the max utilization percentage
     */
    public static <T extends GpuPe> double getMaxUtilization(List<T> peList) {
        double maxUtilization = 0;
        for (GpuPe pe : peList) {
            double utilization = pe.getPeProvisioner().getUtilization();
            if (utilization > maxUtilization) {
                maxUtilization = utilization;
            }
        }
        return maxUtilization;
    }

    /**
     * Gets the max utilization percentage among all PEs allocated to a VM.
     *
     * @param vm the vm to get the maximum utilization percentage
     * @param peList the pe list
     * @return the max utilization percentage
     */
    public static <T extends GpuPe> double getMaxUtilizationAmongVmsPes(List<T> peList, GpuVm vm) {
        double maxUtilization = 0;
        for (GpuPe pe : peList) {
            if (pe.getPeProvisioner().getAllocatedMipsForVm(vm).isEmpty()) {
                continue;
            }
            double utilization = pe.getPeProvisioner().getUtilization();
            if (utilization > maxUtilization) {
                maxUtilization = utilization;
            }
        }
        return maxUtilization;
    }

    /**
     * Gets the first <tt>FREE</tt> PE which.
     *
     * @param peList the PE list
     * @return the first free PE or null if not found
     * @pre $none
     * @post $none
     */
    public static <T extends GpuPe> GpuPe getFreePe(List<T> peList) {
        for (GpuPe pe : peList) {
            if (pe.getGpuPeStatus() == GpuPe.FREE) {
                return pe;
            }
        }
        return null;
    }

    /**
     * Gets the number of <tt>FREE</tt> (non-busy) PEs.
     *
     * @param peList the PE list
     * @return number of free PEs
     * @pre $none
     * @post $result >= 0
     */
    public static <T extends GpuPe> int getNumberOfFreePes(List<T> peList) {
        int cnt = 0;
        for (GpuPe pe : peList) {
            if (pe.getGpuPeStatus() == GpuPe.FREE) {
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * Sets a PE status.
     *
     * @param status the PE status, either <tt>Pe.FREE</tt> or <tt>Pe.BUSY</tt>
     * @param id the id of the PE to be set
     * @param peList the PE list
     * @return <tt>true</tt> if the PE status has been changed, <tt>false</tt> otherwise (PE id might
     *         not be exist)
     * @pre peID >= 0
     * @post $none
     */
    public static <T extends GpuPe> boolean setPeStatus(List<T> peList, int id, int status) {
        GpuPe pe = getById(peList, id);
        if (pe != null) {
            pe.setStatus(status);
            return true;
        }
        return false;
    }

    /**
     * Gets the number of <tt>BUSY</tt> PEs.
     *
     * @param peList the PE list
     * @return number of busy PEs
     * @pre $none
     * @post $result >= 0
     */
    public static <T extends GpuPe> int getNumberOfBusyPes(List<T> peList) {
        int cnt = 0;
        for (GpuPe pe : peList) {
            if (pe.getGpuPeStatus() == GpuPe.BUSY) {
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * Sets the status of PEs of a host to FAILED or FREE. NOTE: <tt>resName</tt> and
     * <tt>hostId</tt> are used for debugging purposes, which is <b>ON</b> by default.
     * Use {@link (boolean)} if you do not want this information.
     *
     * @param peList the host's PE list to be set as failed or free
     * @param resName the name of the resource
     * @param hostId the id of the host
     * @param failed true if the host's PEs have to be set as FAILED, false
     * if they have to be set as FREE.
     * @see #setStatusFailed(java.util.List, boolean)
     */
    public static <T extends GpuPe> void setStatusFailed(
            List<T> peList,
            String resName,
            int hostId,
            boolean failed) {
        String status = null;
        if (failed) {
            status = "FAILED";
        } else {
            status = "WORKING";
        }

        Log.printConcatLine(resName, " - Machine: ", hostId, " is ", status);

        setStatusFailed(peList, failed);
    }

    /**
     * Sets the status of PEs of a host to FAILED or FREE.
     *
     * @param peList the host's PE list to be set as failed or free
     * @param failed true if the host's PEs have to be set as FAILED, false
     * if they have to be set as FREE.
     */
    public static <T extends GpuPe> void setStatusFailed(List<T> peList, boolean failed) {
        // a loop to set the status of all the PEs in this machine
        for (GpuPe pe : peList) {
            if (failed) {
                pe.setStatus(GpuPe.FAILED);
            } else {
                pe.setStatus(GpuPe.FREE);
            }
        }
    }

}