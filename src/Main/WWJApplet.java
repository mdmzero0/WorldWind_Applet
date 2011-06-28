package main;
/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.examples.ClickAndGoSelectListener;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.*;
import netscape.javascript.JSObject;
//Start of JSatTrak imports
import Bodies.Sun;
import Utilities.Time;
import java.util.Hashtable;
import Satellite.AbstractSatellite;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import Satellite.JSatTrakTimeDependent;
import java.util.Vector;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import Satellite.CustomSatellite;
import Layers.*;
import gov.nasa.worldwind.examples.sunlight.*;
import gov.nasa.worldwind.render.*;
import java.util.ArrayList;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.awt.AWTInputHandler;
import gov.nasa.worldwind.view.BasicView;
import View.*;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a base application framework for simple WorldWind applets.
 * <p/>
 * A simple applet which runs World Wind with a StatusBar at the bottom and lets javascript set some view attributes.
 *
 * @author Patrick Murris
 * @version $Id: WWJApplet.java 15441 2011-05-14 08:50:57Z tgaskins $
 */

public class WWJApplet extends JApplet
{
    protected WorldWindowGLCanvas wwd;
    protected RenderableLayer labelsLayer;
    
    /* Variables being added for the JSatTrak addition to the WorldWind Applet. 
     * Starting with: Sun and Moon bodies (update: Moon may not be used?)
     * Also adding: Time
     * Also adding: Hashtable for satellites
     */
    // Sun object
    private Sun sun;
    
    //Time!
    Time currentJulianDate = new Time(); // current sim or real time (Julian Date)
    
    // date formats for displaying and reading in
    private SimpleDateFormat dateformat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss.SSS z");
    private SimpleDateFormat dateformatShort1 = new SimpleDateFormat("dd MMM y H:m:s.S z");
    private SimpleDateFormat dateformatShort2 = new SimpleDateFormat("dd MMM y H:m:s z"); // no Milliseconds
    
    // scenario epoch time settings
    private boolean epochTimeEqualsCurrentTime = true; // uses current time for scenario epoch (reset button)
    private Time scenarioEpochDate = new Time(); // scenario epoch if epochTimeisCurrentTime = false
    
    // store local time zone for printing
    TimeZone localTZ = TimeZone.getDefault();
    
    //Scenario Update variables
    int currentPlayDirection = 0; // 1= forward, -1=backward, =0-no animation step, but can update time (esentially a graphic ini or refresh)
    private double animationSimStepSeconds = 1.0; // dt in Days per animation step/time update
    
    //Animation variables
    private boolean stopHit = false;
    private long lastFPSms;
    private double fpsAnimation;
    private Timer playTimer;
    private int realTimeAnimationRefreshRateMs = 1000; // refresh rate for real time animation
    private int nonRealTimeAnimationRefreshRateMs = 50; // refresh rate for non-real time animation
    private int animationRefreshRateMs = nonRealTimeAnimationRefreshRateMs; // (current)Milliseconds
     
    //Satellites!
    // hashtable to store all the statelites currently being processed
    private Hashtable<String,AbstractSatellite> satHash = new Hashtable<String,AbstractSatellite>();

    // time dependent objects that should be update when time is updated -- NEED TO BE SAVED?
    Vector<JSatTrakTimeDependent> timeDependentObjects = new Vector<JSatTrakTimeDependent>();
    
    //Stuff from J3DEarthPanel
    ECIRenderableLayer eciLayer; // ECI layer for plotting in ECI coordinates
    ECEFRenderableLayer ecefLayer; // ECEF layer for plotting in ECEF coordinates
    EcefTimeDepRenderableLayer timeDepLayer;
    OrbitModelRenderable orbitModel; // renderable object for plotting
    ECEFModelRenderable ecefModel;
    
    private boolean viewModeECI = true; // view mode - ECI (true) or ECEF (false)
    StarsLayer starsLayer;
    
    // view mode options
    private boolean modelViewMode = false; // default false
    private String modelViewString = ""; // to hold name of satellite to view when modelViewMode=true
    private double modelViewNearClip = 10000; // clipping pland for when in Model View mode
    private double modelViewFarClip = 5.0E7;
    private boolean smoothViewChanges = true; // for 3D view smoothing (only is set after model/earth view has been changed -needs to be fixed)
    // near/far clipping plane distances for 3d windows (can effect render speed and if full orbit is shown)
     private double farClippingPlaneDistOrbit = -1;//200000000d; // good out to geo, but slow for LEO, using AutoClipping plane view I made works better
     private double nearClippingPlaneDistOrbit = -1; // -1 value Means auto adjusting

    ViewControlsLayer viewControlsLayer;

    // sun shader
    private RectangularNormalTessellator tessellator;
    private LensFlareLayer lensFlareLayer;
    private AtmosphereLayer atmosphereLayer;
    private SunPositionProvider spp;
    private boolean sunShadingOn = false; // controls if sun shading is used

    // ECI grid
    private ECIRadialGrid eciRadialGrid = new ECIRadialGrid();

    
    public WWJApplet()
    {
        // create Sun object
        sun = new Sun(currentJulianDate.getMJD());
        
        // first call to update time to current time:
        currentJulianDate.update2CurrentTime(); //update();// = getCurrentJulianDate(); // ini time
        
        // just a little touch up -- remove the milliseconds from the time
        int mil = currentJulianDate.get(Time.MILLISECOND);
        currentJulianDate.add(Time.MILLISECOND,1000-mil); // remove the milliseconds (so it shows an even second)
        
        // set time string format
        currentJulianDate.setDateFormat(dateformat);
        scenarioEpochDate.setDateFormat(dateformat);
        
        updateTime(); // update plots
                
    }

    public void init()
    {
        // create Sun object
        sun = new Sun(currentJulianDate.getMJD());
        try
        {
            // Check for initial configuration values
            String value = getParameter("InitialLatitude");
            if (value != null)
                Configuration.setValue(AVKey.INITIAL_LATITUDE, Double.parseDouble(value));
            value = getParameter("InitialLongitude");
            if (value != null)
                Configuration.setValue(AVKey.INITIAL_LONGITUDE, Double.parseDouble(value));
            value = getParameter("InitialAltitude");
            if (value != null)
                Configuration.setValue(AVKey.INITIAL_ALTITUDE, Double.parseDouble(value));
            value = getParameter("InitialHeading");
            if (value != null)
                Configuration.setValue(AVKey.INITIAL_HEADING, Double.parseDouble(value));
            value = getParameter("InitialPitch");
            if (value != null)
                Configuration.setValue(AVKey.INITIAL_PITCH, Double.parseDouble(value));

            // Create World Window GL Canvas
            this.wwd = new WorldWindowGLCanvas();
            this.getContentPane().add(this.wwd, BorderLayout.CENTER);
            wwd = this.wwd;
            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);

            // Add a renderable layer for application labels
            this.labelsLayer = new RenderableLayer();
            this.labelsLayer.setName("Labels");
            insertBeforeLayerName(this.wwd, this.labelsLayer, "Compass");
            
            // add EcefTimeDepRenderableLayer layer
            timeDepLayer = new EcefTimeDepRenderableLayer(currentJulianDate.getMJD(),sun);
            m.getLayers().add(timeDepLayer);

            // add ECI Layer -- FOR SOME REASON IF BEFORE EFEF and turned off ECEF Orbits don't show up!! Coverage effecting this too, strange
            eciLayer = new ECIRenderableLayer(currentJulianDate.getMJD()); // create ECI layer
            orbitModel = new OrbitModelRenderable(satHash, wwd.getModel().getGlobe());
            eciLayer.addRenderable(orbitModel); // add renderable object
            eciLayer.setCurrentMJD(currentJulianDate.getMJD()); // update time again after adding renderable
            m.getLayers().add(eciLayer); // add ECI Layer
            eciLayer.addRenderable(eciRadialGrid); // add grid (optional if it is on or not)

            // add ECEF Layer
            ecefLayer = new ECEFRenderableLayer(); // create ECEF layer
            ecefModel = new ECEFModelRenderable(satHash, wwd.getModel().getGlobe());
            ecefLayer.addRenderable(ecefModel); // add renderable object
            m.getLayers().add(ecefLayer); // add ECI Layer
            
            RenderableLayer latLongLinesLayer = createLatLongLinesLayer();
            latLongLinesLayer.setName("Lat/Long Lines");
            latLongLinesLayer.setEnabled(false);
            //insertBeforeCompass(this.getWwd(), latLongLinesLayer);
            m.getLayers().add(latLongLinesLayer); // add ECI Layer   
        
            starsLayer.setLongitudeOffset(Angle.fromDegrees(-eciLayer.getRotateECIdeg()));

            // set the sun provider to the shader
            spp = new CustomSunPositionProvider(sun);

            // Replace sky gradient with this atmosphere layer when using sun shading
            this.atmosphereLayer = new AtmosphereLayer();

            // Add lens flare layer
            this.lensFlareLayer = LensFlareLayer.getPresetInstance(LensFlareLayer.PRESET_BOLD);
            m.getLayers().add(this.lensFlareLayer);

            // Get tessellator
            this.tessellator = (RectangularNormalTessellator)m.getGlobe().getTessellator();
            // set default colors for shading
            this.tessellator.setAmbientColor(new Color(0.50f, 0.50f, 0.50f));

            // Add position listener to update light direction relative to the eye
            this.wwd.addPositionListener(new PositionListener()
            {
                Vec4 eyePoint;

                    public void moved(PositionEvent event)
                    {
                        if(eyePoint == null || eyePoint.distanceTo3(wwd.getView().getEyePoint()) > 1000)
                        {
                            update(true);
                            eyePoint = wwd.getView().getEyePoint();
                        }
                    }
            });

            setSunShadingOn(true); // enable sun shading by default
            // END Sun Shading -------------

            // correct clipping plane -- so entire orbits are shown - maybe make variable?
            setupView(); // setup needed viewing specs and use of AutoClipBasicOrbitView

            
            // Add the status bar
            StatusBar statusBar = new StatusBar();
            this.getContentPane().add(statusBar, BorderLayout.PAGE_END);

            // Forward events to the status bar to provide the cursor position info.
            statusBar.setEventSource(this.wwd);

            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.wwd, WorldMapLayer.class));

            // Call javascript appletInit()
            try
            {
                JSObject win = JSObject.getWindow(this);
                win.call("appletInit", null);
            }
            catch (Exception ignore)
            {
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public void start()
    {
        // Call javascript appletStart()
        try
        {
            JSObject win = JSObject.getWindow(this);
            win.call("appletStart", null);
        }
        catch (Exception ignore)
        {
        }
    }

    public void stop()
    {
        // Call javascript appletSop()
        try
        {
            JSObject win = JSObject.getWindow(this);
            win.call("appletStop", null);
        }
        catch (Exception ignore)
        {
        }

        // Shut down World Wind
        WorldWind.shutDown();
    }

    /**
     * Adds a layer to WW current layerlist, before a named layer. Target name can be a part of the layer name
     *
     * @param wwd        the <code>WorldWindow</code> reference.
     * @param layer      the layer to be added.
     * @param targetName the partial layer name to be matched - case sensitive.
     */
    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName)
    {
        // Insert the layer into the layer list just before the target layer.
        LayerList layers = wwd.getModel().getLayers();
        int targetPosition = layers.size() - 1;
        for (Layer l : layers)
        {
            if (l.getName().indexOf(targetName) != -1)
            {
                targetPosition = layers.indexOf(l);
                break;
            }
        }
        layers.add(targetPosition, layer);
    }

    // ============== Public API - Javascript ======================= //

    /**
     * Move the current view position
     *
     * @param lat the target latitude in decimal degrees
     * @param lon the target longitude in decimal degrees
     */
    public void gotoLatLon(double lat, double lon)
    {
        this.gotoLatLon(lat, lon, Double.NaN, 0, 0);
    }

    /**
     * Move the current view position, zoom, heading and pitch
     *
     * @param lat     the target latitude in decimal degrees
     * @param lon     the target longitude in decimal degrees
     * @param zoom    the target eye distance in meters
     * @param heading the target heading in decimal degrees
     * @param pitch   the target pitch in decimal degrees
     */
    public void gotoLatLon(double lat, double lon, double zoom, double heading, double pitch)
    {
        BasicOrbitView view = (BasicOrbitView) this.wwd.getView();
        if (!Double.isNaN(lat) || !Double.isNaN(lon) || !Double.isNaN(zoom))
        {
            lat = Double.isNaN(lat) ? view.getCenterPosition().getLatitude().degrees : lat;
            lon = Double.isNaN(lon) ? view.getCenterPosition().getLongitude().degrees : lon;
            zoom = Double.isNaN(zoom) ? view.getZoom() : zoom;
            heading = Double.isNaN(heading) ? view.getHeading().degrees : heading;
            pitch = Double.isNaN(pitch) ? view.getPitch().degrees : pitch;
            view.addPanToAnimator(Position.fromDegrees(lat, lon, 0),
                Angle.fromDegrees(heading), Angle.fromDegrees(pitch), zoom, true);
        }
    }

    /**
     * Set the current view heading and pitch
     *
     * @param heading the traget heading in decimal degrees
     * @param pitch   the target pitch in decimal degrees
     */
    public void setHeadingAndPitch(double heading, double pitch)
    {
        BasicOrbitView view = (BasicOrbitView) this.wwd.getView();
        if (!Double.isNaN(heading) || !Double.isNaN(pitch))
        {
            heading = Double.isNaN(heading) ? view.getHeading().degrees : heading;
            pitch = Double.isNaN(pitch) ? view.getPitch().degrees : pitch;

            view.addHeadingPitchAnimator(
                view.getHeading(), Angle.fromDegrees(heading), view.getPitch(), Angle.fromDegrees(pitch));
        }
    }

    /**
     * Set the current view zoom
     *
     * @param zoom the target eye distance in meters
     */
    public void setZoom(double zoom)
    {
        BasicOrbitView view = (BasicOrbitView) this.wwd.getView();
        if (!Double.isNaN(zoom))
        {
            view.addZoomAnimator(view.getZoom(), zoom);
        }
    }

    /**
     * Get the WorldWindowGLCanvas
     *
     * @return the current WorldWindowGLCanvas
     */
    public WorldWindowGLCanvas getWW()
    {
        return this.wwd;
    }

    /**
     * Get the current OrbitView
     *
     * @return the current OrbitView
     */
    public OrbitView getOrbitView()
    {
        if (this.wwd.getView() instanceof OrbitView)
            return (OrbitView) this.wwd.getView();
        return null;
    }

    /**
     * Get a reference to a layer with part of its name
     *
     * @param layerName part of the layer name to match.
     *
     * @return the corresponding layer or null if not found.
     */
    public Layer getLayerByName(String layerName)
    {
        for (Layer layer : wwd.getModel().getLayers())
        {
            if (layer.getName().indexOf(layerName) != -1)
                return layer;
        }
        return null;
    }

    /**
     * Add a text label at a position on the globe.
     *
     * @param text  the text to be displayed.
     * @param lat   the latitude in decimal degrees.
     * @param lon   the longitude in decimal degrees.
     * @param font  a string describing the font to be used.
     * @param color the color to be used as an hexadecimal coded string.
     */
    public void addLabel(String text, double lat, double lon, String font, String color)
    {
        GlobeAnnotation ga = new GlobeAnnotation(text, Position.fromDegrees(lat, lon, 0),
            Font.decode(font), Color.decode(color));
        ga.getAttributes().setBackgroundColor(Color.BLACK);
        ga.getAttributes().setDrawOffset(new Point(0, 0));
        ga.getAttributes().setFrameShape(AVKey.SHAPE_NONE);
        ga.getAttributes().setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        ga.getAttributes().setTextAlign(AVKey.CENTER);
        this.labelsLayer.addRenderable(ga);
    }
    
    //Added JSatTrak methods/classes
    public void updateTime()
    {
        // save old time
        double prevJulDate = currentJulianDate.getJulianDate();
        
        // Get current simulation time!             
        /*if(realTimeModeCheckBox.isSelected())
        {
            // real time mode -- just use real time
            
            // Get current time in GMT
            // calculate current Juilian Date, update to current time
            currentJulianDate.update2CurrentTime(); //update();// = getCurrentJulianDate();

        }*/
        //else
        {
            // non-real time mode add fraction of time to current jul date
            //currentJulianDate += currentPlayDirection*animationSimStepDays;
            currentJulianDate.addSeconds( currentPlayDirection*animationSimStepSeconds );
        }
        
        // update sun position
        sun.setCurrentMJD(currentJulianDate.getMJD());
                
        // if time jumps by more than 91 minutes check period of sat to see if
        // ground tracks need to be updated
        double timeDiffDays = Math.abs(currentJulianDate.getJulianDate()-prevJulDate); // in days
        checkTimeDiffResetGroundTracks(timeDiffDays);        
                
        // update date box:
        //dateTextField.setText( currentJulianDate.getDateTimeStr() );//String.format("%tc",cal) );
        
        // now propogate all satellites to the current time  
        for (AbstractSatellite sat : satHash.values() )
        {
            sat.propogate2JulDate( currentJulianDate.getJulianDate() );
        } // propgate each sat 
        
        // update any other time dependant objects
        for(JSatTrakTimeDependent tdo : timeDependentObjects)
        {
            if(tdo != null)
            {
                tdo.updateTime(currentJulianDate, satHash);
            }
        }
                
        forceRepainting(); // repaint 2d/3d earth
        
        
    } // update time
 public void checkTimeDiffResetGroundTracks(double timeDiffDays)
    {
        if( timeDiffDays > 91.0/1440.0)
        {
            // big time jump
            for (AbstractSatellite sat : satHash.values() )
            {
                if(sat.getShowGroundTrack() && (sat.getPeriod() <= (timeDiffDays*24.0*60.0) ) )
                {
                    sat.setGroundTrackIni2False();
                    //System.out.println(sat.getName() +" - Groundtrack Initiated");
                }
            }
        }
    } // checkTimeDiffResetGroundTracks
     public void forceRepainting()
    {
        /* NEED TO FIX THIS FOR THE APPLET!
         * // force repainting of all 2D windows
        for(J2DEarthPanel twoDPanel : twoDWindowVec )
        {
            twoDPanel.repaint();
        }
        
        // repaint 3D windows
        for(J3DEarthPanel threeDPanel : threeDWindowVec )
        {
            threeDPanel.repaintWWJ();
        }
        for(J3DEarthInternalPanel threeDPanel : threeDInternalWindowVec )
        {
            threeDPanel.repaintWWJ();         
        }
        */
    }// forceRepainting
    public void playScenario()
    {
        currentPlayDirection = 1; // forwards
        runAnimation(); // perform animation
    } // playScenario

    //NEEDS TO BE FIXED TO RUN WITHOUT ACTION LISTENER
    private void runAnimation()
    {
        lastFPSms = System.currentTimeMillis();
        playTimer = new Timer(animationRefreshRateMs, new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                // take one time step in the aimation
                updateTime(); // animate
                long stopTime = System.currentTimeMillis();
                
                fpsAnimation = 1.0 / ((stopTime-lastFPSms)/1000.0); // fps calculation
                lastFPSms = stopTime;
                // goal FPS:
                //fpsAnimation = 1.0 / (animationRefreshRateMs/1000.0);
                
                if (stopHit)
                {
                    playTimer.stop();                   
                }
                // SEG - if update took a long time reduce the timers repeat interval
                                
            }
        });
    } // runAnimation
    public void stopAnimation()
    {
        stopHit = true; // set flag for next animation step
    }
public void addCustomSat(String name)
    {
        // if nothing given:
        if(name == null || name.equalsIgnoreCase(""))
        {
            System.out.println("returned");
            return;
        }
        
        CustomSatellite prop = new CustomSatellite(name,this.getScenarioEpochDate());
        
        satHash.put(name, prop);

        // set satellite time to current date
        prop.propogate2JulDate(this.getCurrentJulTime());
    }
    public Time getScenarioEpochDate()
    {
        return scenarioEpochDate;
    }
        public double getCurrentJulTime()
    {
        return currentJulianDate.getJulianDate();
    }
    private RenderableLayer createLatLongLinesLayer()
    {
        RenderableLayer shapeLayer = new RenderableLayer();

            // Generate meridians
            ArrayList<Position> positions = new ArrayList<Position>(3);
            double height = 30e3; // 10e3 default
            for (double lon = -180; lon < 180; lon += 10)
            {
                Angle longitude = Angle.fromDegrees(lon);
                positions.clear();
                positions.add(new Position(Angle.NEG90, longitude, height));
                positions.add(new Position(Angle.ZERO, longitude, height));
                positions.add(new Position(Angle.POS90, longitude, height));
                Polyline polyline = new Polyline(positions);
                polyline.setFollowTerrain(false);
                polyline.setNumSubsegments(30);
                
                if(lon == -180 || lon == 0)
                {
                    polyline.setColor(new Color(1f, 1f, 0f, 0.5f)); // yellow
                }
                else
                {
                    polyline.setColor(new Color(1f, 1f, 1f, 0.5f));
                }
                
                shapeLayer.addRenderable(polyline);
            }

            // Generate parallels
            for (double lat = -80; lat < 90; lat += 10)
            {
                Angle latitude = Angle.fromDegrees(lat);
                positions.clear();
                positions.add(new Position(latitude, Angle.NEG180, height));
                positions.add(new Position(latitude, Angle.ZERO, height));
                positions.add(new Position(latitude, Angle.POS180, height));
                Polyline polyline = new Polyline(positions);
                polyline.setPathType(Polyline.LINEAR);
                polyline.setFollowTerrain(false);
                polyline.setNumSubsegments(30);
                
                if(lat == 0)
                {
                    polyline.setColor(new Color(1f, 1f, 0f, 0.5f));
                }
                else
                {
                    polyline.setColor(new Color(1f, 1f, 1f, 0.5f));
                }
                
                shapeLayer.addRenderable(polyline);
            }

            return shapeLayer;
    }
// Update worldwind wun shading
    private void update(boolean redraw)
    {
        if(sunShadingOn) //this.enableCheckBox.isSelected())
        {
            // Compute Sun position according to current date and time
            LatLon sunPos = spp.getPosition();
            Vec4 sun = wwd.getModel().getGlobe().computePointFromPosition(new Position(sunPos, 0)).normalize3();

            Vec4 light = sun.getNegative3();
            this.tessellator.setLightDirection(light);
            this.lensFlareLayer.setSunDirection(sun);
            this.atmosphereLayer.setSunDirection(sun);

            // Redraw if needed
            if(redraw)
            {
                wwd.redraw();
            }
        } // if sun Shading
        
    } // update - for sun shading

    public void setSunShadingOn(boolean useSunShading)
    {
        if(useSunShading == sunShadingOn)
        {
            return; // nothing to do
        }

        sunShadingOn = useSunShading;

        if(sunShadingOn)
        {
            // enable shading - use special atmosphere
            for(int i = 0; i < wwd.getModel().getLayers().size(); i++)
            {
                Layer l = wwd.getModel().getLayers().get(i);
                if(l instanceof SkyGradientLayer)
                {
                    wwd.getModel().getLayers().set(i, this.atmosphereLayer);
                }
            }
        }
        else
        {
            // disable shading
            // Turn off lighting
            this.tessellator.setLightDirection(null);
            this.lensFlareLayer.setSunDirection(null);
            this.atmosphereLayer.setSunDirection(null);

            // use standard atmosphere
            for(int i = 0; i < wwd.getModel().getLayers().size(); i++)
            {
                Layer l = wwd.getModel().getLayers().get(i);
                if(l instanceof AtmosphereLayer)
                {
                    wwd.getModel().getLayers().set(i, new SkyGradientLayer());
                }
            }
            
        } // if/else shading

        this.update(true); // redraw
    } // setSunShadingOn
    private void setupView()
    {
        if(modelViewMode == false)
        { // Earth View mode
            AutoClipBasicOrbitView bov = new AutoClipBasicOrbitView();
            wwd.setView(bov);
            
            // remove the rest of the old input handler  (does this need a remove of hover listener? - maybe it is now completely removed?)
            wwd.getInputHandler().setEventSource(null);
            
            AWTInputHandler awth = new AWTInputHandler();
            awth.setEventSource(wwd);
            wwd.setInputHandler(awth);
            awth.setSmoothViewChanges(smoothViewChanges); // FALSE MAKES THE VIEW FAST!! -- MIGHT WANT TO MAKE IT GUI Chooseable
                        
            // IF EARTH VIEW -- RESET CLIPPING PLANES BACK TO NORMAL SETTINGS!!!
            wwd.getView().setNearClipDistance(this.nearClippingPlaneDistOrbit);
            wwd.getView().setFarClipDistance(this.farClippingPlaneDistOrbit);
            
            // change class for inputHandler
            Configuration.setValue(AVKey.INPUT_HANDLER_CLASS_NAME, 
                        AWTInputHandler.class.getName());

            // re-setup control layer handler
            wwd.addSelectListener(new ViewControlsSelectListener(wwd, viewControlsLayer));
            
        } // Earth View mode
        else
        { // Model View mode
            
            // TEST NEW VIEW -- TO MAKE WORK MUST TURN OFF ECI!
            this.setViewModeECI(false);

            if(!satHash.containsKey(modelViewString))
            {
                System.out.println("NO Current Satellite Selected, can't switch to Model Mode: " + modelViewString);
                return;
            }

            AbstractSatellite sat = satHash.get(modelViewString);

            BasicModelView3 bmv;
            if(wwd.getView() instanceof BasicOrbitView)
            {
                bmv = new BasicModelView3(((BasicOrbitView)wwd.getView()).getOrbitViewModel(), sat);
                //bmv = new BasicModelView3(sat);
            }
            else
            {
                bmv = new BasicModelView3(((BasicModelView3)wwd.getView()).getOrbitViewModel(), sat);
            }
            
            // remove the old hover listener -- depending on this instance of the input handler class type
            if( wwd.getInputHandler() instanceof AWTInputHandler)
            {
                ((AWTInputHandler) wwd.getInputHandler()).removeHoverSelectListener();
            }
            else if( wwd.getInputHandler() instanceof BasicModelViewInputHandler3)
            {
                ((BasicModelViewInputHandler3) wwd.getInputHandler()).removeHoverSelectListener();
            }
            
            // set view
            wwd.setView(bmv);

            // remove the rest of the old input handler
            wwd.getInputHandler().setEventSource(null);
             
            // add new input handler
            BasicModelViewInputHandler3 mih = new BasicModelViewInputHandler3();
            mih.setEventSource(wwd);
            wwd.setInputHandler(mih);
            
            // view smooth?
            mih.setSmoothViewChanges(smoothViewChanges); // FALSE MAKES THE VIEW FAST!!

            // settings for great closeups!
            wwd.getView().setNearClipDistance(modelViewNearClip);
            wwd.getView().setFarClipDistance(modelViewFarClip);
            bmv.setZoom(900000);
            bmv.setPitch(Angle.fromDegrees(45));
            
            // change class for inputHandler
            Configuration.setValue(AVKey.INPUT_HANDLER_CLASS_NAME, 
                        BasicModelViewInputHandler3.class.getName());

            // re-setup control layer handler
            wwd.addSelectListener(new ViewControlsSelectListener(wwd, viewControlsLayer));
            
        } // model view mode
        
    } // setupView

}

