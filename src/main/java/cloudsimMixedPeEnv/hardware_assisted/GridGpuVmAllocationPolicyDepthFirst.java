package cloudsimMixedPeEnv.hardware_assisted;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.hosts.Host;
import cloudsimMixedPeEnv.Pgpu;

public abstract class GridGpuVmAllocationPolicyDepthFirst extends GridGpuVmAllocationPolicyBreadthFirst {

	public GridGpuVmAllocationPolicyDepthFirst(List<? extends Host> list) {
		super(list);
	}

	@Override
	protected void sortPgpusList(List<Pair<Pgpu, Integer>> pgpuList) {
		Collections.sort(pgpuList, Collections.reverseOrder(new Comparator<Pair<Pgpu, Integer>>() {
			public int compare(Pair<Pgpu, Integer> p1, Pair<Pgpu, Integer> p2) {
				return Integer.compare(p1.getValue(), p2.getValue());
			};
		}));
	}

}
