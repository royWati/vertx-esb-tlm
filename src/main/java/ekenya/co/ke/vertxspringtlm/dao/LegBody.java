package ekenya.co.ke.vertxspringtlm.dao;


import ekenya.co.ke.vertxspringtlm.dao.legbody.FixedFields;
import ekenya.co.ke.vertxspringtlm.dao.legbody.LegRequest;
import ekenya.co.ke.vertxspringtlm.dao.legbody.LegResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegBody {
    private String serviceName;
    private int stepId;
    private List<LegRequest> requestFields;
    private List<FixedFields> fixedFields;
    private LegResponse response;

}
