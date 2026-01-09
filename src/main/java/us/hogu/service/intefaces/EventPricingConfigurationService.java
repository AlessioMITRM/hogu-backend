package us.hogu.service.intefaces;

import java.util.List;

import us.hogu.model.EventPricingConfiguration;

public interface EventPricingConfigurationService {

	List<EventPricingConfiguration> saveAll(List<EventPricingConfiguration> pricingConfigurations);

	void deleteAll(List<EventPricingConfiguration> pricingConfigurations);

}
