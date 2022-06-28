/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.schedulers.cloudlet;

import gpu.Consts;
import gpu.GpuCloudlet;
import gpu.ResCloudlet;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletExecution;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a policy of scheduling performed by a
 * virtual machine to run its {@link Cloudlet Cloudlets}. Cloudlets execute in
 * time-shared manner in VM. Each VM has to have its own instance of a
 * CloudletScheduler. <b>This scheduler does not consider Cloudlets priorities
 * to define execution order. If actual priorities are defined for Cloudlets,
 * they are just ignored by the scheduler.</b>
 *
 * <p>
 * It also does not perform a preemption process in order to move running
 * Cloudlets to the waiting list in order to make room for other already waiting
 * Cloudlets to run. It just imposes there is not waiting Cloudlet,
 * <b>oversimplifying</b> the problem considering that for a given simulation
 * second <i>t</i>, the total processing capacity of the processor cores (in
 * MIPS) is equally divided by the applications that are using them.
 * </p>
 *
 * <p>In processors enabled with <a href="https://en.wikipedia.org/wiki/Hyper-threading">Hyper-threading technology (HT)</a>,
 * it is possible to run up to 2 processes at the same physical CPU core.
 * However, usually just the Host operating system scheduler (a {@link VmScheduler} assigned to a Host)
 * has direct knowledge of HT to accordingly schedule up to 2 processes to the same physical CPU core.
 * Further, this scheduler implementation
 * oversimplifies a possible HT for the virtual PEs, allowing that more than 2 processes to run at the same core.</p>
 *
 * <p>Since this CloudletScheduler implementation does not account for the
 * <a href="https://en.wikipedia.org/wiki/Context_switch">context switch</a>
 * overhead, this oversimplification impacts tasks completion by penalizing
 * equally all the Cloudlets that are running on the same CPU core.
 * Other impact is that, if there are
 * Cloudlets of the same length running in the same PEs, they will finish
 * exactly at the same time. On the other hand, on a real time-shared scheduler
 * these Cloudlets will finish almost in the same time.
 * </p>
 *
 * <p>
 * As an example, consider a scheduler that has 1 PE that is able to execute
 * 1000 MI/S (MIPS) and is running Cloudlet 0 and Cloudlet 1, each of having
 * 5000 MI of length. These 2 Cloudlets will spend 5 seconds to finish. Now
 * consider that the time slice allocated to each Cloudlet to execute is 1
 * second. As at every 1 second a different Cloudlet is allowed to run, the
 * execution path will be as follows:<br>
 *
 * Time (second): 00 01 02 03 04 05<br>
 * Cloudlet (id): C0 C1 C0 C1 C0 C1<br>
 *
 * As one can see, in a real time-shared scheduler that does not define priorities
 * for applications, the 2 Cloudlets will in fact finish in different times. In
 * this example, one Cloudlet will finish 1 second after the other.
 * </p>
 *
 * <p><b>WARNING</b>: Time-shared schedulers drastically degrade performance
 * of large scale simulations.</p>
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 * @see CloudletSchedulerCompletelyFair
 * @see CloudletSchedulerSpaceShared
 */
public class CloudletSchedulerTimeShared extends CloudletSchedulerAbstract {
    @Serial
    private static final long serialVersionUID = 2115862129708036038L;
    protected int currentCPUs;


    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>For this scheduler, this list is always empty, once the VM PEs
     * are shared across all Cloudlets running inside a VM. Each Cloudlet has
     * the opportunity to use the PEs for a given time-slice.</b></p>
     *
     * @return {@inheritDoc}
     */
    @Override
    public List<CloudletExecution> getCloudletWaitingList() {
        //The method was overridden here just to extend its JavaDoc.
        return super.getCloudletWaitingList();
    }

    /**
     * Moves a Cloudlet that was paused and has just been resumed to the
     * Cloudlet execution list.
     *
     * @param cloudlet the Cloudlet to move from the paused to the exec lit
     * @return the Cloudlet expected finish time
     */
    private double movePausedCloudletToExecListAndGetExpectedFinishTime(final CloudletExecution cloudlet) {
        getCloudletPausedList().remove(cloudlet);
        addCloudletToExecList(cloudlet);
        return cloudletEstimatedFinishTime(cloudlet, getVm().getSimulation().clock());
    }

    @Override
    public double cloudletResume(final Cloudlet cloudlet) {
        return findCloudletInList(cloudlet, getCloudletPausedList())
                .map(this::movePausedCloudletToExecListAndGetExpectedFinishTime)
                .orElse(0.0);
    }

    /**
     * This time-shared scheduler shares the CPU time between all executing
     * cloudlets, giving the same CPU time-slice for each Cloudlet to execute. It
     * always allow any submitted Cloudlets to be immediately added to the
     * execution list. By this way, it doesn't matter what Cloudlet is being
     * submitted, since it will always include it in the execution list.
     *
     * @param cloudlet the Cloudlet that will be added to the execution list.
     * @return always <b>true</b> to indicate that any submitted Cloudlet can be
     * immediately added to the execution list
     */
    @Override
    protected boolean canExecuteCloudletInternal(final CloudletExecution cloudlet) {
        return true;
    }

    @Override
    public double updategpuVmProcessing(double currentTime, List<Double> mipsShare) {
        setgpuCurrentMipsShare(mipsShare);
        double timeSpam = currentTime - getPreviousTime();

        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            rcl.updateCloudletFinishedSoFar((long) (getCapacity(mipsShare) * timeSpam * rcl.getNumberOfPes() * Consts.MILLION));
        }

        if (getCloudletExecList().size() == 0) {
            setPreviousTime(currentTime);
            return 0.0;
        }

        // check finished cloudlets
        double nextEvent = Double.MAX_VALUE;
        List<ResCloudlet> toRemove = new ArrayList<ResCloudlet>();
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            long remainingLength = rcl.getRemainingCloudletLength();
            if (remainingLength == 0) {// finished: remove from the list
                toRemove.add(rcl);
                gpucloudletFinish(rcl);
                continue;
            }
        }
        getCloudletExecList().removeAll(toRemove);

        // estimate finish time of cloudlets
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            double estimatedFinishTime = currentTime
                    + (rcl.getRemainingCloudletLength() / (getCapacity(mipsShare) * rcl.getNumberOfPes()));
            if (estimatedFinishTime - currentTime < simulation.getMinTimeBetweenEvents()) {
                estimatedFinishTime = currentTime + simulation.getMinTimeBetweenEvents();
            }

            if (estimatedFinishTime < nextEvent) {
                nextEvent = estimatedFinishTime;
            }
        }

        setPreviousTime(currentTime);
        return nextEvent;

    }


    @Override
    public double gpucloudletSubmit(GpuCloudlet cloudlet, double fileTransferTime) {
        ResCloudlet rcl = new ResCloudlet(cloudlet);
        rcl.setCloudletStatus(GpuCloudlet.INEXEC);
        for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
            rcl.setMachineAndPeId(0, i);
        }

        getgpuCloudletExecList().add(rcl);

        // use the current capacity to estimate the extra amount of
        // time to file transferring. It must be added to the cloudlet length
        double extraSize = getCapacity(getgpuCurrentMipsShare()) * fileTransferTime;
        long length = (long) (cloudlet.getLength() + extraSize);
        cloudlet.setLength(length);

        return cloudlet.getLength() / getCapacity(getgpuCurrentMipsShare());
    }

    @Override
    public double gpucloudletSubmit(GpuCloudlet cloudlet) {
        return gpucloudletSubmit(cloudlet, 0.0);
    }


    @Override
    public Cloudlet gpucloudletCancel(int cloudletId) {
        boolean found = false;
        int position = 0;

        // First, looks in the finished queue
        found = false;
        for (ResCloudlet rcl : getgpuCloudletFinishedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            return getCloudletFinishedList().remove(position).getCloudlet();
        }

        // Then searches in the exec list
        position=0;
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            ResCloudlet rcl = getgpuCloudletExecList().remove(position);
            if (rcl.getRemainingCloudletLength() == 0) {
                gpucloudletFinish(rcl);
            } else {
                rcl.setCloudletStatus(GpuCloudlet.CANCELED);
            }
            return rcl.getCloudlet();
        }

        // Now, looks in the paused queue
        found = false;
        position=0;
        for (ResCloudlet rcl : getgpuCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                rcl.setCloudletStatus(GpuCloudlet.CANCELED);
                break;
            }
            position++;
        }

        if (found) {
            return getCloudletPausedList().remove(position).getCloudlet();
        }

        return null;    }

    @Override
    public boolean gpucloudletPause(int cloudletId) {
        boolean found = false;
        int position = 0;

        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            // remove cloudlet from the exec list and put it in the paused list
            ResCloudlet rcl = getgpuCloudletExecList().remove(position);
            if (rcl.getRemainingCloudletLength() == 0) {
                gpucloudletFinish(rcl);
            } else {
                rcl.setCloudletStatus(GpuCloudlet.PAUSED);
                getgpuCloudletPausedList().add(rcl);
            }
            return true;
        }
        return false;
    }

    @Override
    public double gpucloudletResume(int cloudletId) {
        boolean found = false;
        int position = 0;

        // look for the cloudlet in the paused list
        for (ResCloudlet rcl : getgpuCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            ResCloudlet rgl = getgpuCloudletPausedList().remove(position);
            rgl.setCloudletStatus(GpuCloudlet.INEXEC);
            getgpuCloudletExecList().add(rgl);

            // calculate the expected time for cloudlet completion
            // first: how many PEs do we have?

            double remainingLength = rgl.getRemainingCloudletLength();
            double estimatedFinishTime = simulation.clock()
                    + (remainingLength / (getCapacity(getgpuCurrentMipsShare()) * rgl.getNumberOfPes()));

            return estimatedFinishTime;
        }

        return 0.0;
    }

    @Override
    public void gpucloudletFinish(ResCloudlet rcl) {
        rcl.setCloudletStatus(GpuCloudlet.SUCCESS);
        rcl.finalizeCloudlet();
        getgpuCloudletFinishedList().add(rcl);
    }

    @Override
    public int getgpuCloudletStatus(int cloudletId) {
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                return rcl.getgpuCloudletStatus();
            }
        }
        for (ResCloudlet rcl : getgpuCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                return rcl.getgpuCloudletStatus();
            }
        }
        return -1;
    }


    @Override
    public boolean gpuisFinishedCloudlets() {
        return getCloudletFinishedList().size() > 0;
    }

    @Override
    public Cloudlet getgpuNextFinishedCloudlet() {
        if (getCloudletFinishedList().size() > 0) {
            return getCloudletFinishedList().remove(0).getCloudlet();
        }
        return null;    }

    @Override
    public int gpurunningCloudlets() {
        return getCloudletExecList().size();
    }

    @Override
    public Cloudlet gpumigrateCloudlet() {
        ResCloudlet rgl = getgpuCloudletExecList().remove(0);
        rgl.finalizeCloudlet();
        return rgl.getCloudlet();    }

    @Override
    public double getgpuTotalUtilizationOfCpu(double time) {
        double totalUtilization = 0;
        for (ResCloudlet gl : getgpuCloudletExecList()) {
            totalUtilization += gl.getCloudlet().getUtilizationOfCpu(time);
        }
        return totalUtilization;
    }

    @Override
    public List<Double> getgpuCurrentRequestedMips() {
        List<Double> mipsShare = new ArrayList<Double>();
        return mipsShare;    }

    @Override
    public double getgpuTotalCurrentAvailableMipsForCloudlet(ResCloudlet rcl, List<Double> mipsShare) {
        return getCapacity(getgpuCurrentMipsShare());
    }

    @Override
    public double getgpuTotalCurrentRequestedMipsForCloudlet(ResCloudlet rcl, double time) {
        return 0;
    }

    @Override
    public double getgpuTotalCurrentAllocatedMipsForCloudlet(ResCloudlet rcl, double time) {
        return 0;
    }

    @Override
    public double getgpuCurrentRequestedUtilizationOfRam() {
        double ram = 0;
        for (ResCloudlet cloudlet : gpucloudletExecList) {
            ram += cloudlet.getCloudlet().getUtilizationOfRam(simulation.clock());
        }
        return ram;
    }

    @Override
    public double getgpuCurrentRequestedUtilizationOfBw() {
        double bw = 0;
        for (ResCloudlet cloudlet : gpucloudletExecList) {
            bw += cloudlet.getCloudlet().getUtilizationOfBw(simulation.clock());
        }
        return bw;
    }
    protected double getCapacity(List<Double> mipsShare) {
        double capacity = 0.0;
        int cpus = 0;
        for (Double mips : mipsShare) {
            capacity += mips;
            if (mips > 0.0) {
                cpus++;
            }
        }
        currentCPUs = cpus;

        int pesInUse = 0;
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            pesInUse += rcl.getNumberOfPes();
        }

        if (pesInUse > currentCPUs) {
            capacity /= pesInUse;
        } else {
            capacity /= currentCPUs;
        }
        return capacity;
    }
}
