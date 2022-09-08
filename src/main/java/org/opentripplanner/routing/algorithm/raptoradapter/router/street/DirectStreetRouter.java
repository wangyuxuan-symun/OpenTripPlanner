package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collections;
import java.util.List;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.util.OTPFeature;

public class DirectStreetRouter {

  public static List<Itinerary> route(OtpServerRequestContext serverContext, RouteRequest request) {
    if (request.journey().direct().mode() == StreetMode.NOT_SET) {
      return Collections.emptyList();
    }

    RouteRequest directRequest = request.getStreetSearchRequest(request.journey().direct().mode());
    try (
      var temporaryVertices = new TemporaryVerticesContainer(serverContext.graph(), directRequest)
    ) {
      if (!straightLineDistanceIsWithinLimit(directRequest, temporaryVertices)) {
        return Collections.emptyList();
      }

      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(
        serverContext.traverseVisitor(),
        serverContext.routerConfig().streetRoutingTimeout(),
        serverContext.dataOverlayContext(request)
      );
      List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(directRequest, temporaryVertices);

      // Convert the internal GraphPaths to itineraries
      final GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
        serverContext.transitService().getTimeZone(),
        serverContext.graph().streetNotesService,
        serverContext.graph().ellipsoidToGeoidDifference
      );
      List<Itinerary> response = graphPathToItineraryMapper.mapItineraries(paths);
      ItinerariesHelper.decorateItinerariesWithRequestData(
        response,
        directRequest.wheelchair(),
        directRequest.preferences().wheelchair()
      );
      return response;
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  private static boolean straightLineDistanceIsWithinLimit(
    RouteRequest request,
    TemporaryVerticesContainer vertexContainer
  ) {
    // TODO This currently only calculates the distances between the first fromVertex
    //      and the first toVertex
    double distance = SphericalDistanceLibrary.distance(
      vertexContainer.getFromVertices().iterator().next().getCoordinate(),
      vertexContainer.getToVertices().iterator().next().getCoordinate()
    );
    return distance < calculateDistanceMaxLimit(request);
  }

  /**
   * Calculates the maximum distance in meters based on the maxDirectStreetDuration and the
   * fastest mode available. This assumes that it is not possible to exceed the speed defined in the
   * RoutingRequest.
   */
  private static double calculateDistanceMaxLimit(RouteRequest request) {
    var preferences = request.preferences();
    double distanceLimit;
    StreetMode mode = request.journey().direct().mode();

    double durationLimit = preferences.street().maxDirectDuration().valueOf(mode).toSeconds();

    if (mode.includesDriving()) {
      distanceLimit = durationLimit * preferences.car().speed();
    } else if (mode.includesBiking()) {
      distanceLimit = durationLimit * preferences.bike().speed();
    } else if (mode.includesWalking()) {
      distanceLimit = durationLimit * preferences.walk().speed();
    } else {
      throw new IllegalStateException("Could not set max limit for StreetMode");
    }

    return distanceLimit;
  }
}
