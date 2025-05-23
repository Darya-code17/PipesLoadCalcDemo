package com.study.pipesloadcalculation.service;

import com.study.pipesloadcalculation.model.Pipe;
import com.study.pipesloadcalculation.model.TruckTrailer;
import com.study.pipesloadcalculation.util.GeometryUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.util.Collections;
import java.util.List;

public class CircularStrategy implements PackingStrategy {
	
	private CalculationService.PackingData packingData;
	
	
	public CircularStrategy(CalculationService.PackingData packingData) {
		this.packingData = packingData;
	}
	
	
	
	@Override
	public boolean fitInContour(Pipe outerPipe, Pipe newPipe) { /// fit in PIPE
		return isFitByCircularMethod(newPipe, packingData.getInnerPipeSpace(outerPipe), outerPipe); // new method
	}
	
	
	@Override
	public boolean fitInContour(TruckTrailer truckTrailer, Pipe newPipe) { /// fit in TRUCK
		return isFitByCircularMethod(newPipe, packingData.getInnerTruckSpace(truckTrailer), packingData.getTruck()); // new method (inscribed circular)
	}
	
	
	private boolean isFitByCircularMethod(Pipe pipeToPlace, Geometry geometryToFit, Pipe outerPipe) { // place inside the Pipe
		
		List<Pipe> listOfInnerPipes = packingData.getPipesOf(outerPipe);
		
		if (listOfInnerPipes.isEmpty()) {
			// outer Pipe is empty inside
			// place at lowest point (centered)
			Coordinate newCenter = new Coordinate(
					outerPipe.getCenter().x,
					outerPipe.getCenter().y + outerPipe.getDiameterInner() / 2 - pipeToPlace.getDiameterOuter() / 2 - 1
			);
			Geometry testCircle = pipeToPlace.toJTSGeometryOuterSpace(newCenter);
			return geometryToFit.contains(testCircle);
		} else {
			// outer Pipe has some other inner Pipes
			Collections.sort(listOfInnerPipes);
			boolean result = false;
			for (Pipe oneOfPipes : listOfInnerPipes.reversed()) {
				result = circularRevolveInside(pipeToPlace, oneOfPipes, geometryToFit);
				if (result) {
					break;
				}
			}
			return result;
		}
	}
	
	
	private boolean isFitByCircularMethod(Pipe pipeToPlace, Geometry geometryToFit, TruckTrailer truckTrailer) {// place inside the truck
		
		List<Pipe> listOfInnerPipes = packingData.getPipesOf(truckTrailer);
		
		if (listOfInnerPipes.isEmpty()) {
			// the truck's container is empty
			// place as lower and lefter as possible
			Coordinate newCenter = new Coordinate(
					(double) pipeToPlace.getDiameterOuter() / 2 + 0, // todo (3) (when there will be list of containers) truckTrailer get x-coord
					truckTrailer.getHeight() - (double) pipeToPlace.getDiameterOuter() / 2
			);
			Geometry testCircle = pipeToPlace.toJTSGeometryOuterSpace(newCenter);
			return geometryToFit.contains(testCircle);
		} else {
			// truck's container already has some Pipes or Boxes inside
			Collections.sort(listOfInnerPipes);
			boolean result = false;
			for (Pipe oneOfPipes : listOfInnerPipes.reversed()) {
				result = circularRevolveInside(pipeToPlace, oneOfPipes, geometryToFit);
				if (result) {
					break;
				}
			}
			return result;
		}
	}
	
	
	
	
	
	private boolean circularRevolveInside(Pipe innerPipe, Pipe outerPipe, Geometry geometryToFit) {
		// n-gon (whose vertices are potential centers of the circles)
		Geometry polygon = GeometryUtils.adaptivePolygon(
				outerPipe.getCenter(),
				(outerPipe.getDiameterOuter() / 2 + innerPipe.getDiameterOuter() / 2 // radius of n-gon
				) * CalculationService.c1 // since pipe's JTS contour isn't perfectly round but edgy, collision calculations are not precise. So there is nessesery to make n-gon a little bigger
				, CalculationService.c2 // coefficient for more vertices of n-gon
		);

//		catchGeometryToDebug((polygon)); // debug
		
		// place innerPipe at every point
		GeometryFactory geometryFactory = new GeometryFactory();
		for (Coordinate c : GeometryUtils.coordinatesOrderRectangular(polygon,false,true)) {
			if (geometryToFit.contains(geometryFactory.createPoint(c))) { // consider a point only if it's within given geometry
				Geometry testCircle = innerPipe.toJTSGeometryOuterSpace(c);
				if (geometryToFit.contains(testCircle)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	
	
	
}
