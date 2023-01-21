package tbt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.GHPoint;

/**
 * The Sample program to demonstrate the GraphHopper library. which takes the
 * list of mock coordinates from txt file (attached in the resource directory).
 * The mock coordinates will be iterated to every 5000ms and the current iterated
 * coordinate will be considered as a current location (to mimick the GPS event during the travel).
 * 
 * Also it uses the South Indian region OSM data downloaded from https://download.geofabrik.de/asia/india/southern-zone.html
 * 
 * @author Kavin
 *
 */
public class TurnByTurnNavigation {

	private static List<GHPoint> travelPointsMock = mockTravelPath();

	public static void main(String[] args) throws InterruptedException {
		// Create a new GraphHopper object
		GraphHopper hopper = new GraphHopper();
		// Path for the OSM data file
		hopper.setOSMFile("D://Downloads/southern-zone-latest.osm.pbf");
		// Location to store the parsed Map data
		hopper.setGraphHopperLocation("D://Downloads/graphhopper/");
		hopper.setProfiles(new Profile("car"));
		hopper.importOrLoad();

		// Create GraphHopper request, first point from the mock route is considered
		// as starting point for the direction API and last point will be considered as
		// end point of the Direction API
		GHRequest request = new GHRequest();
		request.addPoint(travelPointsMock.get(0));
		request.addPoint(travelPointsMock.get(travelPointsMock.size() - 1));
		request.setProfile("car");

		// Get a route response from routing engine
		GHResponse response = hopper.route(request);

		// Check if the response is successful
		if (response.hasErrors()) {
			// Handle errors
		} else {
			// Get the path from the response
			ResponsePath path = response.getBest();
			Translation tr = hopper.getTranslationMap().getWithFallBack(Locale.UK);
			// Get the turn-by-turn instructions
			InstructionList instructions = path.getInstructions();
			DistanceCalcEarth distanceCalc = new DistanceCalcEarth();
			int nextInstrcutionIndex = 1;
			for (GHPoint currentPoint : travelPointsMock) {
				Instruction currentinstrcution = instructions.get(nextInstrcutionIndex);
				PointList nextInstrcutionPoint = currentinstrcution.getPoints();
				GHPoint point = nextInstrcutionPoint.get(0);
				double lat = point.getLat();
				double lon = point.getLon();

				double distance = distanceCalc.calcDist(currentPoint.lat, currentPoint.lon, lat, lon);
				System.out.println(currentinstrcution.getTurnDescription(tr) + " in " + distance + " meters");
				// Distance between the current location and point from the instruction is less
				// than 100 then it will be considered as we reached the turn. So it will take
				// the next instruction for next trun
				// 100M threshold need to be validated in real world scenario
				if (distance < 100) {
					nextInstrcutionIndex += 1;

				}
				if (nextInstrcutionIndex >= instructions.size()) {
					return;
				}
				Thread.sleep(5000L);
			}
		}

	}

	/*
	 * Load the list of GeoCordinates from txt file, which will be used to mock the
	 * GPS events.
	 */
	private static List<GHPoint> mockTravelPath() {
		List<GHPoint> points = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(
				TurnByTurnNavigation.class.getClassLoader().getResource("mocklocations.txt").getPath()))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				double lat = Double.parseDouble(parts[0].trim());
				double lon = Double.parseDouble(parts[1].trim());
				GHPoint point = new GHPoint(lat, lon);
				points.add(point);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return points;
	}

}
