/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
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
import org.cloudbus.cloudsim.resources.Pe;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a policy of scheduling performed by a
 * virtual machine to run its {@link Cloudlet Cloudlets}. It considers there
 * will be only one Cloudlet per VM. Other Cloudlets will be in a waiting list.
 * It also considers that the time to transfer Cloudlets to the Vm happens
 * before Cloudlet starts executing. I.e., even though Cloudlets must wait for
 * CPU, data transfer happens as soon as Cloudlets are submitted.
 *
 * <p>
 * <b>This scheduler does not consider Cloudlets priorities to define execution
 * order. If actual priorities are defined for Cloudlets, they are just ignored
 * by the scheduler.</b></p>
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 */
public class CloudletSchedulerSpaceShared extends CloudletSchedulerAbstract {
    @Serial
    private static final long serialVersionUID = 4699085761507163349L;
    protected int currentCpus;
    protected int usedPes;
    @Override
    public double cloudletResume(Cloudlet cloudlet) {
        return findCloudletInList(cloudlet, getCloudletPausedList())
            .map(this::movePausedCloudletToExecListOrWaitingList)
            .orElse(0.0);
    }

    /**
     * Moves a Cloudlet that is being resumed to the exec or waiting List.
     *
     * @param cle the resumed Cloudlet to move
     * @return the time the cloudlet is expected to finish or zero if it was moved to the waiting list
     */
    private double movePausedCloudletToExecListOrWaitingList(final CloudletExecution cle) {
        getCloudletPausedList().remove(cle);

        // it can go to the exec list
        if (isThereEnoughFreePesForCloudlet(cle)) {
            return movePausedCloudletToExecList(cle);
        }

        // No enough free PEs: go to the waiting queue
        /*
         * A resumed cloudlet is not immediately added to the execution list.
         * It is queued so that the next time the scheduler process VM execution,
         * the cloudlet may have the opportunity to run.
         * It goes to the end of the waiting list because other cloudlets
         * could be waiting longer and have priority to execute.
         */
        addCloudletToWaitingList(cle);
        return 0.0;
    }

    /**
     * Moves a paused cloudlet to the execution list.
     *
     * @param cle the cloudlet to be moved
     * @return the time the cloudlet is expected to finish
     */
    private double movePausedCloudletToExecList(final CloudletExecution cle) {
        addCloudletToExecList(cle);
        return cloudletEstimatedFinishTime(cle, getVm().getSimulation().clock());
    }

    /**
     * The space-shared scheduler <b>does not</b> share the CPU time between
     * executing cloudlets. Each CPU ({@link Pe}) is used by another Cloudlet
     * just when the previous Cloudlet using it has finished executing
     * completely. By this way, if there are more Cloudlets than PEs, some
     * Cloudlet will not be allowed to start executing immediately.
     *
     * @param cle {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected boolean canExecuteCloudletInternal(final CloudletExecution cle) {
        return isThereEnoughFreePesForCloudlet(cle);
    }

    @Override
    public double updategpuVmProcessing(double currentTime, List<Double> mipsShare) {
        setgpuCurrentMipsShare(mipsShare);
        double timeSpam = currentTime - getPreviousTime(); // time since last update
        double capacity = 0.0;
        int cpus = 0;

        for (Double mips : mipsShare) { // count the CPUs available to the VMM
            capacity += mips;
            if (mips > 0) {
                cpus++;
            }
        }
        currentCpus = cpus;
        capacity /= cpus; // average capacity of each cpu

        // each machine in the exec list has the same amount of cpu
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            rcl.updateCloudletFinishedSoFar(
                    (long) (capacity * timeSpam * rcl.getNumberOfPes() * Consts.MILLION));
        }

        // no more cloudlets in this scheduler
        if (getCloudletExecList().size() == 0 && getCloudletWaitingList().size() == 0) {
            setPreviousTime(currentTime);
            return 0.0;
        }

        // update each cloudlet
        int finished = 0;
        List<ResCloudlet> toRemove = new ArrayList<ResCloudlet>();
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            // finished anyway, rounding issue...
            if (rcl.getRemainingCloudletLength() == 0) {
                toRemove.add(rcl);
                gpucloudletFinish(rcl);
                finished++;
            }
        }
        getgpuCloudletExecList().removeAll(toRemove);

        // for each finished cloudlet, add a new one from the waiting list
        if (!getCloudletWaitingList().isEmpty()) {
            for (int i = 0; i < finished; i++) {
                toRemove.clear();
                for (ResCloudlet rcl : getgpuCloudletWaitingList()) {
                    if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) {
                        rcl.setCloudletStatus(GpuCloudlet.INEXEC);
                        for (int k = 0; k < rcl.getNumberOfPes(); k++) {
                            rcl.setMachineAndPeId(0, i);
                        }
                        getgpuCloudletExecList().add(rcl);
                        usedPes += rcl.getNumberOfPes();
                        toRemove.add(rcl);
                        break;
                    }
                }
                getgpuCloudletWaitingList().removeAll(toRemove);
            }
        }

        // estimate finish time of cloudlets in the execution queue
        double nextEvent = Double.MAX_VALUE;
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            double remainingLength = rcl.getRemainingCloudletLength();
            double estimatedFinishTime = currentTime + (remainingLength / (capacity * rcl.getNumberOfPes()));
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
    public Cloudlet gpucloudletCancel(int cloudletId) {
        // First, looks in the finished queue
        for (ResCloudlet rcl : getgpuCloudletFinishedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                getgpuCloudletFinishedList().remove(rcl);
                return rcl.getCloudlet();
            }
        }

        // Then searches in the exec list
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                getgpuCloudletExecList().remove(rcl);
                if (rcl.getRemainingCloudletLength() == 0) {
                    gpucloudletFinish(rcl);
                } else {
                    rcl.setCloudletStatus(GpuCloudlet.CANCELED);
                }
                return rcl.getCloudlet();
            }
        }

        // Now, looks in the paused queue
        for (ResCloudlet rcl : getgpuCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                getgpuCloudletPausedList().remove(rcl);
                return rcl.getCloudlet();
            }
        }

        // Finally, looks in the waiting list
        for (ResCloudlet rcl : getgpuCloudletWaitingList()) {
            if (rcl.getCloudletId() == cloudletId) {
                rcl.setCloudletStatus(GpuCloudlet.CANCELED);
                getgpuCloudletWaitingList().remove(rcl);
                return rcl.getCloudlet();
            }
        }

        return null;
    }

    @Override
    public boolean gpucloudletPause(int cloudletId) {
        boolean found = false;
        int position = 0;

        // first, looks for the cloudlet in the exec list
        for (ResCloudlet rcl : getgpuCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            // moves to the paused list
            ResCloudlet rgl = getgpuCloudletExecList().remove(position);
            if (rgl.getRemainingCloudletLength() == 0) {
                gpucloudletFinish(rgl);
            } else {
                rgl.setCloudletStatus(GpuCloudlet.PAUSED);
                getgpuCloudletPausedList().add(rgl);
            }
            return true;

        }

        // now, look for the cloudlet in the waiting list
        position = 0;
        found = false;
        for (ResCloudlet rcl : getgpuCloudletWaitingList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            // moves to the paused list
            ResCloudlet rgl = getgpuCloudletWaitingList().remove(position);
            if (rgl.getRemainingCloudletLength() == 0) {
                gpucloudletFinish(rgl);
            } else {
                rgl.setCloudletStatus(GpuCloudlet.PAUSED);
                getgpuCloudletPausedList().add(rgl);
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
            ResCloudlet rcl = getgpuCloudletPausedList().remove(position);

            // it can go to the exec list
            if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) {
                rcl.setCloudletStatus(GpuCloudlet.INEXEC);
                for (int i = 0; i < rcl.getNumberOfPes(); i++) {
                    rcl.setMachineAndPeId(0, i);
                }

                long size = rcl.getRemainingCloudletLength();
                size *= rcl.getNumberOfPes();
                rcl.getCloudlet().setLength(size);

                getgpuCloudletExecList().add(rcl);
                usedPes += rcl.getNumberOfPes();

                // calculate the expected time for cloudlet completion
                double capacity = 0.0;
                int cpus = 0;
                for (Double mips : getgpuCurrentMipsShare()) {
                    capacity += mips;
                    if (mips > 0) {
                        cpus++;
                    }
                }
                currentCpus = cpus;
                capacity /= cpus;

                long remainingLength = rcl.getRemainingCloudletLength();
                double estimatedFinishTime = simulation.clock()
                        + (remainingLength / (capacity * rcl.getNumberOfPes()));

                return estimatedFinishTime;
            } else {// no enough free PEs: go to the waiting queue
                rcl.setCloudletStatus(GpuCloudlet.QUEUED);

                long size = rcl.getRemainingCloudletLength();
                size *= rcl.getNumberOfPes();
                rcl.getCloudlet().setLength(size);

                getgpuCloudletWaitingList().add(rcl);
                return 0.0;
            }

        }

        // not found in the paused list: either it is in in the queue, executing or not exist
        return 0.0;    }

    @Override
    public double gpucloudletSubmit(GpuCloudlet cloudlet, double fileTransferTime) {
// it can go to the exec list
        if ((currentCpus - usedPes) >= cloudlet.getNumberOfPes()) {
            ResCloudlet rcl = new ResCloudlet(cloudlet);
            rcl.setCloudletStatus(GpuCloudlet.INEXEC);
            for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
                rcl.setMachineAndPeId(0, i);
            }
            getgpuCloudletExecList().add(rcl);
            usedPes += cloudlet.getNumberOfPes();
        } else {// no enough free PEs: go to the waiting queue
            ResCloudlet rcl = new ResCloudlet(cloudlet);
            rcl.setCloudletStatus(GpuCloudlet.QUEUED);
            getgpuCloudletWaitingList().add(rcl);
            return 0.0;
        }

        // calculate the expected time for cloudlet completion
        double capacity = 0.0;
        int cpus = 0;
        for (Double mips : getgpuCurrentMipsShare()) {
            capacity += mips;
            if (mips > 0) {
                cpus++;
            }
        }

        currentCpus = cpus;
        capacity /= cpus;

        // use the current capacity to estimate the extra amount of
        // time to file transferring. It must be added to the cloudlet length
        double extraSize = capacity * fileTransferTime;
        long length = cloudlet.getLength();
        length += extraSize;
        cloudlet.setLength(length);
        return cloudlet.getLength() / capacity;    }

    public double gpucloudletSubmit(GpuCloudlet cloudlet) {
        return gpucloudletSubmit(cloudlet, 0.0);    }


    @Override
    public void gpucloudletFinish(ResCloudlet rcl) {
        rcl.setCloudletStatus(GpuCloudlet.SUCCESS);
        rcl.finalizeCloudlet();
        getgpuCloudletFinishedList().add(rcl);
        usedPes -= rcl.getNumberOfPes();
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

        for (ResCloudlet rcl : getgpuCloudletWaitingList()) {
            if (rcl.getCloudletId() == cloudletId) {
                return rcl.getgpuCloudletStatus();
            }
        }

        return -1;
    }

    @Override
    public boolean gpuisFinishedCloudlets() {
        return getgpuCloudletFinishedList().size() > 0;
    }

    @Override
    public Cloudlet getgpuNextFinishedCloudlet() {
        if (getgpuCloudletFinishedList().size() > 0) {
            return getgpuCloudletFinishedList().remove(0).getCloudlet();
        }
        return null;
    }

    @Override
    public int gpurunningCloudlets() {
        return getgpuCloudletExecList().size();
    }

    @Override
    public Cloudlet gpumigrateCloudlet() {
        ResCloudlet rcl = getgpuCloudletExecList().remove(0);
        rcl.finalizeCloudlet();
        Cloudlet cl = rcl.getCloudlet();
        usedPes -= cl.getNumberOfPes();
        return cl;
    }

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
        if (getCurrentMipsShare() != null) {
            for (Double mips : getgpuCurrentMipsShare()) {
                mipsShare.add(mips);
            }
        }
        return mipsShare;    }

    @Override
    public double getgpuTotalCurrentAvailableMipsForCloudlet(ResCloudlet rcl, List<Double> mipsShare) {
        /*@todo The param rcl is not being used.*/
        double capacity = 0.0;
        int cpus = 0;
        for (Double mips : mipsShare) { // count the cpus available to the vmm
            capacity += mips;
            if (mips > 0) {
                cpus++;
            }
        }
        currentCpus = cpus;
        capacity /= cpus; // average capacity of each cpu
        return capacity;    }

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
        return 0;
    }

    @Override
    public double getgpuCurrentRequestedUtilizationOfBw() {
        return 0;
    }
}
