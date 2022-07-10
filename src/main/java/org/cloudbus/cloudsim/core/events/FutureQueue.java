/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.core.events;

import gpu.GpuDatacenter;
import gpu.GpuDatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An {@link EventQueue} that stores future simulation events.
 * It uses a {@link TreeSet} in order ensure the events
 * are stored ordered. Using a {@link LinkedList}
 * as defined by {@link DeferredQueue} to improve performance
 * doesn't work for this queue.
 *
 * @author Marcos Dias de Assuncao
 * @author Manoel Campos da Silva Filho
 * @see TreeSet
 * @since CloudSim Toolkit 1.0
 */
public class FutureQueue implements EventQueue {

    /**
     * The sorted set of events.
     */
    private final SortedSet<SimEvent> sortedSet = new TreeSet<>();

    /** @see #getSerial() */
    private long serial;

    private long lowestSerial;

    /** @see #getMaxEventsNumber() */
    private long maxEventsNumber;

    @Override
    public void addEvent(final SimEvent newEvent) {
        if(checkEvent(newEvent)){
        newEvent.setSerial(serial++);
        sortedSet.add(newEvent);
        maxEventsNumber = Math.max(maxEventsNumber, sortedSet.size());
    }}
    public boolean checkEvent(SimEvent event){
        Class sr =event.getSource().getClass();
        Class dst = event.getDestination().getClass();
        if(dst.equals(GpuDatacenter.class) || dst.equals(GpuDatacenterBroker.class)){
            if(sr.equals(DatacenterSimple.class) || sr.equals(DatacenterBrokerSimple.class)){
            return false;}else return true;
        }
        else if(dst.equals(DatacenterSimple.class)|| dst.equals(DatacenterBrokerSimple.class)){
            if(sr.equals(GpuDatacenter.class) || sr.equals(GpuDatacenterBroker.class)){return false;}
            else return true;}


        else return true;

    }
    /**
     * Adds a new event to the head of the queue.
     *
     * @param newEvent The event to be put in the queue.
     */
    public void addEventFirst(final SimEvent newEvent) {
        newEvent.setSerial(--lowestSerial);
        sortedSet.add(newEvent);
    }

    @Override
    public Iterator<SimEvent> iterator() {
        return sortedSet.iterator();
    }

    @Override
    public Stream<SimEvent> stream() {
        return sortedSet.stream();
    }

    @Override
    public int size() {
        return sortedSet.size();
    }

    @Override
    public boolean isEmpty() {
        return sortedSet.isEmpty();
    }

    /**
     * Removes the event from the queue.
     *
     * @param event the event
     * @return true if successful; false if not event was removed
     */
    public boolean remove(final SimEvent event) {
        return sortedSet.remove(event);
    }

    /**
     * Removes all the events from the queue.
     *
     * @param events the events
     * @return true if successful; false if not event was removed
     */
    public boolean removeAll(final Collection<SimEvent> events) {
        return sortedSet.removeAll(events);
    }

    public boolean removeIf(final Predicate<SimEvent> predicate){
        return sortedSet.removeIf(predicate);
    }

    @Override
    public SimEvent first() throws NoSuchElementException {
        return sortedSet.first();
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        sortedSet.clear();
    }

    /** Gets an incremental number used for {@link SimEvent#getSerial()} event attribute. */
    public long getSerial() {
        return serial;
    }

    /**
     * Maximum number of events that have ever existed at the same time
     * inside the queue.
     */
    public long getMaxEventsNumber() {
        return maxEventsNumber;
    }
}
