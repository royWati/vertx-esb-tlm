package ekenya.co.ke.vertxspringtlm.dao.wrapper;


import ekenya.co.ke.vertxspringtlm.dao.LegConfigurationManager;
import ekenya.co.ke.vertxspringtlm.dao.LegManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegWrapper {

    private LegManager legManager;
    private LegConfigurationManager configuration;
}
