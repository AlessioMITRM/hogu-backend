package us.hogu.service.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import us.hogu.model.EventPricingConfiguration;
import us.hogu.repository.jpa.EventPricingConfigurationJpa;
import us.hogu.service.intefaces.EventPricingConfigurationService;

@RequiredArgsConstructor
@Repository
public class EventPricingConfigurationServiceImpl implements EventPricingConfigurationService {
	private final EventPricingConfigurationJpa eventPricingConfigurationJpa;
	
	
	@Override
	public List<EventPricingConfiguration> saveAll(List<EventPricingConfiguration> pricingConfigurations) {
		return eventPricingConfigurationJpa.saveAll(pricingConfigurations);
	}

	@Override
	public void deleteAll(List<EventPricingConfiguration> pricingConfigurations) {
		eventPricingConfigurationJpa.deleteAll(pricingConfigurations);
	}
	
	
}
