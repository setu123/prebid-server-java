package org.prebid.server.bidder.grid;

import com.iab.openrtb.request.Imp;
import org.prebid.server.bidder.OpenrtbBidder;
import org.prebid.server.bidder.grid.model.ExtImpGrid;
import org.prebid.server.exception.PreBidException;
import org.prebid.server.json.JacksonMapper;
import org.prebid.server.proto.openrtb.ext.response.BidType;

import java.util.List;

public class GridBidder extends OpenrtbBidder<ExtImpGrid> {

    public GridBidder(String endpointUrl, JacksonMapper mapper) {
        super(endpointUrl, RequestCreationStrategy.SINGLE_REQUEST, ExtImpGrid.class, mapper);
    }

    @Override
    protected Imp modifyImp(Imp imp, ExtImpGrid impExt) {
        if (impExt.getUid() == 0) {
            throw new PreBidException("uid is empty");
        }
        return imp;
    }

    @Override
    protected BidType getBidType(String impId, List<Imp> imps) {
        for (Imp imp : imps) {
            if (imp.getId().equals(impId)) {
                if (imp.getBanner() != null) {
                    return BidType.banner;
                }
                if (imp.getVideo() != null) {
                    return BidType.video;
                }
                throw new PreBidException(String.format("Unknown impression type for ID: %s", impId));
            }
        }
        throw new PreBidException(String.format("Failed to find impression for ID: %s", impId));
    }
}
