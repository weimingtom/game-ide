package jpct;
import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;


class TesteGame {
	
   private final static SimpleVector STARTING_POS=new SimpleVector(800, -120, -400); 
   private final static float COLLISION_SPHERE_RADIUS=8f;   
   private final static float PLAYER_HEIGHT=30f;
   private final static SimpleVector ELLIPSOID_RADIUS=
	   new SimpleVector(COLLISION_SPHERE_RADIUS,PLAYER_HEIGHT/2f,COLLISION_SPHERE_RADIUS);

   private final static float GRAVITY=4f;
   private final static float MOVE_SPEED=3.5f;
   private final static float TURN_SPEED=0.06f;
   private final static int SWITCH_RENDERER=35;

   private boolean fullscreen=false;
   private boolean openGL=false;
   private boolean wireframe=false;

   private Object3D level=null;
   private Object3D weapon=null;
   private Object3D elevator=null;
   
   private FrameBuffer buffer=null;
   private World theWorld=null;
   private TextureManager texMan=null;
   private Camera camera=null;
   private Texture numbers=null;
   private Matrix playerDirection=new Matrix();
   private SimpleVector tempVector=new SimpleVector();
   private int width=1280;
   private int height=800;
  
   private Frame frame=null;
   private Graphics gFrame=null;
   private BufferStrategy bufferStrategy=null;
   private GraphicsDevice device=null;
   private int titleBarHeight=0;
   private int leftBorderWidth=0;

   private int switchMode=0;
   private int fps;
   private int lastFps;
   private long totalFps;
   private int pps;
   private int lastPps;
   private boolean isIdle=false;
   private boolean exit=false; 
   private boolean left=false;
   private boolean right=false;
   private boolean up=false;
   private boolean down=false;
   private boolean forward=false;
   private boolean back=false;
   private KeyMapper keyMapper=null;
   float elevatorOffset=-0.8f;
   float elevatorPosition=-90f;
   int elevatorCountdown=50;

   public static void main(String[] args) {
	   TesteGame start=new TesteGame(args);
   }
   
   private TesteGame(String[] args) {
  
      for (int i=0; i<args.length; i++) {
         if (args[i].equals("fullscreen")) {
            fullscreen=true;
            Config.glFullscreen=true;
         }
         if (args[i].equals("mipmap")) {
            Config.glMipmap=true;
         }
         if (args[i].equals("trilinear")) {
            Config.glTrilinear=true;
         }

         if (args[i].equals("16bit")) {
            Config.glColorDepth=16;
         }
         try {
            if (args[i].startsWith("width=")) {
               width=Integer.parseInt(args[i].substring(6));
            }
            if (args[i].startsWith("height=")) {
               height=Integer.parseInt(args[i].substring(7));
            }
            if (args[i].startsWith("refresh=")) {
               Config.glRefresh=Integer.parseInt(args[i].substring(8));
            }
            if (args[i].startsWith("zbuffer=")) {
               Config.glZBufferDepth=Integer.parseInt(args[i].substring(8));
               if (Config.glZBufferDepth==16) {
                  Config.glFixedBlitting=true;
               }
            }

         } catch (Exception e) {
            
         }
      }

      isIdle=false;
      switchMode=0;
      totalFps=0;
      fps=0;
      lastFps=0;

      theWorld=new World();
      texMan=TextureManager.getInstance();


      Config.fadeoutLight=true;
      Config.linearDiv=100;
      Config.lightDiscardDistance=350;
      theWorld.getLights().setOverbrightLighting(Lights.OVERBRIGHT_LIGHTING_DISABLED);
      theWorld.getLights().setRGBScale(Lights.RGB_SCALE_2X);
      theWorld.setAmbientLight(10, 15, 15);

      theWorld.addLight(new SimpleVector(820, -150, -400), 5, 20, 15);
      theWorld.addLight(new SimpleVector(850, -130, -580), 20, 18, 2);
      theWorld.addLight(new SimpleVector(850, -130, -760), 15, 10, 15);
      theWorld.addLight(new SimpleVector(1060, -170, -910), 20, 0, 0);
      theWorld.addLight(new SimpleVector(760, -200, -990), 15, 10, 20);
      theWorld.addLight(new SimpleVector(850, -230, -780), 0, 15, 25);
      theWorld.addLight(new SimpleVector(600, -230, -770), 20, 25, 0);
      theWorld.addLight(new SimpleVector(405, -230, -610), 18, 20, 25);
      theWorld.addLight(new SimpleVector(340, -150, -370), 15, 20, 25);
      theWorld.addLight(new SimpleVector(650, -170, -200), 15, 0, 0);
      theWorld.addLight(new SimpleVector(870, -230, -190), 15, 20, 20);
      theWorld.addLight(new SimpleVector(540, -190, -180), 15, 15, 15);
    
      theWorld.setFogging(World.FOGGING_ENABLED);
      theWorld.setFogParameters(500, 0, 0, 0);
      Config.farPlane=500;
      
      char c=File.separatorChar;
      numbers=new Texture("textures"+c+"other"+c+"numbers.jpg");
      texMan.addTexture("numbers", numbers);
      texMan.addTexture("envmap", new Texture("textures"+c+"other"+c+"envmap.jpg"));

      File dir=new File("textures");
      String[] files=dir.list();
      for (int i=0; i<files.length; i++) {
         String name=files[i];
         if (name.toLowerCase().endsWith(".jpg")) {
            texMan.addTexture(name, new Texture("textures"+c+name));
         }
      }

      Object3D[] miss=Loader.load3DS("3ds"+c+"weapon.3ds", 2);
      weapon=miss[0];
      weapon.rotateY(-(float) Math.PI/2f);
      weapon.rotateZ(-(float) Math.PI/2f);
      weapon.rotateX(-(float) Math.PI/7f);


      weapon.rotateMesh();
      weapon.translate(6, 6, 10);


      weapon.translateMesh();
      weapon.setRotationMatrix(new Matrix());
      weapon.setTranslationMatrix(new Matrix());

      weapon.setTexture("envmap");
      weapon.setEnvmapped(Object3D.ENVMAP_ENABLED);
      weapon.setEnvmapMode(Object3D.ENVMAP_WORLDSPACE);
      theWorld.addObject(weapon);

      Object3D[] levelParts=Loader.load3DS("3ds"+c+"ql.3ds", 20f);
      level=new Object3D(0);

      for (int i=0; i<levelParts.length; i++) {
         Object3D part=levelParts[i];

         part.setCenter(SimpleVector.ORIGIN);
         part.rotateX((float)-Math.PI/2);
         part.rotateMesh();
         part.setRotationMatrix(new Matrix());


         level=Object3D.mergeObjects(level, part);
      }

      level.createTriangleStrips(2);
      level.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
      level.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);

      OcTree oc=new OcTree(level, 100, OcTree.MODE_OPTIMIZED);
      oc.setCollisionUse(OcTree.COLLISION_USE);
      level.setOcTree(oc); 
      level.enableLazyTransformations();
      theWorld.addObject(level);

      elevator=Primitives.getBox(15f,0.1f);
      elevator.rotateY((float)Math.PI/4);
      elevator.setOrigin(new SimpleVector(800,-90,-450));
      elevator.setCollisionOptimization(Object3D.COLLISION_DETECTION_OPTIMIZED);
      elevator.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
      elevator.setTexture("envmap");
      elevator.setEnvmapped(Object3D.ENVMAP_ENABLED);
      elevator.setEnvmapMode(Object3D.ENVMAP_CAMERASPACE);
      theWorld.addObject(elevator);
      camera=theWorld.getCamera();
      camera.setPosition(STARTING_POS);
      theWorld.buildAllObjects();
      weapon.setRotationPivot(new SimpleVector(0, 0, 0));
      Config.collideOffset=250;
      Config.tuneForOutdoor();
      initializeFrame();
      gameLoop();
   }


   private void initializeFrame() {
      if (fullscreen) {
         GraphicsEnvironment env=GraphicsEnvironment.getLocalGraphicsEnvironment();
         device=env.getDefaultScreenDevice();
         GraphicsConfiguration gc=device.getDefaultConfiguration();
         frame=new Frame(gc);
         frame.setUndecorated(true);
         frame.setIgnoreRepaint(true);
         device.setFullScreenWindow(frame);
         if (device.isDisplayChangeSupported()) {
            device.setDisplayMode(new DisplayMode(width, height, 32, 0));
         }
         frame.createBufferStrategy(2);
         bufferStrategy=frame.getBufferStrategy();
         Graphics g=bufferStrategy.getDrawGraphics();
         bufferStrategy.show();
         g.dispose();
      } else {
         frame=new Frame();
         frame.setTitle("jPCT "+Config.getVersion());
         frame.pack();
         Insets insets = frame.getInsets();
         titleBarHeight=insets.top;
         leftBorderWidth=insets.left;
         frame.setSize(width+leftBorderWidth+insets.right, height+titleBarHeight+insets.bottom);
         frame.setResizable(false);
         frame.show();
         gFrame=frame.getGraphics();
      }

      /**
       * The listeners are bound to the AWT frame...they are useless in OpenGL mode.
       */
      frame.addWindowListener(new WindowEvents());
      keyMapper=new KeyMapper(frame);
   }


   private void display() {
      blitNumber((int) totalFps, 5, 2);
      blitNumber((int) lastPps, 5, 12);

      if (!openGL) {
         if (!fullscreen) {
            buffer.display(gFrame, leftBorderWidth, titleBarHeight);
         } else {
            Graphics g=bufferStrategy.getDrawGraphics();
            g.drawImage(buffer.getOutputBuffer(), 0, 0, null);
            bufferStrategy.show();
            g.dispose();
         }
      } else {
         buffer.displayGLOnly();
      }
   }


   private void blitNumber(int number, int x, int y) {
      if (numbers!=null) {
         String sNum=Integer.toString(number);
         for (int i=0; i<sNum.length(); i++) {
            char cNum=sNum.charAt(i);
            int iNum=cNum-48;
            buffer.blit(numbers, iNum*5, 0, x, y, 5, 9, FrameBuffer.TRANSPARENT_BLITTING);
            x+=5;
         }
      }
   }

   private void doMovement() {

      SimpleVector camPos=camera.getPosition();
      camPos.add(new SimpleVector(0, PLAYER_HEIGHT/2f, 0));
      SimpleVector dir=new SimpleVector(0, GRAVITY, 0);
      dir=theWorld.checkCollisionEllipsoid(camPos, dir, ELLIPSOID_RADIUS, 1);
      camPos.add(new SimpleVector(0, -PLAYER_HEIGHT/2f, 0));
      dir.x=0;
      dir.z=0;
      camPos.add(dir);
      camera.setPosition(camPos);


      boolean cameraChanged=false;

      if (forward) {
         camera.moveCamera(new SimpleVector(0,1,0), PLAYER_HEIGHT/2f);
         cameraChanged=true;
         tempVector=playerDirection.getZAxis();
         theWorld.checkCameraCollisionEllipsoid(tempVector, ELLIPSOID_RADIUS, MOVE_SPEED, 5);
      }
      if (back) {
         if (!cameraChanged) {
            camera.moveCamera(new SimpleVector(0,1,0), PLAYER_HEIGHT/2f);
            cameraChanged=true;
         }
         tempVector=playerDirection.getZAxis();
         tempVector.scalarMul(-1f);
         theWorld.checkCameraCollisionEllipsoid(tempVector, ELLIPSOID_RADIUS, MOVE_SPEED, 5);
      }

      if (left) {
         camera.rotateAxis(camera.getBack().getYAxis(), -TURN_SPEED);
         playerDirection.rotateY(-TURN_SPEED);
      }
      if (right) {
         camera.rotateAxis(camera.getBack().getYAxis(), TURN_SPEED);
         playerDirection.rotateY(TURN_SPEED);
      }

      if (up) {
         camera.rotateX(TURN_SPEED);
      }
      if (down) {
         camera.rotateX(-TURN_SPEED);
      }

      if (cameraChanged) {
         camera.moveCamera(new SimpleVector(0, -1, 0), PLAYER_HEIGHT/2f);
      }
   }

   private void moveElevator() {

      if ((elevator.wasTargetOfLastCollision()&&elevatorCountdown--<=0)||
          (elevatorPosition!=-90f&&elevatorPosition!=-180f)) {

         float tempElevator=elevatorPosition+elevatorOffset;
         float tempOffset=elevatorOffset;

         if (tempElevator<-180f) {
            tempOffset=-180f-elevatorPosition;
            elevatorCountdown=50;
            elevatorOffset*=-1f;
         } else {
            if (tempElevator>-90f) {
               tempOffset=-90f-elevatorPosition;
               elevatorCountdown=50;
               elevatorOffset*=-1f;
            }
         }
         elevatorPosition+=tempOffset;
         elevator.translate(0, tempOffset, 0);
      }
   }


   private void poll() {
      KeyState state=null;
      do {
         state=keyMapper.poll();
         if (state!=KeyState.NONE) {
            keyAffected(state);
         }
      } while (state!=KeyState.NONE);
   }


   private void gameLoop() {
      World.setDefaultThread(Thread.currentThread());

      buffer=new FrameBuffer(width, height, FrameBuffer.SAMPLINGMODE_NORMAL);
      buffer.enableRenderer(IRenderer.RENDERER_SOFTWARE);
      buffer.setBoundingBoxMode(FrameBuffer.BOUNDINGBOX_NOT_USED);

      buffer.optimizeBufferAccess();

      Timer timer=new Timer(25);
      timer.start();

      Timer fpsTimer=new Timer(1000);
      fpsTimer.start();

      long timerTicks=0;

      while (!exit) {

         if (!isIdle) {

            long ticks=timer.getElapsedTicks();
            timerTicks+=ticks;

            for (int i=0; i<ticks; i++) {
               /**
                * Do this as often as ticks have passed. This can
                * be improved by calling the method only once and letting
                * the collision detection somehow handle the ticks passed.
                */
                doMovement();
                moveElevator();
            }

            poll();

            if (switchMode!=0) {
               switchOptions();
            }

            buffer.clear();

            weapon.getTranslationMatrix().setIdentity();
            weapon.translate(camera.getPosition());
            weapon.align(camera);
            weapon.rotateAxis(camera.getDirection(), (float) Math.sin(timerTicks/6f)/20f);

            theWorld.renderScene(buffer);

            if (!wireframe) {
               theWorld.draw(buffer);
            } else {
               theWorld.drawWireframe(buffer, Color.white);
            }

            buffer.update();
            display();

            fps++;
            pps+=theWorld.getVisibilityList().getSize();

            if (fpsTimer.getElapsedTicks()>0) {
               totalFps=(fps-lastFps);
               lastFps=fps;
               lastPps=pps;
               pps=0;
            }

            Thread.yield();

         } else {
            try {
               Thread.sleep(500);
            } catch (InterruptedException e) {}
         }
      }

      buffer.dispose();
      if (!openGL && fullscreen) {
        device.setFullScreenWindow(null);
      }
      System.exit(0);
   }

   private void switchOptions() {
      switch (switchMode) {
         case (SWITCH_RENDERER): {
            isIdle=true;
            if (buffer.usesRenderer(IRenderer.RENDERER_OPENGL)) {
               keyMapper.destroy();
               buffer.disableRenderer(IRenderer.RENDERER_OPENGL);
               buffer.enableRenderer(IRenderer.RENDERER_SOFTWARE, IRenderer.MODE_OPENGL);
               openGL=false;
               if (fullscreen) {
                  device.setFullScreenWindow(null);
               }
               frame.hide();
               frame.dispose();
               initializeFrame();
            } else {
               frame.hide();
               keyMapper.destroy();
               buffer.enableRenderer(IRenderer.RENDERER_OPENGL, IRenderer.MODE_OPENGL);
               buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
               openGL=true;
               keyMapper=new KeyMapper();
            }
            isIdle=false;
            break;
         }
      }
      switchMode=0;
   }


   private void keyAffected(KeyState state) {
      int code=state.getKeyCode();
      boolean event=state.getState();
      switch (code) {
         case (KeyEvent.VK_ESCAPE): {
            exit=event;
            break;
         }
         case (KeyEvent.VK_LEFT): {
            left=event;
            break;
         }
         case (KeyEvent.VK_RIGHT): {
            right=event;
            break;
         }
         case (KeyEvent.VK_PAGE_UP): {
            up=event;
            break;
         }
         case (KeyEvent.VK_PAGE_DOWN): {
            down=event;
            break;
         }
         case (KeyEvent.VK_UP): {
            forward=event;
            break;
         }
         case (KeyEvent.VK_DOWN): {
            back=event;
            break;
         }
         case (KeyEvent.VK_1): {
            if (event&&buffer.supports(FrameBuffer.SUPPORT_FOR_RGB_SCALING)) {
               theWorld.getLights().setRGBScale(Lights.RGB_SCALE_DEFAULT);
            }
            break;
         }

         case (KeyEvent.VK_2): { // 2x scaling
            if (event&&buffer.supports(FrameBuffer.SUPPORT_FOR_RGB_SCALING)) {
               theWorld.getLights().setRGBScale(Lights.RGB_SCALE_2X);
            }
            break;
         }

         case (KeyEvent.VK_4): { // 4x scaling
            if (event&&buffer.supports(FrameBuffer.SUPPORT_FOR_RGB_SCALING)) {
               theWorld.getLights().setRGBScale(Lights.RGB_SCALE_4X);
            }
            break;
         }

         case (KeyEvent.VK_W): { // wireframe mode (w)
            if (event) {
               wireframe=!wireframe;
            }
            break;
         }

         case (KeyEvent.VK_X): { // change renderer  (x)
            if (event) {
               switchMode=SWITCH_RENDERER;
            }
            break;
         }
      }
   }

   private class WindowEvents extends WindowAdapter {

      public void windowIconified(WindowEvent e) {
         isIdle=true;
      }

      public void windowDeiconified(WindowEvent e) {
         isIdle=false;
      }
   }



   private class Timer {
      private long ticks=0;
      private long granularity=0;

      public Timer(int granularity) {
         this.granularity=granularity;
      }

      public void start() {
         ticks=System.currentTimeMillis();
      }

      public void reset() {
         start();
      }

      public long getElapsedTicks() {
         long cur=System.currentTimeMillis();
         long l=cur-ticks;

         if (l>=granularity) {
            ticks=cur-(l%granularity);
            return l/granularity;
         }
         return 0;
      }
   }
}
