/*
 * =====================================================================
 * Copyright (C) 2009 Shawn E. Gano
 * 
 * This file is part of JSatTrak.
 * 
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
 */
package Layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.FrameFactory;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.Hashtable;
import javax.media.opengl.GL;
import Satellite.AbstractSatellite;
import Shapes.SphereObject;


/**
 * @author Shawn E. Gano 
 */
public class ECEFModelRenderable implements Renderable
{
    
    // hash of sat props
    Hashtable<String,AbstractSatellite> satHash;

    
    // save globe
    Globe globe;
    
    // altitude to plot ground trace 
    double groundTrackAlt = 10000;
    
    // sphere for Ground Station
    double sphereRadius = 100000;
    SphereObject sphere = new SphereObject(new Vec4(0,0,0,0), sphereRadius, true);
    
    // annotation
    //GlobeAnnotation annotation;
    
    
    /** Creates a new instance of OrbitModel
     * @param satHash
     * @param globe 
     */
    public ECEFModelRenderable(Hashtable<String,AbstractSatellite> satHash, Globe globe)
    {
        this.satHash = satHash;
        this.globe = globe;
        
     }
    
    
    @Override
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        
        javax.media.opengl.GL gl = dc.getGL();
        
        //gl.glEnable(GL.GL_TEXTURE_2D); // removed so the sun shading wouldn't effect line colors
        gl.glPushAttrib(javax.media.opengl.GL.GL_TEXTURE_BIT | javax.media.opengl.GL.GL_ENABLE_BIT | javax.media.opengl.GL.GL_CURRENT_BIT);
        gl.glMatrixMode(javax.media.opengl.GL.GL_MODELVIEW);

        // Added so that the colors wouldn't depend on sun shading
        gl.glDisable(GL.GL_TEXTURE_2D);
              
        // for each satellite
        for(AbstractSatellite sat : satHash.values() ) // search through all sat nodes
        {
            // set color
            Color satColor = sat.getSatColor();
            gl.glColor3f( satColor.getRed()/255.0f , satColor.getGreen()/255.0f , satColor.getBlue()/255.0f ); // COLOR
            
            // GROUND TRACK
            if (sat.isShowGroundTrack3d())
            {
                
                //System.out.println("here");
                
                Vec4 ptLoc;
                // ground trace - lag
                gl.glBegin(GL.GL_LINE_STRIP); //GL_LINE_STRIP
                for (int i = 0; i < sat.getNumGroundTrackLagPts(); i++)
                {
                    // add next Mean of Date lla
                    double[] lla = sat.getGroundTrackLlaLagPt(i);
                    
                    ptLoc = globe.computePointFromPosition(
                    Angle.fromRadiansLatitude(lla[0]), 
                    Angle.fromRadiansLongitude(lla[1]), groundTrackAlt);
                    
                    gl.glVertex3f( (float)ptLoc.x, (float)ptLoc.y , (float)ptLoc.z);
                    
                }
                gl.glEnd();

                // plot lead orbit ground track
                gl.glBegin(GL.GL_LINE_STRIP); //GL_LINE_STRIP
                for (int i = 0; i < sat.getNumGroundTrackLeadPts(); i++)
                {
                    // add next Mean of Date lla
                    double[] lla = sat.getGroundTrackLlaLeadPt(i);
                    
                    ptLoc = globe.computePointFromPosition(
                    Angle.fromRadiansLatitude(lla[0]), 
                    Angle.fromRadiansLongitude(lla[1]), groundTrackAlt);
                    
                    gl.glVertex3f( (float)ptLoc.x, (float)ptLoc.y , (float)ptLoc.z);
                }
                gl.glEnd();
            } // show ground trace
            
            // ECEF ORBIT TRACE
            if (sat.isShow3DOrbitTrace() && !sat.isShow3DOrbitTraceECI() )
            {
                Vec4 ptLoc;
                // ground trace - lag
                gl.glBegin(GL.GL_LINE_STRIP); //GL_LINE_STRIP
                for (int i = 0; i < sat.getNumGroundTrackLagPts(); i++)
                {
                    // add next Mean of Date lla
                    double[] lla = sat.getGroundTrackLlaLagPt(i);
                    
                    ptLoc = globe.computePointFromPosition(
                    Angle.fromRadiansLatitude(lla[0]), 
                    Angle.fromRadiansLongitude(lla[1]), lla[2]);
                    
                    gl.glVertex3f( (float)ptLoc.x, (float)ptLoc.y , (float)ptLoc.z);
                    
                }
                gl.glEnd();

                // plot lead orbit ground track
                gl.glBegin(GL.GL_LINE_STRIP); //GL_LINE_STRIP
                for (int i = 0; i < sat.getNumGroundTrackLeadPts(); i++)
                {
                    // add next Mean of Date lla
                    double[] lla = sat.getGroundTrackLlaLeadPt(i);
                    
                    ptLoc = globe.computePointFromPosition(
                    Angle.fromRadiansLatitude(lla[0]), 
                    Angle.fromRadiansLongitude(lla[1]), lla[2]);
                    
                    gl.glVertex3f( (float)ptLoc.x, (float)ptLoc.y , (float)ptLoc.z);
                }
                gl.glEnd();
                
            } // ecef orbit trace
            
            
        } // for each sat

        // for each GroundStation
        // for each satellite
        
        // LOCATION OF SAT - and orientation
                   // plot position 
//         gl.glRotated(-90, 0.0, 1.0, 0.0); // needs to be before veleocty?
//        for(AbstractSatellite sat : satHash.values() )
//        {
//            double[] xyz = sat.getPosMOD();
//            if(xyz != null)
//            {
//                // 3D model is rendered Here
//                if(sat.isUse3dModel())
//                {
//                    // custom 3D object
//                    if(sat.getThreeDModel() != null) // make sure it is not null
//                    {
//                       //- 
//                        sat.getThreeDModel().render(dc); // render model
//                    }
//                }
//                else
//                {
//                    // default "sphere" for model
//                    sphere.setCenter(-xyz[0], xyz[2], xyz[1]);
//                    sphere.render(dc);
//                }
//            } // if pos is not null
//        }
//        
        gl.glPopAttrib();
        
     } // render
    
    
    private AnnotationAttributes createFontAttribs(Color textColor)
    {
        AnnotationAttributes geoAttr = new AnnotationAttributes();
            geoAttr.setFrameShape(FrameFactory.SHAPE_NONE);  // No frame
            geoAttr.setFont(Font.decode("Arial-ITALIC-12"));
            geoAttr.setTextColor(textColor);
            //geoAttr.setTextAlign(MultiLineTextRenderer.ALIGN_CENTER);
            geoAttr.setDrawOffset(new Point(0, 5)); // centered just above
            //geoAttr.setEffect(MultiLineTextRenderer.EFFECT_OUTLINE);  // Black outline
            geoAttr.setBackgroundColor(Color.BLACK);
            
            return geoAttr;
    } //createFontAttribs
    
    
}
