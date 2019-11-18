package ekenya.co.ke.vertxspringtlm.dao.legbody;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestLeg {

    private List<LegRequest> request;

}
