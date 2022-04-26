package org.opentripplanner.api.mapping;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.model.plan.Itinerary;

public class ItineraryMapper {

  private final LegMapper legMapper;

  public ItineraryMapper(Locale locale, boolean addIntermediateStops) {
    this.legMapper = new LegMapper(locale, addIntermediateStops);
  }

  public List<ApiItinerary> mapItineraries(Collection<Itinerary> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(this::mapItinerary).collect(Collectors.toList());
  }

  public ApiItinerary mapItinerary(Itinerary domain) {
    if (domain == null) {
      return null;
    }
    ApiItinerary api = new ApiItinerary();

    api.duration = (long) domain.durationSeconds;
    api.startTime = GregorianCalendar.from(domain.startTime());
    api.endTime = GregorianCalendar.from(domain.endTime());
    api.walkTime = domain.nonTransitTimeSeconds;
    api.transitTime = domain.transitTimeSeconds;
    api.waitingTime = domain.waitingTimeSeconds;
    api.walkDistance = domain.nonTransitDistanceMeters;
    api.generalizedCost = domain.generalizedCost;
    api.elevationLost = domain.elevationLost;
    api.elevationGained = domain.elevationGained;
    api.transfers = domain.nTransfers;
    api.tooSloped = domain.tooSloped;
    api.arrivedAtDestinationWithRentedBicycle = domain.arrivedAtDestinationWithRentedVehicle;
    api.fare = domain.fare;
    api.legs = legMapper.mapLegs(domain.legs);
    api.systemNotices = SystemNoticeMapper.mapSystemNotices(domain.systemNotices);
    api.accessibilityScore = domain.accessibilityScore;

    return api;
  }
}
