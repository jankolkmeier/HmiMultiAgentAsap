package nl.utwente.hmi.starters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import asap.bml.ext.bmlt.BMLTInfo;
import asap.environment.AsapEnvironment;
import asap.realizerembodiments.SharedPortLoader;
import hmi.animation.VJoint;
import hmi.audioenvironment.AudioEnvironment;
import hmi.environmentbase.Environment;
import hmi.jcomponentenvironment.JComponentEnvironment;
import hmi.mixedanimationenvironment.MixedAnimationEnvironment;
import hmi.physicsenvironment.OdePhysicsEnvironment;
import hmi.renderenvironment.HmiRenderEnvironment;
import hmi.renderenvironment.HmiRenderEnvironment.RenderStyle;
import hmi.unityembodiments.loader.SharedMiddlewareLoader;
import hmi.util.Console;
import hmi.worldobjectenvironment.VJointWorldObject;
import hmi.worldobjectenvironment.WorldObjectEnvironment;
import saiba.bml.BMLInfo;
import saiba.bml.core.FaceLexemeBehaviour;
import saiba.bml.core.HeadBehaviour;
import saiba.bml.core.PostureShiftBehaviour;

/**
 * Simple demo for the AsapRealizer+environment
 * @author hvanwelbergen
 * 
 */
public class UnityWithArmandiaStarter
{
    private final HmiRenderEnvironment hre;
    private final OdePhysicsEnvironment ope;

    private VJoint sphereJoint;
    protected JFrame mainJFrame = null;

    public UnityWithArmandiaStarter(JFrame j) throws IOException
    {
        String specArmandia = "multiAgentSpecs/armandia.xml";
        String specUnity = "multiAgentSpecs/uma/UMA_F.xml";
        
        String shared_port = "multiAgentSpecs/shared_port.xml";
        String shared_middleware = "multiAgentSpecs/shared_middleware.xml";
        String resources = "";
        
        Console.setEnabled(false);
        System.setProperty("sun.java2d.noddraw", "true"); // avoid potential
                                                          // interference with
                                                          // (non-Jogl) Java
                                                          // using direct draw
        mainJFrame = j;

        BMLTInfo.init();
        BMLInfo.addCustomFloatAttribute(FaceLexemeBehaviour.class, "http://asap-project.org/convanim", "repetition");
        BMLInfo.addCustomStringAttribute(HeadBehaviour.class, "http://asap-project.org/convanim", "spindirection");
        BMLInfo.addCustomFloatAttribute(PostureShiftBehaviour.class, "http://asap-project.org/convanim", "amount");

        hre = new HmiRenderEnvironment()
        {
            @Override
            protected void renderTime(double currentTime)
            {
                super.renderTime(currentTime);
                double speed = 1;
                if (sphereJoint != null) sphereJoint.setTranslation(0.75f + (float) Math.sin(currentTime * speed) * 1.5f,
                        (float) Math.cos(currentTime * speed) * 1f + 1.5f, 0.5f);
            }
        };

        ope = new OdePhysicsEnvironment();

        WorldObjectEnvironment we = new WorldObjectEnvironment();
        MixedAnimationEnvironment mae = new MixedAnimationEnvironment();
        final AsapEnvironment ee = new AsapEnvironment();
        AudioEnvironment aue = new AudioEnvironment("LJWGL_JOAL");

        final JComponentEnvironment jce = setupJComponentEnvironment();

        hre.init(); // canvas does not exist until init was called
        we.init();
        ope.init();
        aue.init();
        mae.init(ope, 0.002f);

        ArrayList<Environment> environments = new ArrayList<Environment>();

        SharedMiddlewareLoader sml = new SharedMiddlewareLoader();
        sml.load(resources, shared_middleware);
        environments.add(sml);
        
        SharedPortLoader spl = new SharedPortLoader();
        spl.load(resources, shared_port, ope.getPhysicsClock());
        environments.add(spl);

        environments.add(hre);
        environments.add(we);
        environments.add(ope);
        environments.add(mae);
        environments.add(ee);
        environments.add(aue);
        environments.add(jce);

        ee.init(environments, ope.getPhysicsClock()); // if no physics, just use renderclock here!

        // this clock method drives the engines in ee. if no physics, then register ee as a listener at the render clock!
        ope.addPrePhysicsCopyListener(ee);

        // hre.getRenderClock().addClockListener(ee);

        hre.loadCheckerBoardGround("ground", 0.5f, 0f);
        hre.setBackground(0.2f, 0.2f, 0.2f);
        VJoint camera = hre.getCameraTarget();
        we.getWorldObjectManager().addWorldObject("camera", new VJointWorldObject(camera));

        // set camera position
        // hre.setNavigationEnabled(false);
        // hre.setViewPoint(new float[]{0,2,4});

        ee.loadVirtualHuman(resources, specArmandia, "Armandia Agent");
        ee.loadVirtualHuman(resources, specUnity, "UMA Agent");

        j.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent winEvt)
            {
                System.exit(0);
            }
        });

        mainJFrame.setSize(1000, 600);

        java.awt.Component canvas = hre.getAWTComponent();
        mainJFrame.add(canvas, BorderLayout.CENTER);
        mainJFrame.setVisible(true);

    }

    private JComponentEnvironment setupJComponentEnvironment()
    {
        final JComponentEnvironment jce = new JComponentEnvironment();
        try
        {
            SwingUtilities.invokeAndWait(() -> {
                mainJFrame.setLayout(new BorderLayout());

                JPanel jPanel = new JPanel();
                jPanel.setPreferredSize(new Dimension(400, 40));
                jPanel.setLayout(new GridLayout(1, 1));
                jce.registerComponent("textpanel", jPanel);
                mainJFrame.add(jPanel, BorderLayout.SOUTH);
            });
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        return jce;
    }

    public void startClocks()
    {
        hre.startRenderClock();
        ope.startPhysicsClock();
    }

    public static void main(String[] args) throws IOException
    {
        UnityWithArmandiaStarter demo = new UnityWithArmandiaStarter(new JFrame("AsapRealizer demo"));
        demo.startClocks();
    }
}
