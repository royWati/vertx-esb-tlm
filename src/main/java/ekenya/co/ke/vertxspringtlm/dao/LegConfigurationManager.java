package ekenya.co.ke.vertxspringtlm.dao;


import ekenya.co.ke.vertxspringtlm.dao.legbody.FixedFields;
import ekenya.co.ke.vertxspringtlm.dao.legbody.LegRequest;
import ekenya.co.ke.vertxspringtlm.dao.legbody.MappingFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @Version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegConfigurationManager {

    private List<MappingFields> clientRequest;
    private List<LegBody> body;
    private List<LegRequest> finalResponse;
    private List<FixedFields> fixedFields;
}
