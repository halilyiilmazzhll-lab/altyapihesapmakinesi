package com.example.egimhesabi.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class CadGeometryTest {
    @Test fun scaleMatchesWorldDistanceForTallAndWideSites() {
        for ((rx,ry) in listOf(100.0 to 1000.0,1000.0 to 100.0,1.0 to 1.0)) {
            val v=CadViewport(0.0,ry,rx,ry,720.0,840.0)
            for (zoom in listOf(.2,1.0,50.0)) {
                val meters=CadGeometry.scaleMeters(v.baseScale*zoom,144.0)
                val a=v.project(0.0,0.0,zoom,CadPoint(25.0,-50.0))
                val b=v.project(meters,0.0,zoom,CadPoint(25.0,-50.0))
                assertEquals(meters*v.baseScale*zoom,hypot(b.x-a.x,b.y-a.y),1e-8)
                assertTrue(meters>0)
            }
        }
    }
    @Test fun arrowWingsRemainBehindTargetInEveryDirection() {
        val start=CadPoint(0.0,0.0)
        for (end in listOf(CadPoint(100.0,0.0),CadPoint(-100.0,0.0),CadPoint(0.0,100.0),CadPoint(10.0,-100.0))) {
            val wings=CadGeometry.arrowWings(start,end,10.0)
            for(w in listOf(wings.first,wings.second)) assertTrue((w.x-end.x)*end.x+(w.y-end.y)*end.y<0)
        }
    }
    @Test fun combinedPanAndZoomKeepsPointUnderMovingFingers() {
        val updated=CadGeometry.transformPan(CadPoint(0.0,0.0),CadPoint(100.0,100.0),CadPoint(10.0,20.0),2.0)
        assertEquals(110.0,100.0*2+updated.x,1e-8)
        assertEquals(120.0,100.0*2+updated.y,1e-8)
    }
    @Test fun hitTestUsesFiniteSegmentAndHandlesOverlappingEndpoints() {
        assertEquals(10.0,CadGeometry.segmentDistance(CadPoint(50.0,10.0),CadPoint(0.0,0.0),CadPoint(100.0,0.0)),1e-8)
        assertEquals(50.0,CadGeometry.segmentDistance(CadPoint(150.0,0.0),CadPoint(0.0,0.0),CadPoint(100.0,0.0)),1e-8)
        assertEquals(5.0,CadGeometry.segmentDistance(CadPoint(3.0,4.0),CadPoint(0.0,0.0),CadPoint(0.0,0.0)),1e-8)
    }
}
