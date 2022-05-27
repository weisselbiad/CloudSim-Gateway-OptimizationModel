package cloudsimMixedPeEnv.allocation;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VideoCard;

public class VideoCardAllocationPolicyNull extends VideoCardAllocationPolicy {

	public VideoCardAllocationPolicyNull(List<? extends VideoCard> videoCards) {
		super(videoCards);
	}

	@Override
	public boolean allocate(Vgpu vgpu, int PCIeBw) {
		throw new NotImplementedException("");
	}

}
